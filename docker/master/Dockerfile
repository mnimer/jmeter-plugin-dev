# inspired by https://github.com/hauptmedia/docker-jmeter  and
# https://github.com/hhcordero/docker-jmeter-server/blob/master/Dockerfile
FROM ubuntu:16.04

ARG JMETER_VERSION="5.1.1"
ENV JMETER_HOME /opt/apache-jmeter-${JMETER_VERSION}
ENV	JMETER_BIN	${JMETER_HOME}/bin
ENV	JMETER_DOWNLOAD_URL  https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz

ENV ETC_ENV=/etc/environment

# VNC Setup
ENV HOME_FOLDER=/root
ENV VNC_FOLDER=${HOME_FOLDER}/.vnc
ENV VNC_CONFIG_FOLDER=${HOME_FOLDER}/.config
ENV VNC_XSTARTUP=${VNC_FOLDER}/xstartup
ENV VNC_PWD=jm3t3rS3


# blank out RMI_HOST_DEF if it is not needed
# see jmeter-server for more information
ENV	RMI_HOST_DEF=

# Install OS & Utils
RUN    apt-get update -y \
	&& apt-get upgrade -y \
	&& apt-get install apt-utils -y \
	&& apt-get install curl vim iputils-ping gedit -y \
	&& DEBIAN_FRONTEND=noninteractive apt-get install keyboard-configuration locales -y
	
# Create a locale file
RUN sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen && \
    dpkg-reconfigure --frontend=noninteractive locales && \
    update-locale LANG=en_US.UTF-8

ENV LANG en_US.UTF-8
	
# Install Java
RUN    apt-get install default-jre -y \
	&& apt-get install default-jdk -y \
	&& echo "JAVA_HOME=\"/usr/lib/jvm/java-8-openjdk-amd64/jre\"" > ${ETC_ENV} 

# Install JMeter	
RUN	   mkdir -p /tmp/dependencies  \
    && curl -L --silent ${JMETER_DOWNLOAD_URL} >  /tmp/dependencies/apache-jmeter-${JMETER_VERSION}.tgz  \
    && mkdir -p /opt  \
    && tar -xzf /tmp/dependencies/apache-jmeter-${JMETER_VERSION}.tgz -C /opt  \
    && rm -rf /tmp/dependencies

# Install VNC packages and configure it
RUN    apt-get remove --purge nautilus -y \
	&& printf "Install gnome" \
	&& apt-get install gnome-core -y \
	&& printf "Install gnome extras" \
	&& apt-get install gnome-panel gnome-settings-daemon metacity nautilus gnome-terminal -y \
	&& printf "Install vncServer" \
	&& apt-get install vnc4server -y \
	&& printf "Install gtk lib" \
	&& apt install libcanberra-gtk-module libcanberra-gtk3-module 
	
# Configure VNC
RUN echo "Install VNC"
RUN	   mkdir ${VNC_FOLDER} \
	&& printf "$VNC_PWD\n$VNC_PWD\n\n" | vncpasswd \
	&& printf "#!/bin/sh\n" >> ${VNC_XSTARTUP} \
	&& printf "[ -x /etc/vnc/xstartup ] && exec /etc/vnc/xstartup\n" >> ${VNC_XSTARTUP} \
	&& printf "[ -r $HOME/.Xresources ] && xrdb $HOME/.Xresources\n" >> ${VNC_XSTARTUP} \
	&& printf "xsetroot -solid grey\n" >> ${VNC_XSTARTUP} \
	&& printf "vncconfig -iconic &\n" >> ${VNC_XSTARTUP} \
	&& printf "x-terminal-emulator -geometry 80x24+10+10 -ls -title "$VNCDESKTOP Desktop" &\n" >> ${VNC_XSTARTUP} \
	&& printf "x-window-manager &\n" >> ${VNC_XSTARTUP} \
	&& printf "\n" >> ${VNC_XSTARTUP} \	
	&& printf "metacity &\n" >> ${VNC_XSTARTUP} \	
	&& printf "gnome-settings-daemon &\n" >> ${VNC_XSTARTUP} \	
	&& printf "gnome-panel &\n" >> ${VNC_XSTARTUP} \	
	&& printf "nautilus &\n" >> ${VNC_XSTARTUP} \	
	&& chmod 755 ${VNC_XSTARTUP} \
	&& printf "\$geometry = \"1400x850\"\n" > ${HOME_FOLDER}/.vncrc \
	&& . ${ETC_ENV} \
	&& mkdir -p ${VNC_CONFIG_FOLDER}/nautilus
	

# Disable SSL for easier remote setup
RUN printf "\nserver.rmi.ssl.disable=true\n" >> ${JMETER_HOME}/bin/user.properties && cat ${JMETER_HOME}/bin/user.properties

# Set global PATH such that "jmeter" command is found
ENV PATH $PATH:$JMETER_BIN

# Entrypoint has same signature as "jmeter" command
COPY entrypoint.sh /

# TODO - install gcloud/gsutil on VM (not docker image)
# TODO - install git, mvn
# TODO - git clone this plugin " https://source.developers.google.com/p/swarm-tools/r/jmeter-gcp-plugins"
# TODO - build plugin
# TODO - copy plugin jar (from /target folder) into /<jmeter path>/lib/ext folder

# TODO - install blazeds PluginManager - https://www.blazemeter.com/blog/how-install-jmeter-plugins-manager/

# TODO - Change VM so it doesn't die when you restart jmeter, should not be hard linked to the jmeter app


WORKDIR	${JMETER_HOME}

EXPOSE 5901

ENTRYPOINT /entrypoint.sh
