#!/usr/bin/env bash

#
# Changes the global test-config.yaml to convert things to realtime when possible
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

DB_USER=$1
DB_HOST=$2
DB_NAME=$3
DB_DSN=$4

if [ -z "$DB_USER" ]; then
    echo "The database user must be the first argument."
    exit 1
fi

if [ -z "$DB_HOST" ]; then
    echo "The database host must be the second argument."
    exit 1
fi

if [ -z "$DB_NAME" ]; then
    echo "The database name must be the third argument."
    exit 1
fi

if [ -z "$DB_DSN" ]; then
    echo "The database DSN must be the fourth argument."
    exit 1
fi

pushd testsuite

# We replace the test-config.yaml with one that enables realtime auto-conversion
rm -rf test-config.yaml

cat > test-config.yaml << EOF
global-settings:
    test-configuration: config-realtime

    condition-definitions:
            -
                name: 'threads'
                pre:
                    typename: 'thread_test_condition.ThreadPreTestCondition'
                post:
                    typename: 'thread_test_condition.ThreadPostTestCondition'
                    related-type: 'thread_test_condition.ThreadPreTestCondition'
            -
                name: 'sip-dialogs'
                pre:
                    typename: 'sip_dialog_test_condition.SipDialogPreTestCondition'
                post:
                    typename: 'sip_dialog_test_condition.SipDialogPostTestCondition'
            -
                name: 'locks'
                pre:
                    typename: 'lock_test_condition.LockTestCondition'
                post:
                    typename: 'lock_test_condition.LockTestCondition'
            -
                name: 'file-descriptors'
                pre:
                    typename: 'fd_test_condition.FdPreTestCondition'
                post:
                    typename: 'fd_test_condition.FdPostTestCondition'
                    related-type: 'fd_test_condition.FdPreTestCondition'
            -
                name: 'channels'
                pre:
                    typename: 'channel_test_condition.ChannelTestCondition'
                post:
                    typename: 'channel_test_condition.ChannelTestCondition'
            -
                name: 'sip-channels'
                pre:
                    typename: 'sip_channel_test_condition.SipChannelTestCondition'
                post:
                    typename: 'sip_channel_test_condition.SipChannelTestCondition'
            -
                name: 'memory'
                pre:
                    typename: 'memory_test_condition.MemoryPreTestCondition'
                post:
                    typename: 'memory_test_condition.MemoryPostTestCondition'
                    related-type: 'memory_test_condition.MemoryPreTestCondition'

config-realtime:
    test-modules:
        modules:
            -
                typename: realtime_converter.RealtimeConverter
                config-section: realtime-config

    realtime-config:
        username: '${DB_USER}'
        host: '${DB_HOST}'
        db: '${DB_NAME}'
        dsn: '${DB_DSN}'
EOF

popd

echo "*** Asterisk Test Suite Ready With Realtime Support ***"

