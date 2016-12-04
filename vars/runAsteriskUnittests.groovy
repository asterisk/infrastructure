def call() {
	stage("unittests") {

		env.PATH = "${env.PATH}:/usr/lib/ccache:/usr/local/bin:/usr/sbin:/usr/local/sbin"
		
		sh '''\
			set +e
			which asterisk || (echo "Asterisk isn't installed" ; exit 1) || exit 1
			sudo killall -9 asterisk || echo "Asterisk not running (good)" 
			sudo chown -R jenkins:users /etc/asterisk
			# We create the link so we can use the pipeline writeFile function
			# which only writes to the workspace.
			ln -sf /etc/asterisk etc-asterisk
		'''.stripIndent()

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
				sh "[ -f ${path} ] && mv -f ${path} ${path}.backup"
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

			dir("asterisk") {
				sh """\
					[ -d test-reports ] && sudo rm -rf test-reports
					mkdir test-reports
					sudo asterisk -gn
					sleep 3
					sudo asterisk -rx 'core waitfullybooted'
					sleep 1
					sudo asterisk -rx 'test execute all'
					sudo asterisk -rx 'test generate results xml ${pwd()}/test-reports/unit-test-results.xml'
					if [ -f core* ] ; then
						echo '*** Found a core file after running unit tests ***'
						sudo gdb asterisk core* -ex 'bt full' -ex 'thread apply all bt' --batch
						exit 1
					fi
				""".stripIndent()
			}
		} catch(e) {
			error "Oops: ${e}"
		} finally {
			sh 'sudo killall -9 asterisk || echo "Asterisk not running (good)"' 
			for (f in configs) {
				def path = "/etc/asterisk/${f}"
				sh "[ -f ${path}.backup ] && mv -f ${path}.backup ${path}"
			}
		}

		if (!fileExists("asterisk/test-reports/unit-test-results.xml")) {
			error "No unit test results report was found"
		}
		sh '''
		sed -i -r -e 's@name="(.*)/([^"]+)"@classname="\\1" name="\\2"@g' -e :1 -e 's@(classname=".*)/(.*")@\\1.\\2@;t1' -e 's@name="[.]@name="@g' asterisk/test-reports/*.xml
		'''.stripIndent()
		
		archiveArtifacts allowEmptyArchive: true, artifacts: 'asterisk/test-reports/*.xml', defaultExcludes: false, fingerprint: true
		stash name: "unit-test-results", includes: "asterisk/test-reports/*.xml"
		junit testResults: "asterisk/test-reports/*.xml",
			healthScaleFactor: 1.0,
			keepLongStdio: true
	}
}
