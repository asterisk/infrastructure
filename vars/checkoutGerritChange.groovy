def call(destination) {
	stage("checkout-${env.GERRIT_PROJECT}-gerrit") {
		def changeid = "${env.GERRIT_BRANCH}-${env.GERRIT_CHANGE_NUMBER}"
		def refspec = env.GERRIT_REFSPEC
		def url = getGerritServerUrl(env.GERRIT_NAME)
		def repo = "/srv/git/${env.GERRIT_PROJECT}.gerrit"
		def branch = env.GERRIT_BRANCH
		
		shell """\
		sudo rm -rf ${destination} >/dev/null 2>&1 || :
		git config --global user.email "jenkins2@asterisk.org"
		git config --global user.name "jenkins2"
		if [ -d ${repo} ] ; then
			pushd ${repo} 
			isbare=`git config --local core.bare 2>/dev/null || echo false`
			popd
		fi
		if [ -d ${repo} -a "\${isbare}" = "false" ] ; then
			sudo rm -rf ${repo}
		fi
		if [ ! -d ${repo} ] ; then
			git clone --bare "${url}/${env.GERRIT_PROJECT}" ${repo}
		else
			pushd ${repo}
			git fetch origin ${env.GERRIT_BRANCH}:${env.GERRIT_BRANCH}
			popd
		fi
		"""
 
		checkout poll: false,
			scm: [$class: 'GitSCM', branches: [[name: "${refspec}"]],
				doGenerateSubmoduleConfigurations: false,
				extensions: [
					[$class: 'CloneOption', noTags: true,
						honorRefspec: true, reference: repo],
					[$class: 'ScmName', name: "${env.GERRIT_PROJECT}.gerrit"],
					[$class: 'AuthorInChangelog'],
					[$class: 'RelativeTargetDirectory', relativeTargetDir: destination]
				],
				submoduleCfg: [],
				userRemoteConfigs: [
					[name: 'origin', refspec: "${refspec}:${refspec}",
						url: "${url}/${env.GERRIT_PROJECT}"]]]
		shell """\
			pushd ${destination}
			git fetch origin ${env.GERRIT_BRANCH}:${env.GERRIT_BRANCH}
			git rebase ${env.GERRIT_BRANCH}
			popd
		"""
	}
}
