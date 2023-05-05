import json
import logging
import os
import pickle
import pytz
import sys
import urllib.parse
from datetime import datetime

# configure logging
logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
handler = logging.StreamHandler(sys.stdout)
handler.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
handler.setFormatter(formatter)
logger.addHandler(handler)

# template definitions
TEMPLATE_TIME_MS = '<TIME_MS>'
TEMPLATE_TIME_STR = '<TIME_STR>'


# MAIN MAPPER FUNCTIONS

def annotate_events(events, component_id, device_id):
    result = ''
    for event in events:
        annotation = annotate_event(event=event, component_id=component_id, device_id=device_id)
        if annotation is not None:
            result += annotation + '\n\n'
    return result


def annotate_event(event, component_id, device_id):
    try:
        # check if event is valid
        valid = 'metric' in event and \
                'value' in event and \
                'unit' in event and \
                'time' in event

        # if the event is not valid, return None
        if not valid:
            logger.warning('No valid event: %s', event)
            return None

        # otherwise, return mapped event
        rdf_event = map_observation_to_rdf(event=event, component_id=component_id, device_id=device_id)
        return remove_whitespace(rdf_event) if rdf_event is not None else rdf_event

    except Exception as e:
        msg = 'Unknown exception occurred while semantically annotating the event \'%s\': %s: %s' \
              % (event, type(e).__name__, e)
        logger.error(msg)
        return None


def map_observation_to_rdf(event, component_id, device_id):
    result = map_observation(component_id=component_id,
                             device_id=device_id,
                             metric=event['metric'],
                             value=event['value'],
                             unit=event['unit'],
                             timestamp=event['time'],
                             feature_of_interest_id=(event['featureOfInterestId']
                                                     if 'featureOfInterestId' in event else None))
    return result


# MAPPING FUNCTIONS OF HEADACHE PARTS

def map_observation(metric,
                    value,
                    unit,
                    timestamp,
                    component_id,
                    device_id,
                    feature_of_interest_id):
    # map property
    # -> if no mapping is found, observations of this metric
    #    are ignored by the mapper
    if metric not in PROPERTY_MAP:
        return None
    property_class, feature_of_interest_type = PROPERTY_MAP[metric]

    # generate UUID & timestamp
    uuid = generate_uuid(component_id=component_id, metric=metric)
    timestamp_utc = int(timestamp)
    time_str = convert_timestamp_to_string(timestamp)
        
    # construct feature of interest URI
    feature_of_interest_uri = map_feature_of_interest(feature_of_interest_type=feature_of_interest_type,
                                                      component_id=component_id,
                                                      device_id=device_id,
                                                      feature_of_interest_id=feature_of_interest_id)

    # map value & unit
    unit_uri, property_value_type = UNIT_MAP[unit]
    value_literal = map_value(value, property_value_type)

    # create RDF data
    return OBSERVATION_TEMPLATE % (component_id, metric, uuid,
                                   property_class, feature_of_interest_uri,
                                   time_str, str(timestamp_utc), value_literal, unit_uri)


def map_feature_of_interest(feature_of_interest_type, component_id, device_id, feature_of_interest_id):
    if feature_of_interest_type == 'device':
        return FEATURE_OF_INTEREST_MAP[feature_of_interest_type] % device_id
    elif feature_of_interest_type == 'rsp_query':
        return FEATURE_OF_INTEREST_MAP[feature_of_interest_type] % (component_id, feature_of_interest_id)
    elif feature_of_interest_type == 'rdf_stream':
        return FEATURE_OF_INTEREST_MAP[feature_of_interest_type] % \
               (component_id, urllib.parse.quote(feature_of_interest_id, safe=''))
    else:
        return '<https://divide.idlab.ugent.be/meta-model/entity/unknown>'


def map_value(value, property_value_type):
    if property_value_type == bool:
        processed_value = int(1) if value == 1 or value == '1' else int(0)
    elif property_value_type == int:
        processed_value = int(value)
    else:
        processed_value = value

    return VALUE_TEMPLATE % (processed_value, TYPE_MAP[property_value_type])


def map_unit(unit):
    if unit not in UNIT_MAP:
        logger.warning('Unknown unit type %s', unit)
        unit = 'number'
    return UNIT_MAP[unit]


# MAPPING TEMPLATES

OBSERVATION_TEMPLATE = """
<https://divide.idlab.ugent.be/%s/%s/obs%s>
    <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/core/Measurement> ;
    <https://saref.etsi.org/core/relatesToProperty> [
        <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://divide.idlab.ugent.be/meta-model/monitoring/%s> 
    ] ;
    <https://saref.etsi.org/core/isMeasurementOf> %s ;
    <https://saref.etsi.org/core/hasTimestamp> "%s"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
    <https://dahcc.idlab.ugent.be/Ontology/Sensors/hasTimestampUTC> "%s"^^<http://www.w3.org/2001/XMLSchema#integer> ;
    <https://saref.etsi.org/core/hasValue> %s ;
    <https://saref.etsi.org/core/isMeasuredIn> %s .
"""

VALUE_TEMPLATE = "\"%s\"^^%s"


