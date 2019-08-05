#!/bin/bash
# Inspired from https://github.com/hhcordero/docker-jmeter-client
# Basically runs jmeter, assuming the PATH is set to point to JMeter bin-dir (see Dockerfile)
#
# This script expects the standdard JMeter command parameters.
#
# Run vncserver
echo "Starting VNCServer"
rm -rf /tmp/.X1-lock
vncserver &

# Wait a bit to give vncserver a chance to start
sleep 20s

set -e
freeMem=`awk '/MemFree/ { print int($2/1024) }' /proc/meminfo`
s=$(($freeMem/10*8))
x=$(($freeMem/10*8))
n=$(($freeMem/10*2))
export JVM_ARGS="-Xmn${n}m -Xms${s}m -Xmx${x}m"

export DISPLAY=:1

echo "START Running Jmeter on `date`"
echo "JVM_ARGS=${JVM_ARGS}"
echo "jmeter args=$@"

if [ -z "$REMOTE_HOSTS" ]
then
      echo "\$REMOTE_HOSTS is empty"
	 
else
      echo "\$REMOTE_HOSTS is NOT empty: $REMOTE_HOSTS"
	  # append to existing user properties file
	  echo "\$JMETER_HOME is $JMETER_HOME"
	  printf "remote_hosts=$REMOTE_HOSTS" >> $JMETER_HOME/bin/user.properties
	  cat $JMETER_HOME/bin/user.properties
fi

jmeter $@

# Keep entrypoint simple: we must pass the standard JMeter arguments
#jmeter $@
#echo "END Running Jmeter on `date`"

#     -n \
#    -t "/tests/${TEST_DIR}/${TEST_PLAN}.jmx" \
#    -l "/tests/${TEST_DIR}/${TEST_PLAN}.jtl"
# exec tail -f jmeter.log
#    -D "java.rmi.server.hostname=${IP}" \
#    -D "client.rmi.localport=${RMI_PORT}" \
#  -R $REMOTE_HOSTS
