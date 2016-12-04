
def call() {
	timestamps {
		node("check") {
			checkoutTestsuiteMirror("master", "testsuite")

			stage("run-testsuite-pep8") {
				dir("testsuite") {
					sh '''
                    set +e
                    which pep8 || exit 1
                    pep8 . > ./testsuite-pep8.txt
                    RC=$?
                    cat ./testsuite-pep8.txt
                    exit $RC
                    '''
					archiveArtifacts allowEmptyArchive: true, artifacts: 'testsuite-pep8.txt', defaultExcludes: false
				}
			}
		}
	}
}
