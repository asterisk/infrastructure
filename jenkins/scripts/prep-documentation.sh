#!/usr/bin/env bash

#
# Retrieves the astxml2wiki script for use in updating wiki documentation
#
# Copyright 2016 (C), Digium, Inc.
# Joshua Colp <jcolp@digium.com>
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
#	http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

GIT_ASTXML2WIKI_ORIGIN=$1
GIT_ASTERISK_ORIGIN=$2
BRANCH=$3

CI_USER=jenkins
CI_GROUP=users

echo "Running with the following parameters:"
echo "  GIT_ASTXML2WIKI_ORIGIN => $GIT_ASTXML2WIKI_ORIGIN"
echo "  GIT_ASTERISK_ORIGIN => $GIT_ASTERISK_ORIGIN"
echo "  BRANCH => $BRANCH"

CLEAN_DIR=0

if [ -d ./astxml2wiki ]; then
    pushd astxml2wiki
    if output=$(git status --untracked-files=no --porcelain) && [ -z "$output" ]; then
        echo "astxml2wiki clean and usable"
    else
        CLEAN_DIR=1
    fi
    popd
fi

if [ -d ./astxml2wiki ] && [ $CLEAN_DIR -eq 1 ]; then
    echo "Cleaning astxml2wiki"
    rm -fr astxml2wiki
fi

if [ ! -d ./astxml2wiki ]; then
    git clone $GIT_ASTXML2WIKI_ORIGIN astxml2wiki
fi

pushd astxml2wiki

git remote set-url origin $GIT_ASTXML2WIKI_ORIGIN

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

git fetch origin master
git checkout master
git reset --hard master
git pull

CLEAN_DIR=0

if [ -d ./asterisk ]; then
    pushd asterisk
    if output=$(git status --untracked-files=no --porcelain) && [ -z "$output" ]; then
        echo "asterisk clean and usable"
    else
        CLEAN_DIR=1
    fi
    popd
fi

if [ -d ./asterisk ] && [ $CLEAN_DIR -eq 1 ]; then
    echo "Cleaning asterisk"
    rm -fr asterisk
fi

if [ ! -d ./asterisk ]; then
    git clone $GIT_ASTERISK_ORIGIN asterisk
fi

pushd asterisk

git remote set-url origin $GIT_ASTERISK_ORIGIN

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

git fetch origin $BRANCH
git checkout $BRANCH
git reset --hard $BRANCH
git pull

popd

popd

echo "*** Documentation clones ready ***"
