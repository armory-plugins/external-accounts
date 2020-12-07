FROM busybox:latest
MAINTAINER engineering@armory.io

ARG PLUGIN_ID
ENV PLUGIN_FILE=${PLUGIN_ID}.zip
RUN mkdir -p /opt/plugin

ADD distributions/${PLUGIN_ID}.zip /opt/plugin/
ADD distributions/install.sh /usr/local/bin/install.sh

RUN adduser -D spinnaker && addgroup spinnaker spinnaker

CMD ["install.sh"]
