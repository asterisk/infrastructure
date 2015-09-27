#!/usr/bin/env bash

#
# Executes Python unit tests within the Asterisk Test Suite
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

CI_USER=jenkins
CI_GROUP=users

echo "*** Running testsuite unit tests ***"
pushd testsuite

# Since the testsuite doesn't have an actual Python library of unit
# tests - which would be nice - for now, just execute each individually
TEST_NAMES=(buildoptions channel_test_condition sippversion version)
for test in ${TEST_NAMES[@]}; do
	echo " ==> Executing ${test}"
	python ./lib/python/asterisk/${test}.py
done

popd

# Drop the permissions down on the testsuite and /tmp directory
# for other scripts
chown -R ${CI_USER}:${CI_GROUP} testsuite
