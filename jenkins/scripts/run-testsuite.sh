#!/usr/bin/env bash

#
# Executes the Asterisk Test Suite
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

TESTSUITE_ARGS=$1

CI_USER=jenkins
CI_GROUP=users

if [ ! -f /usr/sbin/asterisk ]; then
	echo "ERROR: Asterisk not installed."
	exit 1
fi

if [ "$(id -u)" != "0" ]; then
	echo "ERROR: run-testsuite must be run as 'root'."
	exit 1
fi

ASTERISK_VER=$(asterisk -V)

echo "*** Running tests against $ASTERISK_VER ***"
pushd testsuite
./runtests.py $TESTSUITE_ARGS

# Archive the logs
if [ -d ./logs ]; then
	tar -zcvf logs.tar.gz asterisk-test-suite-report.xml logs
fi

if [ -f ./logs/refleaks-summary.txt ]; then
	find logs/ \( -name 'refs.txt' -o -name 'refleaks-summary.txt' \) -print0 | tar -czvf refleaks.tar.gz --null -T -
fi

popd

# Drop the permissions down on the testsuite and /tmp directory
# for other scripts
chown -R ${CI_USER}:${CI_GROUP} testsuite
chown -R ${CI_USER}:${CI_GROUP} /tmp/asterisk-testsuite || true

