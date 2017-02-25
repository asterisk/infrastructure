@NonCPS
def parse(buildopts) {
	def bo = " " + buildopts + " "
	def debug = (bo =~ /.*\s+-v\s+.*/).matches()
	def enables = (bo =~ /\s+-e\s+([^ ]+)/).collect { it[1] }
	def disables = (bo =~ /\s+-d\s+([^ ]+)/).collect { it[1] }
	def DESTDIR = (bo =~ /\s+DESTDIR\s+([^ ]+)/).collect { it[1] }
	
	def es = ""
	for (e in enables) {
		es += " --enable ${e}"
	}

	def ds = ""
	for (d in disables) {
		ds += " --disable ${d}"
	}

	def dest = ""
	for (d in DESTDIR) {
		dest += "${d}"
	}

	return [ debug: debug, enables: es, disables: ds, destdir: dest ]
}

def call(branch, buildopts) {

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

	sh """\
		sudo mkdir -p /srv/cache/externals /srv/cache/sounds || :
		sudo chown -R jenkins:users /srv/cache
		sudo chown -R jenkins:users asterisk
	""".stripIndent();

	dir("asterisk") {
		stage("check-alembic") {
			sh '''\
				ALEMBIC=`which alembic 2>/dev/null || :`
				if [ x"$ALEMBIC" = x ] ; then
					echo "Alembic not installed"
					exit 1
				fi
				cd contrib/ast-db-manage
				find -name *.pyc -delete
				out=`alembic -c config.ini.sample branches`
				if [ x$out != x ] ; then
					>&2 echo "Alembic branches were found for config"
					>&2 echo $out
					exit 1
				fi
			
				out=`alembic -c cdr.ini.sample branches`
				if [ x$out != x ] ; then
					>&2 echo "Alembic branches were found for cdr"
					>&2 echo $out
					exit 1
				fi

				out=`alembic -c voicemail.ini.sample branches`
				if [ x$out != x ] ; then
					>&2 echo "Alembic branches were found for voicemail"
					>&2 echo $out
					exit 1
				fi
			'''.stripIndent()
		}

		stage("distclean configure menuselect") {
			sh """\
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

				local_cat_disables=""
				local_disables+=" res_mwi_external codec_opus codec_silk codec_g729a codec_siren7"
				local_disables+=" codec_siren14 res_digium_phone chan_vpb"

				es=""
				for ecat in \$local_cat_enables ; do
					es+=" --enable-category \${ecat}"
				done

				for e in \${local_enables} ; do
					es+=" --enable \${e}"
				done
			
				if [ \${#es} -ne 0 ] ; then
					menuselect/menuselect \${es} menuselect.makeopts
				fi

				ds=""
				for dcat in \${local_cat_disables} ; do
					ds+=" --disable-category \${dcat}"
				done
				for d in \${local_disables} ; do
					ds+=" --disable \${d}"
				done

				if [ \${#ds} -ne 0 ] ; then
					menuselect/menuselect \${ds} menuselect.makeopts
				fi
	
				if [ ${parameters.enables.length()} -ne 0 ] ; then
					menuselect/menuselect ${parameters.enables} menuselect.makeopts
				fi

				if [ ${parameters.disables.length()} -ne 0 ] ; then
					menuselect/menuselect ${parameters.disables} menuselect.makeopts
				fi
			""".stripIndent()

			archiveArtifacts allowEmptyArchive: true, artifacts: 'config.log config_summary.log menuselect.makeopts menuselect.makedeps makeopts', defaultExcludes: false, fingerprint: true
		}

		stage("make") {
			sh "${make} -j${jobs} || ${make} -j${jobs} NOISY_BUILD=yes"
		}

		stage("validate-docs") {
			sh """\
				if [ -f "doc/core-en_US.xml" ] ; then
					${make} validate-docs || ${make} NOISY_BUILD=yes validate-docs
				fi
			""".stripIndent()
		}

		stage("install") {
			def DESTDIR=""
			if (parameters.destdir.length()) {
				DESTDIR="DESTDIR=${parameters.destdir}"
			}
			sh """\
				export WGET_EXTRA_ARGS="--quiet"
				sudo ${make}  ${DESTDIR} install || sudo ${make} NOISY_BUILD=yes ${DESTDIR} install 
				sudo ${make}  ${DESTDIR} samples
				git clean -fdx >/dev/null 2>&1
				set +e
				sudo chown -R jenkins:users ${parameters.destdir}/var/lib/asterisk
				sudo chown -R jenkins:users ${parameters.destdir}/var/spool/asterisk
				sudo chown -R jenkins:users ${parameters.destdir}/var/log/asterisk
				sudo chown -R jenkins:users ${parameters.destdir}/var/run/asterisk
				sudo chown -R jenkins:users ${parameters.destdir}/etc/asterisk
				sudo ldconfig
				""".stripIndent()
		}
	}
}
