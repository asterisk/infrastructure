
def call() {
	checkoutTestsuiteMirror("master", "testsuite")

	stage("run-testsuite-unitests") {
		dir("testsuite") {
			def test_names = [
				"buildoptions",
				"channel_test_condition",
				"sippversion",
				"version"
			]
			try {
				for (test_name in test_names) {
					echo " ==> Executing " + test_name
					sh "python ./lib/python/asterisk/" + test_name + ".py"
				}
				gerritverificationpublisher verifyStatusValue: 1, verifyStatusCategory: 'Passed',
					verifyStatusComment: '${env.BUILD_TAG}', verifyStatusName: "${env.JOB_NAME}",
					verifyStatusReporter: 'Jenkins2', verifyStatusRerun: 'recheck'
			} catch(e) {
				gerritverificationpublisher verifyStatusValue: -1, verifyStatusCategory: 'Failed',
					verifyStatusComment: '${env.BUILD_TAG}', verifyStatusName: "${env.JOB_NAME}",
					verifyStatusReporter: 'Jenkins2', verifyStatusRerun: 'recheck'
				error e
			}
		}
	}
}
