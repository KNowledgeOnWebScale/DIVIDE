import json
import sys
import subprocess
import os

import argparse

from paramiko import SSHClient
from scp import SCPClient

DIVIDE_DIRECTORY = '~/.divide'

START_COMMAND_TEMPLATE = 'bash -c \'CENTRAL_SERVER_HOST="%s"; ' \
                         'if [[ "$CENTRAL_SERVER_HOST" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; ' \
                         'then CENTRAL_SERVER_IP="$CENTRAL_SERVER_HOST"; ' \
                         'else CENTRAL_SERVER_IP=$(dig +short "$CENTRAL_SERVER_HOST"); fi; ' \
                         'INTERFACE=$(ip route get "$CENTRAL_SERVER_IP" | grep -oP "dev \K[^ ]+"); ' \
                         'sed -i "s/#INTERFACE#/$INTERFACE/g" %s \' &&' \
                         "mkdir -p %s && cd %s && " \
                         "java -jar %s %s 2>&1 >> %s &"
STOP_COMMAND_TEMPLATE = "kill -9 $(ps -aux | grep '%s' | grep 'java -jar' | grep -v 'grep' | tr -s ' ' | cut -d' ' -f2)"
STATUS_CHECK_COMMAND_TEMPLATE = "ps -aux | grep '%s' | grep -v 'grep'"


def create_ssh_and_scp_clients(ip_address: str):
    ssh = SSHClient()
    ssh.load_system_host_keys()
    ssh.connect(hostname=ip_address, username='divide', allow_agent=False)
    scp = SCPClient(ssh.get_transport())

    return ssh, scp


def start_monitor(ssh_client: SSHClient,
                  scp_client: SCPClient,
                  component_id: str,
                  component_ip_address: str,
                  monitor_jar_path: str,
                  global_monitor_reasoning_service_port: int,
                  global_monitor_reasoning_service_uri: str,
                  divide_central_ip_address: str):
    # create local monitor config
    monitor_config = {
        "component_id": component_id,
        "device_id": component_ip_address,
        "monitor": {
            "rsp": True,
            "network": True,
            "device": True
        },
        "local": {
            "rsp_engine": {
                "monitor": {
                    "ws_port": 54548
                }
            },
            "public_network_interface": "#INTERFACE#"
        },
        "central": {
            "monitor_reasoning_service": {
                "protocol": "http",
                "host": divide_central_ip_address,
                "port": global_monitor_reasoning_service_port,
                "uri": global_monitor_reasoning_service_uri
            }
        }
    }

    # write monitor config to JSON file
    config_file = '%s_config.json' % component_id
    config_folder = os.path.join('.divide', 'monitor', 'local')
    if not os.path.exists(config_folder):
        os.makedirs(config_folder)
    with open(os.path.join(config_folder, config_file), 'w') as config_file_obj:
        json.dump(monitor_config, config_file_obj)

    # copy JAR & config to remote (no check if file exists, always overwrite)
    ssh_client.exec_command('mkdir -p %s' % DIVIDE_DIRECTORY)
    scp_client.put(files=[os.path.join(config_folder, config_file), monitor_jar_path],
                   remote_path=DIVIDE_DIRECTORY)

    # start jar
    jar_name = os.path.basename(monitor_jar_path)
    start_command = START_COMMAND_TEMPLATE % (divide_central_ip_address,
                                              os.path.join(DIVIDE_DIRECTORY, config_file),
                                              os.path.join(DIVIDE_DIRECTORY, 'monitor'),
                                              os.path.join(DIVIDE_DIRECTORY, 'monitor'),
                                              os.path.join(DIVIDE_DIRECTORY, jar_name),
                                              os.path.join(DIVIDE_DIRECTORY, config_file),
                                              os.path.join(DIVIDE_DIRECTORY, 'monitor', 'local_monitor.log'))
    print('Remote command: %s' % start_command)
    transport = ssh_client.get_transport()
    channel = transport.open_session()
    result = channel.exec_command(start_command)
    if result is not None:
        _, stdout, stderr = result
        print('Stdout: %s' % stdout.read().decode('utf-8').strip())
        print('Stderr: %s' % stderr.read().decode('utf-8').strip())


def stop_monitor(ssh_client: SSHClient, monitor_jar_path: str):
    # stop process IF it is active
    if check_monitor(ssh_client=ssh_client, monitor_jar_path=monitor_jar_path):
        jar_name = os.path.basename(monitor_jar_path)
        stop_command = STOP_COMMAND_TEMPLATE % jar_name
        print('Remote command: %s' % stop_command)
        _, stdout, stderr = ssh_client.exec_command(stop_command)
        print('Stdout: %s' % stdout.read().decode('utf-8').strip())
        print('Stderr: %s' % stderr.read().decode('utf-8').strip())


def check_monitor(ssh_client: SSHClient, monitor_jar_path: str):
    # check status of process
    jar_name = os.path.basename(monitor_jar_path)
    check_command = STATUS_CHECK_COMMAND_TEMPLATE % jar_name
    print('Remote command: %s' % check_command)
    _, stdout, stderr = ssh_client.exec_command(check_command)

    # parse response
    stdout_str = stdout.read().decode('utf-8').strip()
    stderr_str = stderr.read().decode('utf-8').strip()
    if not stderr_str and stdout_str:
        print('status:running')
        return True
    else:
        print('Stdout: %s' % stderr_str)
        print('Stderr: %s' % stderr_str)
        print('status:stopped')
        return False


if __name__ == '__main__':
    # define command line arguments
    p = argparse.ArgumentParser()
    p.add_argument('--component-id', required=True)
    p.add_argument('--component-ip-address', required=True)
    p.add_argument('--monitor-jar-path', required=True)
    p.add_argument('--action', choices=['start', 'stop', 'check'], required=True)
    p.add_argument('--global-monitor-reasoning-service-port', required=True)
    p.add_argument('--global-monitor-reasoning-service-uri', required=True)
    p.add_argument('--divide-central-ip-address', required=True)
    parsed_args = p.parse_args(sys.argv[1:])

    # retrieve arguments
    _component_id = str(parsed_args.component_id)
    _component_ip_address = str(parsed_args.component_ip_address)
    _monitor_jar_path = str(parsed_args.monitor_jar_path)
    _action = str(parsed_args.action)
    _global_monitor_reasoning_service_port = int(parsed_args.global_monitor_reasoning_service_port)
    _global_monitor_reasoning_service_uri = str(parsed_args.global_monitor_reasoning_service_uri)
    _divide_central_ip_address = str(parsed_args.divide_central_ip_address)

    # retrieve SSH & SCP clients
    _ssh, _scp = create_ssh_and_scp_clients(ip_address=_component_ip_address)

    # perform correct action
    if _action == 'start':
        start_monitor(ssh_client=_ssh,
                      scp_client=_scp,
                      component_id=_component_id,
                      component_ip_address=_component_ip_address,
                      monitor_jar_path=_monitor_jar_path,
                      global_monitor_reasoning_service_port=_global_monitor_reasoning_service_port,
                      global_monitor_reasoning_service_uri=_global_monitor_reasoning_service_uri,
                      divide_central_ip_address=_divide_central_ip_address)

    elif _action == 'stop':
        stop_monitor(ssh_client=_ssh, monitor_jar_path=_monitor_jar_path)

    elif _action == 'check':
        status = check_monitor(ssh_client=_ssh, monitor_jar_path=_monitor_jar_path)

    # close SSH & SCP clients
    _ssh.close()
    _scp.close()
