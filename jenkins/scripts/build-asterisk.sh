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
BUILDJOBS=$[`nproc` + 1]

echo "*** $(date): Building Asterisk ${BRANCH} with $@ using ${BUILDJOBS} jobs"

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

export PATH="/usr/lib/ccache:$PATH:/usr/local/bin:/usr/sbin:/usr/local/sbin"
echo "*** $(date) PATH has been set to: ${PATH}"

# This probably should be set up in the machine configuration
ulimit -n 32767

echo ${PROJECT}
pushd ${PROJECT}

# If the configuration sample exists then all alembic samples should exist
if [ -f contrib/ast-db-manage/config.ini.sample ]; then
	if [ -f /usr/bin/alembic ]; then
		echo "*** $(date): Testing alembic branches"

		pushd contrib/ast-db-manage
		# Ensure no old .pyc files remain around to skew results
		rm -rf config/*.pyc cdr/*.pyc voicemail/*.pyc
		BRANCHES=$(alembic -c config.ini.sample branches)
		if [ -n "$BRANCHES" ]; then
			>&2 echo "Alembic branches exist for configuration - details follow ***"
			>&2 echo $BRANCHES
			exit 1
		fi
		BRANCHES=$(alembic -c cdr.ini.sample branches)
		if [ -n "$BRANCHES" ]; then
			>&2 echo "Alembic branches exist for CDR - details follow ***"
			>&2 echo $BRANCHES
			exit 1
		fi
		BRANCHES=$(alembic -c voicemail.ini.sample branches)
		if [ -n "$BRANCHES" ]; then
			>&2 echo "Alembic branches exist for voicemail - details follow ***"
			>&2 echo $BRANCHES
			exit 1
		fi
		popd
	else
		>&2 echo "*** $(date) Alembic is unavailable - unable to perform branch checking"
	fi
fi

# Test both 'uninstall' and 'uninstall-all', since they have different logic paths
echo "*** $(date) ${MAKE} uninstall"
${MAKE} uninstall
echo "*** $(date) ${MAKE} uninstall-all"
${MAKE} uninstall-all
echo "*** $(date) ${MAKE} distclean"
${MAKE} distclean

mkdir -p /srv/cache/externals || :
mkdir -p /srv/cache/sounds || :


# Run configure.  Note that if needed, this portion can
# be expanded to provide more configure flags in the future
COMMON_CONFIGURE_ARGS="--sysconfdir=/etc --with-pjproject-bundled \
	--with-sounds-cache=/srv/cache/sounds \
	--with-externals-cache=/srv/cache/externals \
"
if $DEBUG ; then
	COMMON_CONFIGURE_ARGS="$COMMON_CONFIGURE_ARGS --enable-dev-mode --enable-coverage"
fi
echo "*** $(date) ./configure ${COMMON_CONFIGURE_ARGS}"
./configure ${COMMON_CONFIGURE_ARGS}

echo "*** $(date) ${MAKE} menuselect.makeopts"
${MAKE} menuselect.makeopts

echo "*** $(date) Setting individual menuselect options"
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
menuselect/menuselect --disable res_mwi_external menuselect.makeopts

if grep -q MENUSELECT_UTILS menuselect.makeopts ; then
	menuselect/menuselect --enable-category MENUSELECT_UTILS menuselect.makeopts
fi

# Disable all the downloadable modules.  If needed, they'll be enabled later.
for d in codec_opus codec_silk codec_siren7 codec_siren14 codec_g729a res_digium_phone ; do
	menuselect/menuselect --disable $d menuselect.makeopts
done

for ((i=0; i<${#ARR_ENABLES[@]}; ++i)); do
	menuselect/menuselect --enable "${ARR_ENABLES[$i]}" menuselect.makeopts
done
for ((i=0; i<${#ARR_DISABLES[@]}; ++i)); do
	menuselect/menuselect --disable "${ARR_DISABLES[$i]}" menuselect.makeopts
done

# Disable chan_vpb, because it is "not so good"
menuselect/menuselect --disable chan_vpb menuselect.makeopts

echo "*** $(date) ${MAKE} ASTCFLAGS=${ASTCFLAGS} -j${BUILDJOBS}"
${MAKE} ASTCFLAGS=${ASTCFLAGS} -j${BUILDJOBS}
if [ $? -ne 0 ]; then
	# This additional run without -j may work but if it still fails the
	# error will appear at the very end of the log.
	>&2 echo "*** $(date) Failed.  Remaking: ${MAKE} ASTCFLAGS=${ASTCFLAGS}"
	${MAKE} NOISY_BUILD=yes ASTCFLAGS=${ASTCFLAGS}
	if [ $? -ne 0 ]; then
		>&2 echo "*** $(date) Failed to build Asterisk ***"
		exit 1
	fi
fi

if [ -f doc/core-en_US.xml ] ; then
	echo "*** $(date) ${MAKE} validate-docs"
	${MAKE} validate-docs
	if [ $? -ne 0 ]; then
		>&2 echo "*** $(date) Failed.  Remaking: $(date) ${MAKE} validate-docs"
		${MAKE} NOISY_BUILD=yes validate-docs
		if [ $? -ne 0 ]; then
			>&2 echo "*** $(date) Failed to validate documentation ***"
			exit 1
		fi
	fi
fi

echo "*** $(date) WGET_EXTRA_ARGS=--quiet ${MAKE} install"
WGET_EXTRA_ARGS=--quiet ${MAKE} install
if [ $? -ne 0 ]; then
	>&2 echo "*** $(date) Failed.  Remaking: WGET_EXTRA_ARGS=--quiet ${MAKE} install"
	WGET_EXTRA_ARGS=--quiet ${MAKE} NOISY_BUILD=yes install
	if [ $? -ne 0 ]; then
		>&2 echo "*** Failed to install Asterisk ***"
		exit 1
	fi
fi

echo "*** $(date) ${MAKE} samples"
${MAKE} samples
if [ $? -ne 0 ]; then
	>&2 echo "*** Failed to install Asterisk samples ***"
	exit 1
fi

popd

if [ ! -f /usr/sbin/asterisk ]; then
	>&2 echo "*** Failed to install Asterisk ***"
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

ldconfig

echo "*** Asterisk built and installed successfully ***"

