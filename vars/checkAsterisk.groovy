import globals

def call(branch, arch) {
	if (!env.GERRIT_REFSPEC || !env.GERRIT_REFSPEC.length()) {
		error '''
				This job can't be triggered manually as it relies on environment variables
				provided by Gerrit.  You may be able to manually trigger it from the
				"Query and Trigger Gerrit Patches" main menu.
				'''.stripIndent()
	}
	manager.build.displayName = "${env.GERRIT_CHANGE_NUMBER}"
	def node_family = ""

	node("build && ${arch}-bit") {
		manager.createSummary("/plugin/workflow-job/images/48x48/pipelinejob.png").appendText("Execution Node: ${NODE_NAME}", false)

		checkoutAsteriskGerrit("asterisk")
		def build_options = globals.test_options["unittst"].build_options ?: globals.default_build_options
		buildAsterisk(branch, "${build_options} ${globals.ast_branches[branch].build_options}", "asterisk-install")
		node_family = getNodeFamily("${NODE_NAME}")
	}

	node("check && ${arch}-bit && ${node_family}") {
		manager.createSummary("/plugin/workflow-job/images/48x48/pipelinejob.png").appendText("Execution Node: ${NODE_NAME}", false)
		installAsteriskFromStash("asterisk-install", "/")

		runAsteriskUnittests()
	}
}
