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

GIT_ORIGIN=$1

CI_USER=jenkins
CI_GROUP=users

if [ -z "$GIT_ORIGIN" ]; then
    echo "The git origin must be the first argument."
    exit 1
fi

if [ ! -d ./astxml2wiki ]; then
    git clone $GIT_ORIGIN astxml2wiki
fi

pushd astxml2wiki

git remote set-url origin $GIT_ORIGIN

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

# This purposely keeps us in the astxml2wiki directory for the Asterisk step
popd

echo "*** astxml2wiki Ready ***"

