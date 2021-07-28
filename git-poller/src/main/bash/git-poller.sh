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
GIT_USER=${GIT_USER}
GIT_PASS=${GIT_PASS}
TOKEN=${TOKEN}

trap cleanup INT

function validate() {
  if [[ -z $REPO || "$REPO" == "" ]]; then echo "Environment variable REPO is not defined" && exit 1; fi
  if [[ -n $GIT_USER && -z $GIT_PASS ]]; then echo "GIT_USER supplied without GIT_PASS" && exit 1; fi
  if ! command -v git &>/dev/null; then echo "\"git\" command is not available on PATH" && exit 1; fi
}

function extract_host_from_repo() {
  if [[ $REPO =~ ^git ]] ; then
    TARGET_HOST=$(echo "$REPO" | sed 's|git@||' | sed 's|:.*||')
  elif [[ $REPO =~ ^ssh ]] ; then
    TARGET_HOST=$(echo "$REPO" | sed 's|ssh://git@||' | sed 's|:.*||')
  elif [[ $REPO =~ ^http ]] ; then
    TARGET_HOST=$(echo "$REPO" | sed -E 's|http(s)?://||' | sed 's|[:/].*||')
  fi
}

function add_repo_to_known_hosts() {
  mkdir -p /root/.ssh
  echo "Adding key for host $TARGET_HOST to known_hosts"
  set -x
  ssh-keyscan -t rsa "$TARGET_HOST" >> /root/.ssh/known_hosts
  set x
}

function url_encode() {
  old_lc_collate=$LC_COLLATE
  LC_COLLATE=C

  local length="${#1}"
  for (( i = 0; i < length; i++ )); do
    local c="${1:$i:1}"
    case $c in
      [a-zA-Z0-9.~_-]) printf '%s' "$c" ;;
      *) printf '%%%02X' "'$c" ;;
    esac
  done

  LC_COLLATE=$old_lc_collate
}

function build_clone_url() {
  if [[ -n $GIT_USER  ]]; then
    ENCODED_USERNAME=$(url_encode "$GIT_USER")
    ENCODED_PASSWORD=$(url_encode "$GIT_PASS")
    SCHEME=$(echo "$REPO" | sed 's|:.*||')
    CTX_PATH=$(echo "$REPO" | sed "s|$SCHEME://$TARGET_HOST||")
    CLONE_URL="$SCHEME://$ENCODED_USERNAME:$ENCODED_PASSWORD@$TARGET_HOST$CTX_PATH"
  elif [[ -n $TOKEN  ]]; then
    ENCODED_TOKEN=$(url_encode "$TOKEN")
    SCHEME=$(echo "$REPO" | sed 's|:.*||')
    CTX_PATH=$(echo "$REPO" | sed "s|$SCHEME://$TARGET_HOST||")
    CLONE_URL="$SCHEME://$ENCODED_TOKEN@$TARGET_HOST$CTX_PATH"
  else
    CLONE_URL=$REPO
  fi
}

function extract_repo_name() {
  REPO_NAME=$(echo "$REPO" | sed 's|.*/||' | sed 's|.git||')
}

function clone() {
  extract_host_from_repo
  if [[ ! -f /root/.ssh/known_hosts || -w /root/.ssh/known_hosts ]] ; then
    if [[ -w /root/.ssh && -w /root/.ssh/known_hosts ]] ; then
      add_repo_to_known_hosts
    fi
  fi
  if [[ -f /root/.ssh/id_rsa && -w /root/.ssh/id_rsa ]] ; then
    chmod 400 /root/.ssh/id_rsa
  fi

  # Don't clone again if we're recovering from a crash and the clone already exists
  extract_repo_name
  if [[ -d "${LOCAL_CLONE_DIR}/${REPO_NAME}" ]] ; then echo "Directory ${LOCAL_CLONE_DIR}/${REPO_NAME} already exists, skipping clone step" ; return ; fi

  build_clone_url
  echo "Cloning $REPO into $LOCAL_CLONE_DIR"
  if [[ -d "LOCAL_CLONE_DIR" ]] ; then mkdir -p "$LOCAL_CLONE_DIR" ; fi
  cd "$LOCAL_CLONE_DIR"
  git clone --branch "$BRANCH" --depth 1 "$CLONE_URL"
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
