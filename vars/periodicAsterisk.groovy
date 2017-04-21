import globals

def call(branch, periodic_type) {
	manager.createSummary("/plugin/workflow-job/images/48x48/pipelinejob.png").appendText("Execution Node: ${NODE_NAME}", false)

	if (periodic_type == "doc") {
		runPeriodicDoc(branch)
		return
	}

	checkoutProjectMirror("asterisk", branch, "asterisk")
			
	def build_options = globals.test_options[periodic_type].build_options ?: globals.default_build_options
	buildAsterisk(branch, "${build_options} ${globals.ast_branches[branch].build_options}", "")

	if (periodic_type == "unittst") {
		runAsteriskUnittests()
		return;
	}

	checkoutProjectMirror("testsuite", "master", "testsuite")
	if (periodic_type == "realtime") {
		def db = globals.test_options[periodic_type].db
		setupAsteriskRealtime(branch, db.user, db.host, db.dbname, db.dsn)
	}
	runTestsuite(globals.test_options[periodic_type])
}
