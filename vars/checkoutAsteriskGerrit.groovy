
def call(branch, refspec, destination) {
	stage("checkout") {
		def url = getGerritServerUrl(env.GERRIT_NAME)
		
		echo "URL: ${url}  Changeid: ${branch} Refspec: ${refspec} Project: ${env.GERRIT_PROJECT}"
		
		sh "sudo chown -R jenkins:users ${destination} || :"
		checkout changelog: false, poll: false, scm: [
			$class: 'GitSCM',
			branches: [[name: branch]],
			doGenerateSubmoduleConfigurations: false,
			extensions: [
				[$class: 'RelativeTargetDirectory', relativeTargetDir: destination],
				[$class: 'CleanBeforeCheckout']
			],
			submoduleCfg: [],
			userRemoteConfigs: [
				[name: 'gerrit', refspec: "+${refspec}:refs/heads/${branch}", url: "${url}/${env.GERRIT_PROJECT}"]]
		]
	}
}
