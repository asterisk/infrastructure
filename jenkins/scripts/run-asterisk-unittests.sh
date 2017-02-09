#!/usr/bin/env bash

#
# Runs the Asterisk unit tests
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

PWD=`pwd`

if [ -n "$ZUUL_PROJECT" ]; then
	PROJECT=$ZUUL_PROJECT
else
	PROJECT=asterisk
fi

TEST_RESULTS_DIR=${PWD}/$PROJECT/test-reports

start_asterisk() {
	echo "*** Starting Asterisk ***"

	if [ -d /Library/LaunchDaemons ] ; then
		# Mac OSX
		launchctl load -w /Library/LaunchDaemons/org.asterisk.asterisk.plist
	else
		asterisk -gn
	fi

	sleep 1
	asterisk -rx "core waitfullybooted"
	sleep 3
}

stop_asterisk() {
	echo "*** Stopping Asterisk ***"

	if [ -d /Library/LaunchDaemons ] ; then
		# Mac OSX
		launchctl unload -w /Library/LaunchDaemons/org.asterisk.asterisk.plist || :
	else
		asterisk -rx "core stop gracefully"
	fi
	sleep 5

	(killall -9 asterisk || :) > /dev/null 2>&1
}

killall_asterisk() {
	echo "*** Killing all running Asterisk processes ***"
	if [ -d /Library/LaunchDaemons ] ; then
		launchctl unload -w /Library/LaunchDaemons/org.asterisk.asterisk.plist || :
	fi
	(killall -9 asterisk || :) > /dev/null 2>&1
}

setup_configs() {
	# Archive any existing files that may conflict
	if [ -f /etc/asterisk/extensions.conf ]; then
		mv -f /etc/asterisk/extensions.conf /etc/asterisk/extensions.conf.backup
	fi
	if [ -f /etc/asterisk/extensions.ael ]; then
		mv -f /etc/asterisk/extensions.ael /etc/asterisk/extensions.ael.backup
	fi
	if [ -f /etc/asterisk/extensions.lua ]; then
		mv -f /etc/asterisk/extensions.lua /etc/asterisk/extensions.lua.backup
	fi
	if [ -f /etc/asterisk/manager.conf ]; then
		mv -f /etc/asterisk/manager.conf /etc/asterisk/manager.conf.backup
	fi
	if [ -f /etc/asterisk/logger.conf ]; then
		mv -f /etc/asterisk/logger.conf /etc/asterisk/logger.conf.backup
	fi

	cat > /etc/asterisk/manager.conf << EOF
[general]
enabled=yes
bindaddr=127.0.0.1
port=5038

[test]
secret=test
read = system,call,log,verbose,agent,user,config,dtmf,reporting,cdr,dialplan
write = system,call,agent,user,config,command,reporting,originate
EOF

	cat > /etc/asterisk/logger.conf << EOF
[logfiles]
full => notice,warning,error,debug,verbose
EOF

	cat > /etc/asterisk/http.conf << EOF
[general]
enabled=yes
bindaddr=127.0.0.1
port=8088
EOF

	cat > /etc/asterisk/extensions.conf << EOF
[default]
EOF

}

restore_configs() {

	# Revert back to samples files
	if [ -f /etc/asterisk/extensions.conf.backup ]; then
		mv -f /etc/asterisk/extensions.conf.backup /etc/asterisk/extensions.conf
	fi
	if [ -f /etc/asterisk/extensions.ael.backup ]; then
		mv -f /etc/asterisk/extensions.ael.backup /etc/asterisk/extensions.ael
	fi
	if [ -f /etc/asterisk/extensions.lua.backup ]; then
		mv -f /etc/asterisk/extensions.lua.backup /etc/asterisk/extensions.lua
	fi
	if [ -f /etc/asterisk/manager.conf.backup ]; then
		mv -f /etc/asterisk/manager.conf.backup /etc/asterisk/manager.conf
	fi
	if [ -f /etc/asterisk/logger.conf.backup ]; then
		mv -f /etc/asterisk/logger.conf.backup /etc/asterisk/logger.conf
	fi

}

run_unit_tests() {

	start_asterisk

	echo "*** Executing Unit Tests (Results: ${TEST_RESULTS_DIR}/unit-test-results.xml) ***"
	asterisk -rx "test execute all"
	asterisk -rx "test generate results xml ${TEST_RESULTS_DIR}/unit-test-results.xml"

	if [ -f core* ] ; then
		echo "*** Found a core file after running unit tests ***"
		gdb asterisk core* -ex "bt full" -ex "thread apply all bt" --batch
		exit 1
	fi

	stop_asterisk

}

export PATH="$PATH:/usr/lib/ccache:/usr/local/bin:/usr/sbin:/usr/local/sbin"
echo "PATH has been set to: ${PATH}"

if ! which asterisk; then
	echo "Asterisk not installed"
	exit 1
fi

pushd $PROJECT

if [ ! -d ${TEST_RESULTS_DIR} ]; then
	mkdir ${TEST_RESULTS_DIR}
fi

killall_asterisk
setup_configs
run_unit_tests
restore_configs

popd

exit 0
