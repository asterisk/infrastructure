import globals

def call(branch, gate_type) {
	if (!env.GERRIT_REFSPEC || !env.GERRIT_REFSPEC.length()) {
		error '''
				This job can't be triggered manually as it relies on environment variables
				provided by Gerrit.  You may be able to manually trigger it from the
				"Query and Trigger Gerrit Patches" main menu.
				'''.stripIndent()
	}
	def changeid = "${env.GERRIT_BRANCH}-${env.GERRIT_CHANGE_NUMBER}"
	checkoutAsteriskGerrit(changeid, env.GERRIT_REFSPEC, "asterisk")
	def build_options = globals.test_options[gate_type].build_options ?: globals.default_build_options
	buildAsterisk(branch, "${build_options} ${globals.ast_branches[branch].build_options}")
	try {
		if (gate_type == "unittst") {
			runAsteriskUnittests()
		} else {
			checkoutTestsuiteMirror("master", "testsuite")
			runTestsuite(globals.test_options[gate_type].options)
		}
	} catch(e) {
		println "Sending Failed to gerrit"
		gerritverificationpublisher([
			verifyStatusValue: -1,
			verifyStatusCategory: 'Failed',
			verifyStatusComment: '${env.BUILD_TAG}',
			verifyStatusName: "${env.JOB_NAME}",
			verifyStatusReporter: 'Jenkins2',
			verifyStatusRerun: 'regate'])
		error "Failed: ${e}"
	}
	println "Sending Passed to gerrit"
	gerritverificationpublisher([
		verifyStatusValue: 1,
		verifyStatusCategory: 'Passed',
		verifyStatusComment: '${env.BUILD_TAG}',
		verifyStatusName: "${env.JOB_NAME}",
		verifyStatusReporter: 'Jenkins2',
		verifyStatusRerun: 'regate'])
}
