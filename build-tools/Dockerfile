FROM debian:buster
MAINTAINER engineering@armory.io

ARG PLUGIN_ID

ENV PLUGIN_FILE=${PLUGIN_ID}.zip
ENV PLUGIN_ID=${PLUGIN_ID}

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install coreutils git -y
RUN apt-get remove --purge patch -y
RUN mkdir -p /opt/eap

ADD distributions/${PLUGIN_ID}.zip /opt/eap/
ADD distributions/plugins-docker.json /opt/eap/
ADD distributions/install.sh /usr/bin/install-plugin
ADD distributions/git-poller.sh /usr/bin/git-poller

CMD ["install-plugin"]
