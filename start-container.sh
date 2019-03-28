#!/bin/bash
DIR=
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )"  >/dev/null 2>&1 &&  pwd )" || {
    echo "Failed to get current working directory."
    exit 1
}

if [[ -z "$1" ]]; then
    container_name="jaegertracing/all-in-one"
    container_version=1.11
    container="${container_name}:${container_version}"
else
	container="$1"
fi

echo "Starting docker container for ${container} ..."
declare -a command_line
command_line+=("docker")
command_line+=("-D")
command_line+=("--log-level debug")
command_line+=("run")

# -d : start a detached process for the container.
command_line+=("-d")
command_line+=("--name")
command_line+=("jaeger_all_in_one")

# --rm : delete the container when it stops.
command_line+=("--rm")

# expose ports.
command_line+=("-p5775:5775/udp")
command_line+=("-p6831:6831/udp")
command_line+=("-p6832:6832/udp")
command_line+=("-p5778:5778")
command_line+=("-p16686:16686")
command_line+=("-p14268:14268")
command_line+=("-p9411:9411")
command_line+=("-p5022:22")

command_line+=("${container}")

container_id=
container_id="$(${command_line[*]})"
declare -i status=$?
[[ ${status} -ne 0 ]] && {
    echo "FAILED: docker command: [${command_line[*]}]. Status is ${status}."
    exit 1
}

container_pid=
container_pid="$(docker inspect --format='{{.State.Pid}}' "${container_id}")" || {
    echo "Failed to get PID for docker container ${container_id}."
    exit 1
}	

echo "Started container ${container} (id: ${container_id}) as process Id ${container_pid} OK."
