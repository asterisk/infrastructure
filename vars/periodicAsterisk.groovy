import globals

def call(branch, periodic_type) {
	timestamps {
		node ("periodic") {
			checkoutAsteriskMirror(branch, "asterisk")

			def build_options = globals.test_options[periodic_type].build_options ?: globals.default_build_options
			buildAsterisk(branch, "${build_options} ${globals.ast_branches[branch].build_options}")

			if (periodic_type == "unittst") {
				runAsteriskUnittests()
			} else {
				checkoutTestsuiteMirror("master", "testsuite")
				runTestsuite(globals.test_options[periodic_type])
			}
		}
	}
}