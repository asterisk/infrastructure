def call() {
	stage("unittests") {

		sudo """\
			set +e
			if [ ! -x /usr/sbin/asterisk ] ; then
				echo "Asterisk is not installed"
				exit 1
			fi
			killall -9 asterisk 
			chown -R jenkins:users /etc/asterisk
			ln -sf /etc/asterisk etc-asterisk
		"""

		def configs = [
			"extensions.conf",
			"extensions.ael",
			"extensions.lua",
			"manager.conf",
			"logger.conf"
		]

		try {
			for (f in configs) {
				def path = "/etc/asterisk/${f}"
				shell "[ -f ${path} ] && mv -f ${path} ${path}.backup"
			}

			writeFile file: "etc-asterisk/manager.conf", text:'''\
			[general]
			enabled=yes
			bindaddr=127.0.0.1
			port=5038

			[test]
			secret=test
			read = system,call,log,verbose,agent,user,config,dtmf,reporting,cdr,dialplan
			write = system,call,agent,user,config,command,reporting,originate
			'''.stripIndent()

			writeFile file:"etc-asterisk/logger.conf", text: "full => notice,warning,error,debug,verbose"
			writeFile file:"etc-asterisk/http.conf", text: '''\
			[general]
			enabled=yes
			bindaddr=127.0.0.1
			port=8088
			'''.stripIndent()

			writeFile file: "etc-asterisk/extensions.conf", text: "[default]"

			sudo """\
				[ -d test-reports ] && sudo rm -rf test-reports
				mkdir test-reports
				asterisk -gn
				sleep 3
				asterisk -rx "core waitfullybooted"
				sleep 1
				asterisk -rx "test execute all"
				asterisk -rx "test generate results xml ${pwd()}/test-reports/unit-test-results.xml"
				if [ -f core* ] ; then
					echo "*** Found a core file after running unit tests ***"
					gdb asterisk core* -ex "bt full" -ex "thread apply all bt" --batch
					exit 1
				fi
				"""
		} catch(e) {
			error "Oops: ${e}"
		} finally {
			sudo 'killall -9 asterisk || :' 
			for (f in configs) {
				def path = "/etc/asterisk/${f}"
				sh "[ -f ${path}.backup ] && mv -f ${path}.backup ${path}"
			}
		}

		if (!fileExists("test-reports/unit-test-results.xml")) {
			error "No unit test results report was found"
		}
		sudo '''\
			sed -i -r -e 's@name="(.*)/([^"]+)"@classname="\\1" name="\\2"@g' -e :1 -e 's@(classname=".*)/(.*")@\\1.\\2@;t1' -e 's@name="[.]@name="@g' test-reports/*.xml
		'''
		sudo "rm -rf /var/spool/asterisk/voicemail/*"
		archiveArtifacts allowEmptyArchive: true, defaultExcludes: false, fingerprint: true,
			artifacts: 'test-reports/*.xml'
		junit testResults: "test-reports/*.xml",
			healthScaleFactor: 1.0,
			keepLongStdio: true
	}
}
