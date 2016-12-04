/*
 * This function is not currently used.
 */
def call(type) {
	
	if (!params.SLAVE) {
		node {
		def streams = [:]
		streams["${env.JOB_NAME}-32-bit"] = {
			build job: 'A1', parameters: [booleanParam(name: 'SLAVE', value: true), string(name: 'type', value: '64-bit')]
		}
		streams["${env.JOB_NAME}-64-bit"] = {
				build job: 'A1', parameters: [booleanParam(name: 'SLAVE', value: true), string(name: 'type', value: '32-bit')]
		}
		try {
			parallel(streams)
			unstash "32-bit.stash"
			unstash "64-bit.stash"
		} catch(e) {
			println e
			currentBuild.result = 'FAILURE'
		}
		}
	} else {
		node("64-bit") {
		manager.build.displayName = manager.build.number + " ${params.type}"
		manager.build.description = "tyhis is a test"
		manager.createSummary().appendText("XxXxXx", false)
		writeFile file: "${params.type}.txt", text: "aaaaaaaaaaaaaaa"
		if (params.type == "32-bit") error "SUCK"
		stash name: "${params.type}.stash", includes: "*.txt"
			for (i = 0; i < 10; i++) {
				sleep 1
				echo "${i}"
			}
		}
	}
}
