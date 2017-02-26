import globals

def call(branch, periodic_type) {
	if (periodic_type == "doc") {
		node ("periodic-${periodic_type} && 64-bit") {
//			runPeriodicDoc(branch)
		}
		return
	}
			
	def node_family = ""
		
	node("build && 64-bit") {
		manager.createSummary("/plugin/workflow-job/images/48x48/pipelinejob.png").appendText("Execution Node: ${NODE_NAME}", false)
		checkoutAsteriskMirror(branch, "asterisk")
			
		def build_options = globals.test_options[periodic_type].build_options ?: globals.default_build_options
		buildAsterisk(branch, "${build_options} ${globals.ast_branches[branch].build_options}", "asterisk-install")
		node_family = getNodeFamily("${NODE_NAME}")
	}

	node ("periodic-${periodic_type} && 64-bit && ${node_family}") {
		manager.createSummary("/plugin/workflow-job/images/48x48/pipelinejob.png").appendText("Execution Node: ${NODE_NAME}", false)
		installAsteriskFromStash("asterisk-install", "/")
			
		if (periodic_type == "unittst") {
			runAsteriskUnittests()
			return;
		}

		checkoutTestsuiteMirror("master", "testsuite")
		if (periodic_type == "realtime") {
			def db = globals.test_options[periodic_type].db
			setupAsteriskRealtime(branch, db.user, db.host, db.dbname, db.dsn)
		}
		runTestsuite(globals.test_options[periodic_type])
	}
}
