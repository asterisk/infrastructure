#!/usr/bin/env bash

#
# Tests Codec ABI compatibility
#
# Copyright 2016 (C), Digium, Inc.
# Mark Michelson <mmichelson@digium.com>
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

BRANCH=$1
CODEC=codec_silk

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

install_codecs() {
	echo "*** Installing codecs ***"
	ARCH=$(uname -m)

	# Copied from codec_silk's build tools
	case "$ARCH" in
	    i386 | i686)
	    BITS="32"
	    ARCH="X86"
	    ;;
	    x86_64 | amd64)
	    BITS="64"
	    ARCH="X86"
	    ;;
	    X86)
	    # nothing to do, the user has specified the ARCH and BITS values
	    ;;
	    sun4c | sun4d | sun4m | sun4u)
	    #sun4u can be compiled 64 bit.  Run with ./makeall 64
	    ARCH="SPARC"
	    if [ ! -z "$1" -a "$1" == "64" ]; then
	        echo "This will only work if you compiled libcp as 64 bit."
	        BITS="64"
	    else
	        BITS="32"
	    fi
	    ;;
	    *)
	    echo "Unknown architecture found (${ARCH}); aborting."
	    exit 0
	    ;;
	esac

	# Lowercase the 'X'
	ARCH=${ARCH,X}

	BASE_URL=http://downloads.digium.com/pub/telephony/${CODEC}/asterisk-${BRANCH}.0/${ARCH}-${BITS}

	# Determine the latest version from the manifest file
	VERSION=$(xmllint --xpath 'string(/package/@version)' <(wget --quiet -O - ${BASE_URL}/manifest.xml))
	TARGET_DIR=${CODEC}-${VERSION}-${ARCH}_${BITS}

	# Any extra flags needed for wget?
	wget ${BASE_URL}/${TARGET_DIR}.tar.gz
	tar xvzf ${TARGET_DIR}.tar.gz
	cp ${TARGET_DIR}/${CODEC}.so /usr/lib/asterisk/modules

	# We can go ahead and clean up a bit here.
	rm -rf ${TARGET_DIR}
	rm -f ${TARGET_DIR}.tar.gz
}

remove_codecs() {
	rm -f /usr/lib/asterisk/modules/${CODEC}.so
}

export PATH="$PATH:/usr/lib/ccache:/usr/local/bin:/usr/sbin:/usr/local/sbin"
echo "PATH has been set to: ${PATH}"

if ! which asterisk; then
	echo "Asterisk not installed"
	exit 1
fi

if ! which tar; then
	echo "tar not installed"
	exit 1
fi

if ! which wget; then
	echo "wget not installed"
	exit 1
fi

killall_asterisk
install_codecs
start_asterisk
if [ -f core* ] ; then
	echo "*** Found a core file after starting Asterisk with ${CODEC} loaded***"
	gdb asterisk core* -ex "bt full" -ex "thread apply all bt" --batch
	remove_codecs
	exit 1
fi
stop_asterisk
remove_codecs
