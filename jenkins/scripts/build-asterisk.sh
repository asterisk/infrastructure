#!/usr/bin/env bash

#
# Builds the checked out version of Asterisk
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

show_help()
{
	echo "build-asterisk: Build Asterisk"
	echo "Options:"
	echo "  -d: Add a DISABLE option to pass to menuselect"
	echo "  -e: Add an ENABLE option to pass to menuselect"
	echo "  -v: Enable dev-mode and other related configure options"
}

DEBUG=false
ENABLES=""
DISABLES=""

CI_USER=jenkins
CI_GROUP=users

PWD=`pwd`
BUILDFLAGS="NOISY_BUILD=yes"

if [ -n "$ZUUL_PROJECT" ]; then
	PROJECT=$ZUUL_PROJECT
else
	PROJECT=asterisk
fi

OPTIND=1
while getopts "h?d:e:v" opt; do
	case "$opt" in
	h|\?)
		show_help
		exit 0
		;;
	d)
		DISABLES="$DISABLES $OPTARG"
		;;
	e)
		ENABLES="$ENABLES $OPTARG"
		;;
	v)
		DEBUG=true
		;;
	esac
done
shift $((OPTIND-1)) # Shift off the options and optional --

IFS=' ' read -a ARR_ENABLES <<< "${ENABLES}"
IFS=' ' read -a ARR_DISABLES <<< "$DISABLES"

if which gmake ; then
	MAKE=gmake
else
	MAKE=make
fi

if [ "`uname`" = "FreeBSD" ] && [ "`uname -m`" = "i386" ] ; then
	ASTCFLAGS=-march=i686
fi

export PATH="$PATH:/usr/lib/ccache:/usr/local/bin"
echo "PATH has been set to: ${PATH}"

# This probably should be set up in the machine configuration
ulimit -n 32767

echo ${PROJECT}
pushd ${PROJECT}

# Test distclean
${MAKE} ${BUILDFLAGS} distclean

# Run configure.  Note that if needed, this portion can
# be expanded to provide more configure flags in the future
COMMON_CONFIGURE_ARGS="--sysconfdir=/etc"
if $DEBUG ; then
	COMMON_CONFIGURE_ARGS="$COMMON_CONFIGURE_ARGS --enable-dev-mode --enable-coverage"
fi
./configure ${COMMON_CONFIGURE_ARGS}

# Test both 'uninstall' and 'uninstall-all', since they have different logic paths
# in the Asterisk Makefile.
${MAKE} ${BUILDFLAGS} uninstall
${MAKE} ${BUILDFLAGS} uninstall-all
${MAKE} ${BUILDFLAGS} menuselect.makeopts

if $DEBUG ; then
	menuselect/menuselect --enable DONT_OPTIMIZE menuselect.makeopts
	menuselect/menuselect --enable MALLOC_DEBUG menuselect.makeopts
	menuselect/menuselect --enable BETTER_BACKTRACES menuselect.makeopts
	menuselect/menuselect --enable TEST_FRAMEWORK menuselect.makeopts
	menuselect/menuselect --enable DO_CRASH menuselect.makeopts
	menuselect/menuselect --enable-category MENUSELECT_TESTS menuselect.makeopts
fi
if [ -d bridges ] ; then
	menuselect/menuselect --enable-category MENUSELECT_BRIDGES menuselect.makeopts
fi
if [ -d cel ] ; then
	menuselect/menuselect --enable-category MENUSELECT_CEL menuselect.makeopts
fi
menuselect/menuselect --enable-category MENUSELECT_CDR menuselect.makeopts
menuselect/menuselect --enable-category MENUSELECT_CHANNELS menuselect.makeopts
menuselect/menuselect --enable-category MENUSELECT_CODECS menuselect.makeopts
menuselect/menuselect --enable-category MENUSELECT_FORMATS menuselect.makeopts
menuselect/menuselect --enable-category MENUSELECT_FUNCS menuselect.makeopts
menuselect/menuselect --enable-category MENUSELECT_PBX menuselect.makeopts
menuselect/menuselect --enable-category MENUSELECT_RES menuselect.makeopts
if grep -q MENUSELECT_UTILS menuselect.makeopts ; then
	menuselect/menuselect --enable-category MENUSELECT_UTILS menuselect.makeopts
fi

for ((i=0; i<${#ARR_ENABLES[@]}; ++i)); do
	menuselect/menuselect --enable "${ARR_ENABLES[$i]}" menuselect.makeopts
done
for ((i=0; i<${#ARR_DISABLES[@]}; ++i)); do
	menuselect/menuselect --disable "${ARR_DISABLES[$i]}" menuselect.makeopts
done

${MAKE} ${BUILDFLAGS} ASTCFLAGS=${ASTCFLAGS}

if [ -f doc/core-en_US.xml ] ; then
	echo "*** Validating XML documentation ***"
	${MAKE} ${BUILDFLAGS} validate-docs
fi

echo "*** Installing Asterisk and Sample Configuration ***"
WGET_EXTRA_ARGS=--quiet ${MAKE} ${BUILDFLAGS} install
${MAKE} ${BUILDFLAGS} samples

popd

if ! which asterisk; then
	echo "*** Failed to install Asterisk ***"
	exit 1
fi

# Grant ownership and permissions so we can manipulate things easier
chown -R ${CI_USER}:${CI_GROUP} /usr/lib/asterisk
chown -R ${CI_USER}:${CI_GROUP} /var/lib/asterisk
chown -R ${CI_USER}:${CI_GROUP} /var/spool/asterisk
chown -R ${CI_USER}:${CI_GROUP} /var/log/asterisk
chown -R ${CI_USER}:${CI_GROUP} /var/run/asterisk
chown -R ${CI_USER}:${CI_GROUP} /etc/asterisk
chown -R ${CI_USER}:${CI_GROUP} /usr/sbin/asterisk

