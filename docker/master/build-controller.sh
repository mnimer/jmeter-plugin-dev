#!/bin/bash

JMETER_VERSION="5.1.1"
# Example build line
#sudo docker build -f ControllerDockerfile --build-arg IMAGE_TIMEZONE="America/Chicago" --build-arg JMETER_VERSION=${JMETER_VERSION} -t "mikenimer/jmeter-controller:${JMETER_VERSION}"
gcloud builds submit --config=cloudbuild.yaml .
