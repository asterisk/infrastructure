// Not ready yet.

def call(branch, buildopts) {
	timestamps {
		node ("periodic && debug") {
			checkoutAsteriskMirror(branch, "asterisk")

			buildAsterisk(branch, buildopts)
			
			runAsteriskUnittests();

			checkoutTestsuiteMirror("master", "testsuite")
			
			runTestsuite()
		}
	}
}