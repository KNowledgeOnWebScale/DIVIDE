import json
import logging
import os
import subprocess
import sys
import time
from multiprocessing.pool import Pool
from threading import Thread

import psutil


# configure file logging
logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s;%(name)s;%(levelname)s;%(message)s')
logging_dir = os.path.join(os.path.expanduser('~'), '.divide', 'monitor')
if not os.path.exists(logging_dir):
    os.makedirs(logging_dir, exist_ok=True)
handler = logging.FileHandler(os.path.join(logging_dir, 'network-monitor.log'))
handler.setLevel(logging.INFO)
handler.setFormatter(formatter)
logger.addHandler(handler)


def print_network_statistics(statistics):
    # get current timestamp in milliseconds
    t = int(time.time() * 1000)

    # append time to all statistics
    for statistic in statistics:
        statistic['time'] = t

    # print a JSON dump of the statistics
    print(json.dumps(statistics))

    # log statistics to log file
    for s in statistics:
        logger.info(s)



# MONITORING OF NETWORK STATISTICS ON RELEVANT INTERFACE

# noinspection PyTypeChecker
def collect_network_statistics(inf: str, pool: Pool, interval: int = 1):
    """
    Cool network statistics for the given interface and let them print asynchronously
    as JSON to stdout using the given multiprocessing pool.
    """
    # get initial values
    net_stat = psutil.net_io_counters(pernic=True, nowrap=True)[inf]
    bytes_in_old = net_stat.bytes_recv
    bytes_out_old = net_stat.bytes_sent
    packets_in_old = net_stat.packets_recv
    packets_out_old = net_stat.packets_sent
    dropin_old = net_stat.dropin
    dropout_old = net_stat.dropout

    # loop to update every interval
    while True:
        time.sleep(interval)

        # collect updated values
        net_stat = psutil.net_io_counters(pernic=True, nowrap=True)[inf]
        bytes_in_new = net_stat.bytes_recv
        bytes_out_new = net_stat.bytes_sent
        packets_in_new = net_stat.packets_recv
        packets_out_new = net_stat.packets_sent
        dropin_new = net_stat.dropin
        dropout_new = net_stat.dropout

        # calculate Tx & Rx values over last second in bit/s
        # (values are in bytes, but since we take the difference every second,
        #  we automatically have bytes per second -> * 8 to get bits per second)
        rx_rate = (bytes_in_new - bytes_in_old) * 8
        tx_rate = (bytes_out_new - bytes_out_old) * 8

        # calculate number & percentage of incoming & outgoing packets dropped
        net_dropin = dropin_new - dropin_old
        net_dropout = dropout_new - dropout_old
        net_packets_in = packets_in_new - packets_in_old
        net_packets_out = packets_out_new - packets_out_old
        net_dropin_pct = 0 if net_packets_in == 0 else round(net_dropin / net_packets_in * 100, 2)
        net_dropout_pct = 0 if net_packets_out == 0 else round(net_dropout / net_packets_out * 100, 2)

        # send statistics as JSON to stdout
        statistics = [{'metric': 'network_rx_rate', 'value': rx_rate, 'unit': 'bit_per_second'},
                      {'metric': 'network_tx_rate', 'value': tx_rate, 'unit': 'bit_per_second'},
                      {'metric': 'network_packets_in', 'value': net_packets_in, 'unit': 'number'},
                      {'metric': 'network_packets_out', 'value': net_packets_out, 'unit': 'number'},
                      {'metric': 'network_dropin', 'value': net_dropin, 'unit': 'number'},
                      {'metric': 'network_dropout', 'value': net_dropout, 'unit': 'number'},
                      {'metric': 'network_dropin', 'value': net_dropin_pct, 'unit': 'percentage'},
                      {'metric': 'network_dropout', 'value': net_dropout_pct, 'unit': 'percentage'}]
        pool.apply_async(print_network_statistics, args=(statistics,))

        # overwrite old values
        bytes_in_old = bytes_in_new
        bytes_out_old = bytes_out_new
        packets_in_old = packets_in_new
        packets_out_old = packets_out_new
        dropin_old = dropin_new
        dropout_old = dropout_new


# CENTRAL SERVER PING TO MONITOR NETWORK ROUND TRIP TIME

def ping_central_server(ip: str, interval: int = 1):
    # start a ping process that pings every second
    popen = subprocess.Popen(['ping', '-i %d' % interval, ip],
                             stdout=subprocess.PIPE,
                             stderr=subprocess.STDOUT,
                             universal_newlines=True)
    # yield every output line
    for stdout_line in iter(popen.stdout.readline, ""):
        yield stdout_line
    popen.stdout.close()


def process_ping_result(ping_result: str):
    try:
        # try to process the expected output with a time measure
        statistics = ping_result.split('time=')

        # only continue if this output is actually there
        if len(statistics) > 1:
            rtt_ms = float(statistics[-1].split(' ')[0].strip())
            rtt_s = rtt_ms / 1000.0
            statistics = [{'metric': 'network_round_trip_time', 'value': rtt_s, 'unit': 'second'}]
            print_network_statistics(statistics=statistics)

    except Exception:
        # ignore
        pass


if __name__ == '__main__':
    # check if argument is provided
    if len(sys.argv) < 3:
        print('Usage: python3 call-network-monitor.py <central_server_host_or_ip> <network_interface> '
              '[ <ping_interval_s> ] [ <monitor_interval_s> ]')
        print('  -> <central_server_host_or_ip> is pinged periodically to monitor network round trip time')
        print('  -> <network_interface> is used to monitor network traffic (incoming & dropped packets/bytes)')
        print('  -> <ping_interval_s> defines interval (in seconds) between successive pings (default is 1)')
        print('  -> <monitor_interval_s> defines interval (in seconds) between updates of network traffic'
              ' (default is 1)')
        exit(1)

    # retrieve arguments
    central_server_host = sys.argv[1]
    network_interface = sys.argv[2]
    _ping_interval = int(sys.argv[3]) if len(sys.argv) > 3 else 1
    _monitor_interval = int(sys.argv[4]) if len(sys.argv) > 4 else 1
    logger.info(json.dumps({'ip': central_server_host, 'interface': network_interface,
                            'monitor_interval': _monitor_interval, 'ping_interval': _ping_interval}))

    # create processing pool to asynchronously print results
    _pool = Pool(processes=1)

    # start the collection of network statistics
    thread = Thread(target=collect_network_statistics, args=(network_interface, _pool, _monitor_interval,))
    thread.start()

    # ping the central server & process every result
    for _ping_result in ping_central_server(ip=central_server_host, interval=_ping_interval):
        process_ping_result(ping_result=_ping_result)
