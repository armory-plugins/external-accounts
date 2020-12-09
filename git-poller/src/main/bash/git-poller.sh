#!/bin/bash

set -e

if command -v install-plugin &>/dev/null; then
  echo "Installing plugin.."
  install-plugin
fi

TMP_DIR=$(mktemp -d)

REPO=${REPO}
BRANCH=${BRANCH:-master}
LOCAL_CLONE_DIR=${LOCAL_CLONE_DIR:-$TMP_DIR}
SYNC_INTERVAL_SECS=${SYNC_INTERVAL_SECS:-60}

trap cleanup INT

function validate() {
  if [[ -z $REPO || "$REPO" == "" ]]; then echo "Environment variable REPO is not defined" && exit 1; fi
  if ! command -v git &>/dev/null; then echo "\"git\" command is not available on PATH" && exit 1; fi
}

function add_repo_to_known_hosts() {
  if [[ $REPO =~ ^git ]] ; then
    TARGET_HOST=$(echo "$REPO" | sed 's|git@||' | sed 's|:.*||')
  elif [[ $REPO =~ ^ssh ]] ; then
    TARGET_HOST=$(echo "$REPO" | sed 's|ssh://git@||' | sed 's|:.*||')
  elif [[ $REPO =~ ^http ]] ; then
    TARGET_HOST=$(echo "$REPO" | sed -E 's|http(s)?://||' | sed 's|[:/].*||')
  fi

  mkdir -p /root/.ssh
  echo "Adding key for host $TARGET_HOST to known_hosts"
  set -x
  ssh-keyscan -t rsa "$TARGET_HOST" >> /root/.ssh/known_hosts
  set x
}

function clone() {
  if [[ ! -f /root/.ssh/known_hosts || -w /root/.ssh/known_hosts ]] ; then
    add_repo_to_known_hosts
  fi
  if [[ -f /root/.ssh/id_rsa && -w /root/.ssh/id_rsa ]] ; then
    chmod 400 /root/.ssh/id_rsa
  fi
  echo "Cloning $REPO into $LOCAL_CLONE_DIR"
  if [[ -d "LOCAL_CLONE_DIR" ]] ; then mkdir -p "$LOCAL_CLONE_DIR" ; fi
  cd "$LOCAL_CLONE_DIR"
  set -x
  git clone --branch "$BRANCH" --depth 1 "$REPO"
  set x
}

function pull_forever() {
  set -x
  cd "$LOCAL_CLONE_DIR"
  # shellcheck disable=SC2035
  cd */
  while true ; do
    git pull
    sleep "$SYNC_INTERVAL_SECS"
  done
  set x
}

function cleanup() {
  echo "Cleanly finishing script"
  rm -rf "$LOCAL_CLONE_DIR"
  exit 0
}

validate
echo "REPO: $REPO"
echo "BRANCH: $BRANCH"
echo "LOCAL_CLONE_DIR: $LOCAL_CLONE_DIR"
echo "SYNC_INTERVAL_SECS: $SYNC_INTERVAL_SECS"
echo ""
clone
pull_forever
