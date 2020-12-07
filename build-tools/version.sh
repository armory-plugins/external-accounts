#!/bin/bash

#------------------------------------------------------------------------------------------------------------------
# Calculates the next version based on git tags and current branch.
#
# Examples:
# * Snapshot version: 0.1.0-snapshot.[uncommitted].chore.foo.bar.test.050f9cd
# * RC version:       0.1.0-rc.9
# * Release version:  0.1.0
#
# Step logic:
# * On release branch, only patch version is stepped depending on the latest git tag matching the branch name
# * On all other branches, if latest tag is not rc, step minor and set patch=0. Otherwise, step rc number
#------------------------------------------------------------------------------------------------------------------

VERSION_TYPE=${VERSION_TYPE:-snapshot}
BRANCH=${BRANCH:-$(git rev-parse --abbrev-ref HEAD)}

[[ ! $VERSION_TYPE =~ snapshot|rc|release ]] && echo "Usage: $(basename "$0"). Optional environment variables: VERSION_TYPE=snapshot|rc|release, BRANCH" && exit 1

function first_version() {
  case $BRANCH in
  release-*)
    tmp=$(echo "$BRANCH" | cut -d'-' -f 2)
    read -r br_major br_minor br_patch <<<"${tmp//./ }"
    current_branch_version="${br_major}.${br_minor}.0-rc.0"
    ;;
  *)
    current_branch_version=0.1.0-rc.0
    ;;
  esac
}

function get_current_branch_version() {
  case $BRANCH in
  release-*)
    tmp=$(echo "$BRANCH" | cut -d'-' -f 2)
    read -r br_major br_minor br_patch <<<"${tmp//./ }"
    current_branch_version=$(git tag --sort=-v:refname | grep "v$br_major.$br_minor" | head -1 | sed 's|v||g')
    ;;
  *)
    current_branch_version=$(git tag --sort=-v:refname | head -1 | sed 's|v||g')
    ;;
  esac

  if [[ -z $current_branch_version ]]; then
    first_version
  fi
}

function split_in_version_parts() {
  full_version=$1
  read -r major minor patch rc <<<"${full_version//./ }"
  patch=$(echo "$patch" | sed 's|[^0-9]*||g') # Remove "-rc" from patch part
}

function step_version() {
  SOLID_VERSION_RELEASED=$([[ -n $(git tag -l "v$major.$minor.$patch") ]] && echo 1 || echo 0)

  case $BRANCH in
  release-*)
    if [[ $SOLID_VERSION_RELEASED = 1 ]]; then
      ((patch++))
      rc=1
    else
      ((rc++))
    fi
    ;;
  *)
    if [[ $SOLID_VERSION_RELEASED = 1 ]]; then
      ((minor++))
      patch=0
      rc=1
    else
      ((rc++))
    fi
    ;;
  esac
}

function format_version() {
  case $VERSION_TYPE in
  snapshot)
    if [ "x$(git status --porcelain)" != "x" ]; then u=".uncommitted"; fi
    br=$(echo ".$BRANCH" | sed 's|[-/_]|.|g')
    commit=$(git rev-parse --short HEAD)
    output_version="${major}.${minor}.${patch}-snapshot$u$br.$commit"
    ;;
  rc)
    output_version="${major}.${minor}.${patch}-rc.${rc}"
    ;;
  release)
    output_version="${major}.${minor}.${patch}"
    ;;
  esac
}

get_current_branch_version
split_in_version_parts "$current_branch_version"
#echo "Current version: major: $major, minor: $minor, patch: $patch, rc: $rc"
step_version
#echo "New version: major: $major, minor: $minor, patch: $patch, rc: $rc"
format_version

echo -n "$output_version"
