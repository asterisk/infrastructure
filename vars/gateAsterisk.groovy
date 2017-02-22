import globals

def call(branch, gate_type) {
	if (!env.GERRIT_REFSPEC || !env.GERRIT_REFSPEC.length()) {
		error '''
				This job can't be triggered manually as it relies on environment variables
				provided by Gerrit.  You may be able to manually trigger it from the
				"Query and Trigger Gerrit Patches" main menu.
				'''.stripIndent()
	}
	timestamps {
		node("gate") {
			def url = env.GERRIT_CHANGE_URL
			
			manager.createSummary("/plugin/workflow-job/images/48x48/pipelinejob.png").appendText("Execution Node: ${NODE_NAME}", false)
			manager.build.displayName += "-${env.GERRIT_CHANGE_NUMBER}"
			
			def changeid = "${env.GERRIT_BRANCH}-${env.GERRIT_CHANGE_NUMBER}"
			echo "Changeid: ${changeid} Refspec: ${env.GERRIT_REFSPEC}"
			checkoutAsteriskGerrit(changeid, env.GERRIT_REFSPEC, "asterisk")
			echo "Branch: ${branch} Options: ${globals.ast_branches[branch]}"
			def build_options = globals.test_options[gate_type].build_options ?: globals.default_build_options
			buildAsterisk(branch, "${build_options} ${globals.ast_branches[branch].build_options}")
	
			if (gate_type == "unittst") {
				runAsteriskUnittests()
			} else {
				checkoutTestsuiteMirror("master", "testsuite")
				runTestsuite(globals.test_options[gate_type])
			}
		}
	}
}

/*
 gerritverificationpublisher([
	 verifyStatusValue: -1,
	 verifyStatusCategory: 'Failed',
	 verifyStatusComment: '${env.BUILD_TAG}',
	 verifyStatusName: "${env.JOB_NAME}",
	 verifyStatusReporter: 'Jenkins2',
	 verifyStatusRerun: 'regate'])
gerritverificationpublisher([
 verifyStatusValue: 1,
 verifyStatusCategory: 'Passed',
 verifyStatusComment: '${env.BUILD_TAG}',
 verifyStatusName: "${env.JOB_NAME}",
 verifyStatusReporter: 'Jenkins2',
 verifyStatusRerun: 'regate'])
*/
