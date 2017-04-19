def call(test) {
	stage("run-testsuite") {
		dir("testsuite") {
			
			def command_line = ""
			def tests = []
			if (test.include_tests) {
				tests += test.include_tests
			} else if (test.exclude_tests) {
				def tests_string = sh returnStdout: true, script: 'find tests -maxdepth 1 -type d -printf "%p " | sed -r -e "s/(^| )tests //g"'
				def test_dirs = tests_string.split(' ')
				for (dir in test_dirs) {
					if (! (dir in test.exclude_tests)) {
						tests.push(dir)
					}
				}
			}
			tests.sort()
			
			for (t in tests) {
				command_line += " -t ${t}"
			}
			
			if (test.include_tags) {
				for (g in test.include_tags) {
					command_line += " -g ${g}"
				}
			}
			if (test.exclude_tags) {
				for (G in test.exclude_tags) {
					command_line += " -G ${G}"
				}
			}
			if (test.extra_args) {
				command_line += " ${test.extra_args}"
			}
				
			try {
				sudo """\
					chown -R jenkins:users . 
					[ -d /tmp/asterisk-testsuite ] && rm -rf /tmp/asterisk-testsuite
					./runtests.py ${command_line}
				"""
			} catch(e) {
				echo "Error running runtests.py ${command_line}"
				echo e.toString()
			} finally {
				try {
					sudo '''\
						set +e
						killall -9 asterisk
						pkill -9 -f runtests.py
						pkill -9 -f test_runner.py
					'''
				} catch (e) {
				}
			}

			sudo '''sed -i -r -e 's@name="(.*)/([^"]+)"@classname="\\1" name="\\2"@g' -e :1 -e 's@(classname=".*)/(.*")@\\1.\\2@;t1' -e 's@name="[.]@name="@g' asterisk-test-suite-report.xml'''

			fingerprint "asterisk-test-suite-report.xml"
			archiveArtifacts allowEmptyArchive: true, artifacts: 'asterisk-test-suite-report.xml logs/', defaultExcludes: false, fingerprint: true
			archiveArtifacts allowEmptyArchive: true, artifacts: 'logs/**/refs.txt logs/**/refleaks-summary.txt', defaultExcludes: false, fingerprint: true
			junit testResults: "asterisk-test-suite-report.xml",
				healthScaleFactor: 1.0,
				keepLongStdio: true
			sudo '''\
				./cleanup-test-remnants.sh
				rm -rf /tmp/asterisk-testsuite
			'''
		}
	}
}

