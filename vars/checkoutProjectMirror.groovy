def call(project, branch, destination) {
	stage("checkout-${project}") {
		def repo = "/srv/git/${project}.mirror"
		shell """\
		sudo rm -rf ${destination} >/dev/null 2>&1 || :
		isbare=`git -C ${repo} config --local core.bare 2>/dev/null || echo true`
		if [ -d ${repo} -a "\${isbare}" = "true" ] ; then
			rm -rf ${repo}
		fi
		if [ ! -d ${repo} ] ; then
			git clone git://git.asterisk.org/asterisk/${project}.git ${repo}
		fi
		sudo chown -R jenkins:jenkins ${repo} >/dev/null 2>&1 || :
		ln -s ${repo} ${destination}
		"""
		
		checkout poll: false,
			scm: [$class: 'GitSCM', branches: [[name: "*/${branch}"]],
				doGenerateSubmoduleConfigurations: false,
				extensions: [
					[$class: 'CloneOption', noTags: false],
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
    }
}
