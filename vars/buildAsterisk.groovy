@NonCPS
def parse(buildopts) {
	def bo = " " + buildopts + " "
	def debug = (bo =~ /.*\s+-v\s+.*/).matches()
	def enables = (bo =~ /\s+-e\s+([^ ]+)/).collect { it[1] }
	def disables = (bo =~ /\s+-d\s+([^ ]+)/).collect { it[1] }
	
	def es = ""
	for (e in enables) {
		es += " --enable ${e}"
	}

	def ds = ""
	for (d in disables) {
		ds += " --disable ${d}"
	}

	return [ debug: debug, enables: es, disables: ds ]
}

def call(branch, buildopts) {
	call(branch, buildopts, "")
}

def call(branch, buildopts, destdir) {

	parameters = parse(buildopts)

	def jobs = sh script: 'nproc', returnStdout: true
	jobs = jobs.trim()
	println "Processors: ${jobs}"
	println "Jobservers: ${jobs}"

	def make
	try {
		make = sh script: "which gmake 2>/dev/null", returnStdout: true
	} catch (e) {
		try {
			make = sh script: "which make 2>/dev/null", returnStdout: true
		} catch (e2) {
			error "No 'make' found"
		}
	}
	make = make.trim()

	shell """\
		mkdir -p /srv/cache/externals /srv/cache/sounds || :
		sudo chown -R jenkins:users /srv/cache
		sudo chown -R jenkins:users asterisk
	"""

	dir("asterisk") {
		stage("check-alembic") {
			shell '''\
				ALEMBIC=`which alembic 2>/dev/null || :`
				if [ x"$ALEMBIC" = x ] ; then
					echo "Alembic not installed"
					exit 1
				fi
				cd contrib/ast-db-manage
				find -name *.pyc -delete
				out=`alembic -c config.ini.sample branches`
				if [ "x$out" != "x" ] ; then
					>&2 echo "Alembic branches were found for config"
					>&2 echo $out
					exit 1
				fi
			
				out=`alembic -c cdr.ini.sample branches`
				if [ "x$out" != "x" ] ; then
					>&2 echo "Alembic branches were found for cdr"
					>&2 echo $out
					exit 1
				fi

				out=`alembic -c voicemail.ini.sample branches`
				if [ "x$out" != "x" ] ; then
					>&2 echo "Alembic branches were found for voicemail"
					>&2 echo $out
					exit 1
				fi
			'''
		}

		stage("distclean configure menuselect") {
			shell """\
				common_config_args="--sysconfdir=/etc --with-pjproject-bundled"
				common_config_args+=" --with-sounds-cache=/srv/cache/sounds --with-externals-cache=/srv/cache/externals"

				if [ ${parameters.debug} = true ] ; then
					common_config_args+=" --enable-dev-mode"
				fi

				${make} distclean || :
				export WGET_EXTRA_ARGS="--quiet"
				./configure \${common_config_args} >config_summary.log
				sudo ${make} uninstall || :
				sudo ${make} uninstall-all || :
				${make} menuselect.makeopts

				local_cat_enables="MENUSELECT_BRIDGES MENUSELECT_CEL MENUSELECT_CDR" 
				local_cat_enables+=" MENUSELECT_CHANNELS MENUSELECT_CODECS MENUSELECT_FORMATS MENUSELECT_FUNCS"
				local_cat_enables+=" MENUSELECT_PBX MENUSELECT_RES MENUSELECT_UTILS"

				local_disables=""
				if [ ${parameters.debug} = true ] ; then
					local_enables="DONT_OPTIMIZE MALLOC_DEBUG BETTER_BACKTRACES TEST_FRAMEWORK DO_CRASH"
					local_disables="COMPILE_DOUBLE"
					local_cat_enables+=" MENUSELECT_TESTS"
				fi
				local_enables+=" CORE-SOUNDS-EN-GSM MOH-OPSOUND-GSM EXTRA-SOUNDS-EN-GSM"

				local_cat_disables=" MENUSELECT_CORE_SOUNDS MENUSELECT_MOH MENUSELECT_EXTRA_SOUNDS"
				local_disables+=" res_mwi_external codec_opus codec_silk codec_g729a codec_siren7"
				local_disables+=" codec_siren14 res_digium_phone chan_vpb"

				locals=""
				for ecat in \$local_cat_enables ; do
					locals+=" --enable-category \${ecat}"
				done

				for dcat in \${local_cat_disables} ; do
					locals+=" --disable-category \${dcat}"
				done

				for e in \${local_enables} ; do
					locals+=" --enable \${e}"
				done
			
				for d in \${local_disables} ; do
					locals+=" --disable \${d}"
				done

				if [ \${#locals} -ne 0 ] ; then
					menuselect/menuselect \${locals} menuselect.makeopts
				fi


				if [ ${parameters.enables.length()} -ne 0 ] ; then
					menuselect/menuselect ${parameters.enables} menuselect.makeopts
				fi

				if [ ${parameters.disables.length()} -ne 0 ] ; then
					menuselect/menuselect ${parameters.disables} menuselect.makeopts
				fi
			"""

			archiveArtifacts allowEmptyArchive: true, defaultExcludes: false, fingerprint: true,
				artifacts: 'config.log, config_summary.log, menuselect.makeopts, menuselect.makedeps, makeopts'
			shell "rm -rf config_summary.log || : "	
		}

		stage("make") {
			shell "${make} ari-stubs"
			shell "${make} -j${jobs} || ${make} -j${jobs} NOISY_BUILD=yes"
			shell 'test $(git status --porcelain | wc -l) -eq 0 || (git status --porcelain; false)'
		}

		stage("validate-docs") {
			shell """\
				if [ -f "doc/core-en_US.xml" ] ; then
					${make} validate-docs || ${make} NOISY_BUILD=yes validate-docs
				fi
			"""
		}

		stage("install") {
			sudo """\
				${make} uninstall-all
				${make} install || ${make} NOISY_BUILD=yes install 
				${make} samples
				set +e
				chown -R jenkins:users /var/lib/asterisk
				chown -R jenkins:users /var/spool/asterisk
				chown -R jenkins:users /var/log/asterisk
				chown -R jenkins:users /var/run/asterisk
				chown -R jenkins:users /etc/asterisk
				ldconfig
				"""
		}
	}
}
