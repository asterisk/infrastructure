
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
			
			println command_line
			
			return
			
			try {
				sh """
					sudo chown -R jenkins:users . 
					[ -d /tmp/asterisk-testsuite ] && sudo chown -R jenkins:users /tmp/asterisk-testsuite
					sudo ./runtests.py ${testoptions}
				""".stripIndent()
			} catch(e) {
				println "Error running runtests.py"
			} finally {
				try {
					sh '''set +e
						sudo killall -9 asterisk
						sudo pkill -9 -f runtests.py
						sudo pkill -9 -f test_runner.py
					'''.stripIndent()
				} catch (e) {
				}
			}

			sh '''sed -i -r -e 's@name="(.*)/([^"]+)"@classname="\\1" name="\\2"@g' -e :1 -e 's@(classname=".*)/(.*")@\\1.\\2@;t1' -e 's@name="[.]@name="@g' asterisk-test-suite-report.xml'''

			fingerprint "asterisk-test-suite-report.xml"
			archiveArtifacts allowEmptyArchive: true, artifacts: 'asterisk-test-suite-report.xml logs/', defaultExcludes: false, fingerprint: true
			archiveArtifacts allowEmptyArchive: true, artifacts: 'logs/**/refs.txt logs/**/refleaks-summary.txt', defaultExcludes: false, fingerprint: true
			junit testResults: "asterisk-test-suite-report.xml",
				healthScaleFactor: 1.0,
				keepLongStdio: true
			stash includes: 'asterisk-test-suite-report.xml', name: 'tetssuite-results', useDefaultExcludes: false
		}
	}
}

