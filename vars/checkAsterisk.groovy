import globals

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
		checkoutGerritChange("asterisk")
		def build_options = globals.test_options["unittst"].build_options ?: globals.default_build_options
		buildAsterisk(branch, "${build_options} ${globals.ast_branches[branch].build_options}", "")
		runAsteriskUnittests()
		gerritverificationpublisher verifyStatusValue: 1, verifyStatusCategory: 'Passed',
			verifyStatusComment: '${env.BUILD_TAG}', verifyStatusName: "${env.JOB_NAME}",
			verifyStatusReporter: 'Jenkins2', verifyStatusRerun: 'recheck'
				
	} catch (e) {
		if (e instanceof hudson.AbortException) {
			println "Build aborted"
			echo e.getStackTrace().toString()
		} else { 
			gerritverificationpublisher verifyStatusValue: -1, verifyStatusCategory: 'Failed',
				verifyStatusComment: '${env.BUILD_TAG}', verifyStatusName: "${env.JOB_NAME}",
				verifyStatusReporter: 'Jenkins2', verifyStatusRerun: 'recheck'
			error e.getStackTrace().toString()
		}
	}
}


