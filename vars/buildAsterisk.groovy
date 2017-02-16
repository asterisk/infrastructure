

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
		ds += " --disable ${d[1]}"
	}

	return [ debug: debug, enables: es, disables: ds ]
}

def call(branch, buildopts) {

	parameters = parse(buildopts)

	def jobs = sh script: 'nproc', returnStdout: true
	jobs = jobs.trim()
	println "Processors: ${jobs}"
	jobs++
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

	env.PATH = "/usr/lib/ccache:${env.PATH}:/usr/local/bin:/usr/sbin:/usr/local/sbin"

	sh "sudo mkdir -p /srv/cache/externals /srv/cache/sounds || :"
	sh "sudo chown -R jenkins:users /srv/cache"
	sh "sudo chown -R jenkins:users asterisk"

	dir("asterisk") {
		stage("check-alembic") {
			def alembic = ""
			try {
				alembic = sh script: "which alembic 2>/dev/null", returnStdout: true
			} catch(e) {
				println "Alembic not installed"
			}
			alembic = alembic.trim()

			if (alembic.length() && fileExists("contrib/ast-db-manage/config.ini.sample")) {

				dir("contrib/ast-db-manage") {
					sh "rm -rf config/*.pyc cdr/*.pyc voicemail/*.pyc || :"
					def out
					out = sh script: "alembic -c config.ini.sample branches", returnStdout: true
					if (out && out.length()) {
						println "Alembic branches were found for config"
						error out
					}
					out = sh script: "alembic -c cdr.ini.sample branches", returnStdout: true
					if (out && out.length()) {
						println "Alembic branches were found for cdr"
						error out
					}
					out = sh script: "alembic -c voicemail.ini.sample branches", returnStdout: true
					if (out && out.length()) {
						println "Alembic branches were found for voicemail"
						error out
					}
				}
			}
		}

		stage("distclean configure menuselect") {
			try {
				sh "${make} distclean || :"
			} catch (e) {
				println "Distclean failed but continuing"
			}
			def common_config_args = "--sysconfdir=/etc --with-pjproject-bundled \
					--with-sounds-cache=/srv/cache/sounds --with-externals-cache=/srv/cache/externals"
			if (parameters.debug) {
				common_config_args += " --enable-dev-mode --enable-coverage"
			}
			sh "./configure ${common_config_args}"
			sh "sudo ${make} uninstall"
			sh "sudo ${make} uninstall-all"

			sh "${make} menuselect.makeopts"

			def local_cat_enables = [ "MENUSELECT_BRIDGES", "MENUSELECT_CEL", "MENUSELECT_CDR", 
				"MENUSELECT_CHANNELS", "MENUSELECT_CODECS", "MENUSELECT_FORMATS", "MENUSELECT_FUNCS",
				"MENUSELECT_PBX", "MENUSELECT_RES", "MENUSELECT_UTILS" ]

			def local_cat_disables = []
			
			def local_enables = []
			if (parameters.debug) {
				local_enables = [ "DONT_OPTIMIZE", "MALLOC_DEBUG", "BETTER_BACKTRACES", "TEST_FRAMEWORK",
					"DO_CRASH" ]
				local_cat_enables << "MENUSELECT_TESTS"
			}

			def local_disables = [ "res_mwi_external", "codec_opus", "codec_silk", "codec_g729a",
				"codec_siren7", "codec_siren14", "res_digium_phone", "chan_vpb" ]
			
			def es = ""
			for (ecat in local_cat_enables) {
				es += " --enable-category ${ecat}"
			}
			for (e in local_enables) {
				es += " --enable ${e}"
			}
			if (es.length()) {
				sh "menuselect/menuselect ${es} menuselect.makeopts"
			}

			def ds = ""
			for (dcat in local_cat_disables) {
				ds += " --disable-category ${dcat}"
			}
			for (d in local_disables) {
				ds += " --disable ${d}"
			}
			if (ds.length()) {
				sh "menuselect/menuselect ${ds} menuselect.makeopts"
			}
	
			if (parameters.enables.length()) {
				sh "menuselect/menuselect ${parameters.enables} menuselect.makeopts"
			}

			if (parameters.disables.length()) {
				sh "menuselect/menuselect ${parameters.disables} menuselect.makeopts"
			}
		}

		stage("make") {
			try {
				sh "${make} -j${jobs}"
			} catch (e) {
				sh "${make} -j${jobs} NOISY_BUILD=yes"
			}
		}

		stage("validate-docs") {
			if (fileExists("doc/core-en_US.xml")) {
				try {
					sh "${make} validate-docs"
				} catch (e) {
					sh "${make} NOISY_BUILD=yes validate-docs"
				}
			}
		}

		stage("install") {
			env.WGET_EXTRA_ARGS = "--quiet"
			try {
				sh "sudo ${make} install"
			} catch (e) {
				sh "sudo ${make} NOISY_BUILD=yes install"
			}
			sh "sudo ${make} samples"
			sh '''
				set +e
				sudo chown -R jenkins:users /usr/lib/asterisk
				sudo chown -R jenkins:users /var/lib/asterisk
				sudo chown -R jenkins:users /var/spool/asterisk
				sudo chown -R jenkins:users /var/log/asterisk
				sudo chown -R jenkins:users /var/run/asterisk
				sudo chown -R jenkins:users /etc/asterisk
				sudo chown -R jenkins:users /usr/sbin/asterisk
				sudo ldconfig
				'''.stripIndent()
		}
	}
}