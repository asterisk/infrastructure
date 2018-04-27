def call(branch, arch) {
	if (!env.GERRIT_REFSPEC || !env.GERRIT_REFSPEC.length()) {
		error '''
			This job can not be triggered manually as it relies on environment variables
			provided by Gerrit.  You may be able to manually trigger it from the
			"Query and Trigger Gerrit Patches" main menu.
			'''.stripIndent()
	}
	manager.build.displayName = "${env.GERRIT_CHANGE_NUMBER}"
	manager.createSummary("/plugin/workflow-job/images/48x48/pipelinejob.png").appendText("Execution Node: ${NODE_NAME}", false)

	try {
		checkoutGerritChange("testsuite")
		stage("run-testsuite-unitests") {
			dir("testsuite") {
				sh "./self_test"
				gerritverificationpublisher verifyStatusValue: 1, verifyStatusCategory: 'Passed',
					verifyStatusComment: '${env.BUILD_TAG}', verifyStatusName: "${env.JOB_NAME}",
					verifyStatusReporter: 'Jenkins2', verifyStatusRerun: 'recheck'
			}
		}
	} catch(e) {
		gerritverificationpublisher verifyStatusValue: -1, verifyStatusCategory: 'Failed',
			verifyStatusComment: '${env.BUILD_TAG}', verifyStatusName: "${env.JOB_NAME}",
			verifyStatusReporter: 'Jenkins2', verifyStatusRerun: 'recheck'
		error e.getStackTrace().toString()
	}
}

