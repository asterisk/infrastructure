#!/bin/bash

# Copyright 2015 (C), Digium, Inc.
# Matt Jordan <mjordan@digium.com>
#
# Portions of this script were derived from similar scripts produced by
# the OpenStack Foundation:
# Copyright 2013 OpenStack Foundation
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

if ! which pep8; then
	echo "*** pep8 not installed ***"
	exit 1
fi

if [ -z ${PYTHON_DIR+x} ]; then
	export PYTHON_DIR=.
fi

pep8 $PYTHON_DIR | tee ${PEP8_OUTPUT}

rc=$?

exit $rc
