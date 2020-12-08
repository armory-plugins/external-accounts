#!/bin/sh

set -ex

echo "Installing plugin $PLUGIN_FILE"

VERSION=$(echo "$PLUGIN_ID" | sed 's/eap-//')

cd /opt/eap

mkdir -p /opt/eap/target/eap/$VERSION
cp /opt/eap/plugins-docker.json /opt/eap/target/eap/plugins.json
cp /opt/eap/$PLUGIN_FILE /opt/eap/target/eap/$VERSION/$PLUGIN_FILE
