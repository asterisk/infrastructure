
def call(branch, destination) {
	stage('checkout-asterisk-mirror') {
		sh "sudo chown -R jenkins:users ${destination} || :" 
    	checkout changelog: false, poll: false, scm: [
    		$class: 'GitSCM',
        	branches: [[name: branch]],
        	doGenerateSubmoduleConfigurations: false,
        	extensions: [
				[$class: 'PruneStaleBranch'],
	        	[$class: 'RelativeTargetDirectory', relativeTargetDir: destination],
            	[$class: 'CleanBeforeCheckout'],
            	[$class: 'CloneOption', shallow: true]
        	],
        	submoduleCfg: [],
        	userRemoteConfigs: [[url: 'git://git.asterisk.org/asterisk/asterisk.git']]
    	]
		dir (destination) {
			sh "git gc --prune=all || : "
		}
    }
}

