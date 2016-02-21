#!/bin/bash -e

# Copyright 2015 OpenStack Foundation
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

GERRIT_SITE=$1
GIT_ORIGIN=$2

CI_USER=jenkins
CI_GROUP=users

if [ -z "$CLEAN_DIR" ]; then
    CLEAN_DIR=0
fi

echo "Running with the following parameters:"
echo "  GERRIT_SITE => $GERRIT_SITE"
echo "  GIT_ORIGIN => $GIT_ORIGIN"
echo "  BRANCH => $BRANCH"
echo "  ZUUL_URL => $ZUUL_URL"
echo "  ZUUL_REF => $ZUUL_REF"
echo "  ZUUL_NEWREV => $ZUUL_NEWREV"
echo "  ZUUL_PROJECT => $ZUUL_PROJECT"
echo

if [ -z "$GERRIT_SITE" ]; then
    echo "The gerrit site name (eg 'https://review.openstack.org') must be the first argument."
    exit 1
fi

if [ -z "$GIT_ORIGIN" ] || [ -n "$ZUUL_NEWREV" ]; then
    GIT_ORIGIN="$GERRIT_SITE/p"
    # git://git.openstack.org/
    # https://review.openstack.org/p
fi

if [ -z "$ZUUL_REF" ]; then
    if [ -n "$BRANCH" ]; then
        echo "No ZUUL_REF so using requested branch $BRANCH from origin."
        ZUUL_REF=$BRANCH
        # use the origin since zuul mergers have outdated branches
        ZUUL_URL=$GIT_ORIGIN
    else
        echo "Provide either ZUUL_REF or BRANCH in the calling enviromnent."
        exit 1
    fi
fi

if [ -z "$ZUUL_PROJECT" ]; then
    echo "No $ZUUL_PROJECT specified; using $PROJECT"
    if [ -n "$PROJECT" ]; then
        echo "No ZUUL_PROJECT so using requested $PROJECT from origin."
        ZUUL_PROJECT=$PROJECT
    else
        echo "Provide either ZUUL_PROJECT or PROJECT in the calling environment."
        exit 1
    fi
fi

if [ -z "$ZUUL_URL" ]; then
    echo "The ZUUL_URL must be provided."
    exit 1
fi

if [ ! -z "$ZUUL_CHANGE" ]; then
    echo "Triggered by: $GERRIT_SITE/$ZUUL_CHANGE"
fi

if [ -d $ZUUL_PROJECT ]; then
    pushd $ZUUL_PROJECT
    if output=$(git status --untracked-files=no --porcelain) && [ -z "$output" ]; then
        echo "$ZUUL_PROJECT clean and usable"
    else
        CLEAN_DIR=1
    fi
    popd
fi

if [ -d $ZUUL_PROJECT ] && [ $CLEAN_DIR -eq 1 ]; then
    echo "Cleaning $ZUUL_PROJECT"
    rm -fr $ZUUL_PROJECT
fi

set -x
if [[ ! -e $ZUUL_PROJECT/.git ]]; then
    ls -a
    rm -fr .[^.]* *
    if [ -d /opt/git/$ZUUL_PROJECT/.git ]; then
        git clone file:///opt/git/$ZUUL_PROJECT $ZUUL_PROJECT
    else
        git clone $GIT_ORIGIN/asterisk/$ZUUL_PROJECT.git $ZUUL_PROJECT
    fi
fi

chown -R $CI_USER:$CI_GROUP $ZUUL_PROJECT
pushd $ZUUL_PROJECT

git remote set-url origin $GIT_ORIGIN/asterisk/$ZUUL_PROJECT.git

# attempt to work around bugs 925790 and 1229352
if ! git remote update; then
    echo "The remote update failed, so garbage collecting before trying again."
    git gc
    git remote update
fi

git reset --hard
if ! git clean -x -f -d -q ; then
    sleep 1
    git clean -x -f -d -q
fi

if echo "$ZUUL_REF" | grep -q ^refs/tags/; then
    git fetch --tags $ZUUL_URL/$ZUUL_PROJECT
    git checkout $ZUUL_REF
    git reset --hard $ZUUL_REF
elif [ -z "$ZUUL_NEWREV" ]; then
    if [ "$ZUUL_URL" == "$GIT_ORIGIN" ]; then
        git fetch $ZUUL_URL/asterisk/$ZUUL_PROJECT.git $ZUUL_REF
    else
        git fetch $ZUUL_URL/$ZUUL_PROJECT $ZUUL_REF
    fi
    git checkout FETCH_HEAD
    git reset --hard FETCH_HEAD
else
    git checkout $ZUUL_NEWREV
    git reset --hard $ZUUL_NEWREV
fi

if ! git clean -x -f -d -q ; then
    sleep 1
    git clean -x -f -d -q
fi

if [ -f .gitmodules ]; then
    git submodule init
    git submodule sync
    git submodule update --init
fi

popd

