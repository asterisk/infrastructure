import globals

def call(branch, gate_type) {
	if (!env.GERRIT_REFSPEC || !env.GERRIT_REFSPEC.length()) {
		error '''\
			This job can not be triggered manually as it relies on environment variables
			provided by Gerrit.  You may be able to manually trigger it from the
			"Query and Trigger Gerrit Patches" main menu.
			'''.stripIndent()
	}
	manager.build.displayName = "${env.GERRIT_CHANGE_NUMBER}"
	manager.createSummary("/plugin/workflow-job/images/48x48/pipelinejob.png").appendText("Execution Node: ${NODE_NAME}", false)
		
	try {
		def build_options = globals.test_options[gate_type].build_options ?: globals.default_build_options
		lock("${NODE_NAME}.${env.GERRIT_PROJECT}.gerrit") {
			checkoutGerritChange("asterisk")
			buildAsterisk(branch, "${build_options} ${globals.ast_branches[branch].build_options}", "")
			shell """\
			pushd asterisk >/dev/null 2>&1
			git clean -fdx >/dev/null 2>&1
			popd >/dev/null 2>&1
			"""
		}
		lock("${NODE_NAME}.testsuite.mirror") {
			checkoutProjectMirror("testsuite", "master", "testsuite")
			runTestsuite(globals.test_options[gate_type])
		}
		gerritverificationpublisher verifyStatusValue: 2, verifyStatusCategory: 'Passed',
			verifyStatusComment: '${env.BUILD_TAG}', verifyStatusName: "${env.JOB_NAME}",
			verifyStatusReporter: 'Jenkins2', verifyStatusRerun: 'regate'
	} catch (e) {
		gerritverificationpublisher verifyStatusValue: -1, verifyStatusCategory: 'Failed',
			verifyStatusComment: '${env.BUILD_TAG}', verifyStatusName: "${env.JOB_NAME}",
			verifyStatusReporter: 'Jenkins2', verifyStatusRerun: 'regate'
		error e.getStackTrace().toString()
	}
}
