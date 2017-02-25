import globals

def call(branch, arch) {
	if (!env.GERRIT_REFSPEC || !env.GERRIT_REFSPEC.length()) {
		error '''
				This job can't be triggered manually as it relies on environment variables
				provided by Gerrit.  You may be able to manually trigger it from the
				"Query and Trigger Gerrit Patches" main menu.
				'''.stripIndent()
	}
	timestamps {
			def url = env.GERRIT_CHANGE_URL
			
			manager.createSummary("/plugin/workflow-job/images/48x48/pipelinejob.png").appendText("Execution Node: ${NODE_NAME}", false)
			manager.build.displayName += "-${env.GERRIT_CHANGE_NUMBER}"
			
			def changeid = "${env.GERRIT_BRANCH}-${env.GERRIT_CHANGE_NUMBER}"
			checkoutAsteriskGerrit(changeid, env.GERRIT_REFSPEC, "asterisk")

		node("build && ${arch}-bit") {
			def build_options = globals.test_options["unittst"].build_options ?: globals.default_build_options
			sh "mkdir asterisk-install"
			buildAsterisk(branch, "${build_options} ${globals.ast_branches[branch].build_options} DESTDIR ${WORKSPACE}/asterisk-install")
			sh "tar -czf asterisk-install.tar.gz asterisk-install"
			stash includes: 'asterisk-install.tar.gz', name: 'asterisk-install'
		node("check && ${arch}-bit") {
			echo "DO NOTHING"
//			runAsteriskUnittests()
		}
	}
}
