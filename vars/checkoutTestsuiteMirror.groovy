
def call(branch, destination) {
	stage('checkout-testsuite') {
        sh "sudo rm -rf /tmp/asterisk-testsuite/ || : " 
        sh "sudo chown -R jenkins:users testsuite || : "
    	checkout changelog: false, poll: false, scm: [
    		$class: 'GitSCM',
        	branches: [[name: branch]],
        	doGenerateSubmoduleConfigurations: false,
        	extensions: [
	        	[$class: 'RelativeTargetDirectory', relativeTargetDir: destination],
            	[$class: 'CleanBeforeCheckout'],
            	[$class: 'CloneOption', shallow: true]
        	],
        	submoduleCfg: [],
        	userRemoteConfigs: [[url: 'git://git.asterisk.org/asterisk/testsuite.git']]
    	]
    }
}

