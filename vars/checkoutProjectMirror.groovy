def call(project, branch, destination) {
	stage("checkout-${project}") {
		lock("${NODE_NAME}.${project}.mirror") {
			shell """\
			if [ ! -d /srv/git/${project}.mirror ] ; then
				git clone --bare git://git.asterisk.org/asterisk/${project}.git /srv/git/${project}.mirror
			fi
			sudo chown -R jenkins:jenkins ${destination} 2>&1 || :
			"""
		
			checkout poll: false,
				scm: [$class: 'GitSCM', branches: [[name: "*/${branch}"]],
					doGenerateSubmoduleConfigurations: false,
					extensions: [
						[$class: 'CloneOption', honorRefspec: true, noTags: false,
							reference: "/srv/git/${project}.mirror"],
						[$class: 'LocalBranch', localBranch: "${branch}"],
						[$class: 'CleanBeforeCheckout'],
						[$class: 'ScmName', name: "${project}.mirror"],
						[$class: 'AuthorInChangelog'],
						[$class: 'RelativeTargetDirectory', relativeTargetDir: "${destination}"]
					],
					submoduleCfg: [],
					userRemoteConfigs: [
						[name: 'origin', refspec: "+refs/heads/${branch}:refs/remotes/origin/${branch}",
							url: "git://git.asterisk.org/asterisk/${project}.git"]]]		
				
			shell """\
				pushd ${destination}
				git remote add local /srv/git/${project}.mirror 2>&1 || :
				git push local ${branch}:${branch} --force
				popd
			"""
		}
    }
}
