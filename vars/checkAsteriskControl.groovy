import globals

def call() {
	if (params.PARENT_BUILD_ID == '0') {
		manager.build.displayName += " Check ${GERRIT_CHANGE_NUMBER}-control"
		stage("check") {
			def streams = [:]
			streams["Check ${GERRIT_CHANGE_NUMBER}-64"] = {
				build job: JOB_NAME, propagate: true, wait: true,
				parameters: ([
					string(name: 'PARENT_BUILD_ID', value: env.BUILD_ID),
					string(name: 'BRANCH', value: params.BRANCH),
					string(name: 'arch', value: '64')]
				)
			}
			streams["Check ${GERRIT_CHANGE_NUMBER}-32"] = {
				build job: JOB_NAME, propagate: true, wait: true,
				parameters: ([
					string(name: 'PARENT_BUILD_ID', value: env.BUILD_ID),
					string(name: 'BRANCH', value: params.BRANCH),
					string(name: 'arch', value: '32')]
				)
			}
			try {
				parallel(streams)
			} catch(e) {
				println e
				currentBuild.result = 'FAILURE'
			} finally {
				//					setGerritReview()
			}
		}
	} else {
		if (!copyParentGerritEnv(params.PARENT_BUILD_ID)) {
			error "No parent build matching ${params.PARENT_BUILD_ID} could be found"
		}
		def url = getGerritServerUrl(env.GERRIT_NAME)

		manager.build.displayName += " Check ${env.GERRIT_CHANGE_NUMBER}-${params.arch}"
		manager.addBadge("/plugin/gerrit-trigger/images/icon16.png", "${env.GERRIT_CHANGE_NUMBER}", "${url}/${env.GERRIT_CHANGE_NUMBER}")
		node("check && ${params.arch}-bit") {
			manager.createSummary("/plugin/workflow-job/images/48x48/pipelinejob.png").appendText("Execution Node: ${NODE_NAME}", false)
			checkAsteriskTest(params.BRANCH, "${params.arch}-bit")
		}
	}
}

