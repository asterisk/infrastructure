import globals

@NonCPS
def xxx(_gt) {
	currentBuild.displayName = "${env.GERRIT_CHANGE_NUMBER}-${_gt}"
}

def call() {
	if (params.PARENT_BUILD_ID == '0') {

		//		manager.build.displayName += " Gate ${GERRIT_CHANGE_NUMBER}"

		def gerritParams = copyGerritEnvToParams()

		//			for (i = 0; i < globals.ast_branches[branch].gate_types.size(); i++) {
		//			def gt = globals.ast_branches[branch].gate_types[i]
		parallel(
				"Gate ${GERRIT_CHANGE_NUMBER}-unittst": {
					node("gate") {
						def url = getGerritServerUrl(env.GERRIT_NAME)
						manager.createSummary("/plugin/workflow-job/images/48x48/pipelinejob.png").appendText("abc: Execution Node: ${NODE_NAME}", false)
						timestamps {
							stage "a"
								sleep 2
							stage "b"
								sleep 2
							stage "c"
								sleep 10
							//						gateAsteriskTest(params.BRANCH, _gt)
						}
					}
				},
				"Gate ${GERRIT_CHANGE_NUMBER}-extmwi": {
					node("gate") {
						def url = getGerritServerUrl(env.GERRIT_NAME)
						manager.createSummary("/plugin/workflow-job/images/48x48/pipelinejob.png").appendText("def: Execution Node: ${NODE_NAME}", false)
						timestamps {
							stage "a"
								sleep 2
							stage "b"
								sleep 2
							stage "c"
								sleep 10
							//						gateAsteriskTest(params.BRANCH, _gt)
						}
					}
				},
		)

		/*
		 build job: "${JOB_NAME}-${gt}", propagate: true, wait: true,
		 parameters: ([
		 string(name: 'PARENT_BUILD_ID', value: env.BUILD_ID),
		 string(name: 'BRANCH', value: params.BRANCH),
		 string(name: 'GATE_TYPE', value: gt)]
		 + gerritParams)
		 */
		//			}
	} else {
		copyGerritParamsToEnv()
		if (!env.GERRIT_NAME) {
			error "No parent build matching ${params.PARENT_BUILD_ID} could be found"
		}

		def url = getGerritServerUrl(env.GERRIT_NAME)
		manager.build.displayName += "-${env.GERRIT_CHANGE_NUMBER}"
		manager.addBadge("/plugin/gerrit-trigger/images/icon16.png", "${env.GERRIT_CHANGE_NUMBER}", "${url}/${env.GERRIT_CHANGE_NUMBER}")
		node("gate") {
			manager.createSummary("/plugin/workflow-job/images/48x48/pipelinejob.png").appendText("Execution Node: ${NODE_NAME}", false)
			timestamps {
				gateAsteriskTest(params.BRANCH, params.GATE_TYPE)
			}
		}
	}
}
