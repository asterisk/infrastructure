def call(destination) {
	stage("checkout-${env.GERRIT_PROJECT}-gerrit") {
		def changeid = "${env.GERRIT_BRANCH}-${env.GERRIT_CHANGE_NUMBER}"
		def refspec = env.GERRIT_REFSPEC
		def url = getGerritServerUrl(env.GERRIT_NAME)
		def repo = "/srv/git/${env.GERRIT_PROJECT}.gerrit"
		def branch = env.GERRIT_BRANCH
		
		shell """\
		sudo rm -rf ${destination} >/dev/null 2>&1 || :
		isbare=`git -C ${repo} config --local core.bare 2>/dev/null || echo true`
		if [ -d ${repo} -a "\${isbare}" = "true" ] ; then
			rm -rf ${repo}
		fi
		if [ ! -d ${repo} ] ; then
			git clone "${url}/${env.GERRIT_PROJECT}" ${repo}
		fi
		sudo chown -R jenkins:jenkins ${repo} >/dev/null 2>&1 || :
		ln -s ${repo} ${destination}
		pushd ${destination}
		git checkout ${branch}
		git pull
		popd
		"""
 
		checkout poll: false,
			scm: [$class: 'GitSCM', branches: [[name: "${refspec}"]],
				doGenerateSubmoduleConfigurations: false,
				extensions: [
					[$class: 'CloneOption', noTags: false],
					[$class: 'CleanBeforeCheckout'],
					[$class: 'ScmName', name: "${env.GERRIT_PROJECT}.gerrit"],
					[$class: 'AuthorInChangelog'],
					[$class: 'RelativeTargetDirectory', relativeTargetDir: destination]
				],
				submoduleCfg: [],
				userRemoteConfigs: [
					[name: 'origin', refspec: "${refspec}:${refspec}",
						url: "${url}/${env.GERRIT_PROJECT}"]]]
	}
}
