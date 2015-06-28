#!/usr/bin/env bash

#
# Retrieves the Asterisk Test Suite for actual runs against Asterisk
#
# Copyright 2015 (C), Digium, Inc.
# Matt Jordan <mjordan@digium.com>
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

GERRIT_SITE=$1

if [ -z "$GERRIT_SITE" ]; then
    echo "The gerrit site name (eg 'https://gerrit.asterisk.org') must be the first argument."
    exit 1
fi

if [ ! -d ./testsuite ]; then
    git clone $GERRIT_SITE/testsuite testsuite
fi

pushd testsuite

git remote set-url origin $GERRIT_SITE/testsuite

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

popd

echo "*** Asterisk Test Suite Ready ***"