# MAPPING OF STRING METRICS TO:
#  (i) Observable property class in ontology
#  (ii) Type of feature of interest type
#       (string mapped to feature of interest URI template using other map)
# -> ONLY METRICS ADDED AS A KEY TO THIS MAP ARE MAPPED TO A SEMANTIC EVENT
#    (others yield None with this mapper and are ignored by the Local Monitor)
PROPERTY_MAP = {
    'cpu_load_last_5_minutes': ('CpuLoad', 'device'),
    'cpu_usage_overall': ('CpuUsage', 'device'),
    'disk_space_available': ('DiskSpaceAvailable', 'device'),
    'disk_space_used': ('DiskSpaceUsed', 'device'),
    'ram_available': ('RamAvailable', 'device'),
    'ram_used': ('RamUsed', 'device'),
    'network_rx_rate': ('RxRate', 'device'),
    'network_tx_rate': ('TxRate', 'device'),
    'network_packets_in': ('PacketsReceived', 'device'),
    'network_packets_out': ('PacketsSent', 'device'),
    'network_dropin': ('PacketsReceivedDropped', 'device'),
    'network_dropout': ('PacketsSentDropped', 'device'),
    'network_round_trip_time': ('RoundTripTime', 'device'),
    'network_bandwidth': ('Bandwidth', 'device'),
    'network_delay': ('Delay', 'device'),
    'network_jitter': ('Jitter', 'device'),
    'network_throughput': ('Throughput', 'device'),
    'rsp_stream_event_triples': ('NumberOfStreamEventTriples', 'rdf_stream'),
    'rsp_query_execution_memory_usage': ('RspQueryExecutionMemoryUsage', 'rsp_query'),
    'rsp_query_execution_time': ('RspQueryExecutionTime', 'rsp_query'),
    'rsp_query_processing_time': ('RspQueryProcessingTime', 'rsp_query'),
    'rsp_query_execution_hits': ('RspQueryNumberOfHits', 'rsp_query')
}

FEATURE_OF_INTEREST_MAP = {
    'device': '<https://divide.idlab.ugent.be/meta-model/entity/device/%s>',
    'rdf_stream': '<https://divide.idlab.ugent.be/meta-model/entity/rsp-engine/%s/rdf-stream/%s>',
    'rsp_query': '<https://divide.idlab.ugent.be/meta-model/entity/rsp-engine/%s/rsp-query/%s>'
}

TYPE_MAP = {
    float: '<http://www.w3.org/2001/XMLSchema#float>',
    int: '<http://www.w3.org/2001/XMLSchema#integer>',
    bool: '<http://www.w3.org/2001/XMLSchema#integer>',
    str: '<http://www.w3.org/2001/XMLSchema#string>'
}

# MAPPING OF UNITS TO:
#  (i) Unit URI in the 'ontology of units & measures'
#  (ii) Type of value for a metric with this unit
#       (Python type, mapped to ontology unit class using other map)
UNIT_MAP = {
    'percentage': ('<http://www.ontology-of-units-of-measure.org/resource/om-2/percent>', float),
    'number': ('<http://www.ontology-of-units-of-measure.org/resource/om-2/number>', int),
    'byte': ('<http://www.ontology-of-units-of-measure.org/resource/om-2/byte>', float),
    'bit_per_second': ('<http://www.ontology-of-units-of-measure.org/resource/om-2/bitPerSecond-Time>', float),
    'second': ('<http://www.ontology-of-units-of-measure.org/resource/om-2/second-Time>', float)
}


# HELPER FUNCTIONS

def convert_timestamp_to_string(timestamp):
    return datetime.fromtimestamp(float(timestamp) / 1000.0,
                                  pytz.timezone('Europe/Brussels')). \
        strftime('%Y-%m-%dT%H:%M:%S')


uuid_map = {}
uuid_map_filename = os.path.join(os.path.dirname(os.path.realpath(__file__)), 'uuid_map.pkl')


def generate_uuid(component_id, metric):
    key = '%s.%s' % (component_id, metric)
    if key not in uuid_map:
        uuid_map[key] = 0
    result = uuid_map[key]
    uuid_map[key] += 1
    return result


def remove_whitespace(given_str):
    return ' '.join(given_str.split())


# MAIN FUNCTION FOR TESTING PURPOSES

if __name__ == '__main__':
    # check if argument is provided
    if len(sys.argv) < 4:
        logger.error('Required arguments: component ID, device ID, JSON string of monitoring events')
        exit(1)

    # retrieve JSON arguments
    _component_id = sys.argv[1]
    _device_id = sys.argv[2]
    _events = sys.argv[3]

    try:
        # read in UUID map if it exists
        if os.path.exists(uuid_map_filename):
            with open(uuid_map_filename, 'rb') as f:
                uuid_map = pickle.load(f)
        else:
            uuid_map = {}

        # convert string to JSON
        json_events = json.loads(_events)

        # annotate event as RDF and print to stdout
        _result = annotate_events(component_id=_component_id, device_id=_device_id, events=json_events)
        if _result:
            print(_result)
        else:
            logger.warning('No RDF events could be retrieved from JSON event array')
            exit(1)

        # save UUID map
        with open(uuid_map_filename, 'wb+') as f:
            pickle.dump(uuid_map, f)

    except Exception as _e:
        _msg = 'Unknown exception occurred while semantically annotating events: %s: %s' \
               % (type(_e).__name__, _e)
        logger.error(_msg)
        exit(1)
