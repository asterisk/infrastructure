
def call() {
	timestamps {
		node("check") {
			checkoutTestsuiteMirror("master", "testsuite")

			stage("run-testsuite-unitests") {
				dir("testsuite") {
					def test_names = [
						"buildoptions",
						"channel_test_condition",
						"sippversion",
						"version"
					]
					for (test_name in test_names) {
						echo " ==> Executing " + test_name
						sh "python ./lib/python/asterisk/" + test_name + ".py"
					}
				}
			}
		}
	}
}
