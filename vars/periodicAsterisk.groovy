import globals

def call(branch, periodic_type) {
	manager.createSummary("/plugin/workflow-job/images/48x48/pipelinejob.png").appendText("Execution Node: ${NODE_NAME}", false)

	if (periodic_type == "doc") {
		runPeriodicDoc(branch)
		return
	}
	def build_options = globals.test_options[periodic_type].build_options ?: globals.default_build_options
	lock("${NODE_NAME}.asterisk.mirror") {
		checkoutProjectMirror("asterisk", branch, "asterisk")
		buildAsterisk(branch, "${build_options} ${globals.ast_branches[branch].build_options}", "")

		if (periodic_type == "unittst") {
			runAsteriskUnittests()
			shell """\
			pushd asterisk >/dev/null 2>&1
			git clean -fdx >/dev/null 2>&1
			popd >/dev/null 2>&1
			"""
			return
		}
		shell """\
		pushd asterisk >/dev/null 2>&1
		git clean -fdx >/dev/null 2>&1
		popd >/dev/null 2>&1
		"""
	}

	lock("${NODE_NAME}.testsuite.mirror") {
		checkoutProjectMirror("testsuite", "master", "testsuite")
		if (periodic_type == "realtime") {
			def db = globals.test_options[periodic_type].db
			setupAsteriskRealtime(branch, db.user, db.host, db.dbname, db.dsn)
		}
		runTestsuite(globals.test_options[periodic_type])
	}
}
