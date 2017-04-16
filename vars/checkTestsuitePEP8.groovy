def call() {
	checkoutTestsuiteMirror("master", "testsuite")

	stage("run-testsuite-pep8") {
		try {
			dir("testsuite") {
				shell '''\
				set +e
				which pep8 || exit 1
				pep8 . > ./testsuite-pep8.txt
				RC=$?
				cat ./testsuite-pep8.txt
				exit $RC
				'''
				archiveArtifacts allowEmptyArchive: true, artifacts: 'testsuite-pep8.txt', defaultExcludes: false
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
