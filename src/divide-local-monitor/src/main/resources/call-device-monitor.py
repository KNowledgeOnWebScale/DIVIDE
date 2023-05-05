import json
import logging
import os
import time

import psutil


# configure file logging
logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s;%(name)s;%(levelname)s;%(message)s')
logging_dir = os.path.join(os.path.expanduser('~'), '.divide', 'monitor')
if not os.path.exists(logging_dir):
    os.makedirs(logging_dir, exist_ok=True)
handler = logging.FileHandler(os.path.join(logging_dir, 'device-monitor.log'))
handler.setLevel(logging.INFO)
handler.setFormatter(formatter)
logger.addHandler(handler)


def collect_device_statistics():
    # get current timestamp in milliseconds
    t = int(time.time() * 1000)

    # collect data
    cpu_percentage = psutil.cpu_percent(percpu=False, interval=1)
    cpu_percentage_per_core = psutil.cpu_percent(percpu=True, interval=1)
    # percentage calculation below: see docs at https://psutil.readthedocs.io/en/latest/#psutil.getloadavg
    cpu_load_average = [round(x / psutil.cpu_count() * 100, 2) for x in psutil.getloadavg()]
    mem = psutil.virtual_memory()
    disk_usage = psutil.disk_usage('/')

    # create JSON blob
    result = [
        {'time': t, 'metric': 'cpu_usage_overall',
         'value': cpu_percentage, 'unit': 'percentage'},
        {'time': t, 'metric': 'cpu_usage_per_core',
         'value': cpu_percentage_per_core, 'unit': 'percentage'},
        {'time': t, 'metric': 'cpu_load_last_1_minutes',
         'value': cpu_load_average[0], 'unit': 'percentage'},
        {'time': t, 'metric': 'cpu_load_last_5_minutes',
         'value': cpu_load_average[1], 'unit': 'percentage'},
        {'time': t, 'metric': 'cpu_load_last_15_minutes',
         'value': cpu_load_average[2], 'unit': 'percentage'},

        {'time': t, 'metric': 'ram_available',
         'value': mem.available, 'unit': 'byte'},
        {'time': t, 'metric': 'ram_used',
         'value': mem.used, 'unit': 'byte'},
        {'time': t, 'metric': 'ram_available',
         'value': round(100 - mem.percent, 2), 'unit': 'percentage'},

        {'time': t, 'metric': 'disk_space_available',
         'value': disk_usage.free, 'unit': 'byte'},
        {'time': t, 'metric': 'disk_space_available',
         'value': round(100 - disk_usage.percent, 2), 'unit': 'percentage'}
    ]
    for r in result:
        logger.info(r)
    return result


if __name__ == '__main__':
    status = collect_device_statistics()
    print(json.dumps(status))
