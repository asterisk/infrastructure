import globals

def call(branch, arch) {
	
	if (!env.GERRIT_REFSPEC || !env.GERRIT_REFSPEC.length()) {
		error '''
				This job can't be triggered manually as it relies on environment variables
				provided by Gerrit.  You may be able to manually trigger it from the
				"Query and Trigger Gerrit Patches" main menu.
				'''.stripIndent()
	}
	def changeid = "${env.GERRIT_BRANCH}-${env.GERRIT_CHANGE_NUMBER}"
	manager.listener.logger.println("Starting checkout")
	checkoutAsteriskGerrit(changeid, env.GERRIT_REFSPEC, "asterisk")
	manager.listener.logger.println("Starting build")
	buildAsterisk(branch, "${globals.default_build_options} ${globals.ast_branches[branch].build_options}")
	manager.listener.logger.println("Starting unit tests")
	try {
		echo "UT"
//		runAsteriskUnittests()
	} catch(e) {
		println "Sending Failed to gerrit"
		gerritverificationpublisher([
			verifyStatusValue: -1,
			verifyStatusCategory: 'Failed',
			verifyStatusComment: '${env.BUILD_TAG}',
			verifyStatusName: currentBuild.displayName,
			verifyStatusReporter: 'Jenkins2',
			verifyStatusRerun: 'recheck'])
		error "Failed: ${e}"
	}
	println "Sending Passed to gerrit"
	gerritverificationpublisher([
		verifyStatusValue: 1,
		verifyStatusCategory: 'Passed',
		verifyStatusComment: '${env.BUILD_TAG}',
		verifyStatusName: currentBuild.displayName,
		verifyStatusReporter: 'Jenkins2',
		verifyStatusRerun: 'recheck'])
}
