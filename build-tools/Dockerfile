FROM debian:buster
MAINTAINER engineering@armory.io

ARG PLUGIN_ID

ENV PLUGIN_FILE=${PLUGIN_ID}.zip
ENV PLUGIN_ID=${PLUGIN_ID}

RUN apt-get update && apt-get upgrade -y
RUN mkdir -p /opt/eap

ADD distributions/${PLUGIN_ID}.zip /opt/eap/
ADD distributions/plugins-docker.json /opt/eap/
ADD distributions/install.sh /usr/local/bin/install.sh

RUN adduser --disabled-password --gecos "" spinnaker \
  && addgroup spinnaker spinnaker

CMD ["install.sh"]
