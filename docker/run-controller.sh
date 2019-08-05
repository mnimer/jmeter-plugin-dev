#!/bin/bash
#
# Run JMeter Docker image with options

NAME="jmeter-controller"
IMAGE="rossrick/jmeter-controller:5.1.1"

# Finally run
sudo docker stop ${NAME} > /dev/null 2>&1
sudo docker rm ${NAME} > /dev/null 2>&1
#no environment variables
#sudo docker run --name ${NAME} -p 127.0.0.1:5901:5901/tcp -i -v ${PWD}:${PWD} -w ${PWD} ${IMAGE} $@
# with the REMOTE_HOSTS environment variable
sudo docker run --name ${NAME} -e REMOTE_HOSTS='jmeter-remote-1.c.proj.internal' -p 127.0.0.1:5901:5901/tcp -i -v ${PWD}:${PWD} -w ${PWD} ${IMAGE} $@
