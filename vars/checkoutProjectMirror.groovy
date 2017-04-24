def call(project, branch, destination) {
	stage("checkout-${project}") {
		def repo = "/srv/git/${project}.mirror"
		shell """\
		sudo rm -rf ${destination} >/dev/null 2>&1 || :
		if [ -d ${repo} ] ; then
			pushd ${repo} 
			isbare=`git config --local core.bare 2>/dev/null || echo false`
			popd
		fi
		if [ -d ${repo} -a "\${isbare}" = "false" ] ; then
			sudo rm -rf ${repo}
		fi
		if [ ! -d ${repo} ] ; then
			git clone --bare git://git.asterisk.org/asterisk/${project}.git ${repo}
		else
			pushd ${repo} 
			git fetch origin
			popd
		fi
		"""
		
		checkout poll: false,
			scm: [$class: 'GitSCM', branches: [[name: "*/${branch}"]],
				doGenerateSubmoduleConfigurations: false,
				extensions: [
					[$class: 'CloneOption', noTags: false,
						honorRefspec: true, reference: repo],
					[$class: 'LocalBranch', localBranch: "${branch}"],
					[$class: 'ScmName', name: "${project}.mirror"],
					[$class: 'AuthorInChangelog'],
					[$class: 'RelativeTargetDirectory', relativeTargetDir: "${destination}"]
				],
				submoduleCfg: [],
				userRemoteConfigs: [
					[name: 'origin', refspec: "+refs/heads/${branch}:refs/remotes/origin/${branch}",
						url: "git://git.asterisk.org/asterisk/${project}.git"]]]		
    }
}
