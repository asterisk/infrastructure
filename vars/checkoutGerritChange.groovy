
def call(destination) {
	stage("checkout-${env.GERRIT_PROJECT}-gerrit") {
		def changeid = "${env.GERRIT_BRANCH}-${env.GERRIT_CHANGE_NUMBER}"
		def refspec = env.GERRIT_REFSPEC
		def url = getGerritServerUrl(env.GERRIT_NAME)
		def repo = "${env.GERRIT_PROJECT}.gerrit"
		def branch = env.GERRIT_BRANCH
		
		lock("${NODE_NAME}.${repo}") {
			shell """\
			if [ ! -d /srv/git/${env.GERRIT_PROJECT}.gerrit ] ; then
				git clone --bare "${url}/${env.GERRIT_PROJECT}" /srv/git/${repo}
			fi
			sudo chown -R jenkins:jenkins ${destination} 2>&1 || :
			"""
			try { 
			checkout poll: false,
				scm: [$class: 'GitSCM', branches: [[name: "${refspec}"]],
					doGenerateSubmoduleConfigurations: false,
					extensions: [
						[$class: 'CloneOption', honorRefspec: true, noTags: false,
							reference: "/srv/git/${repo}"],
						[$class: 'CleanBeforeCheckout'],
						[$class: 'ScmName', name: repo],
						[$class: 'AuthorInChangelog'],
						[$class: 'RelativeTargetDirectory', relativeTargetDir: destination]
					],
					submoduleCfg: [],
					userRemoteConfigs: [
						[name: 'origin', refspec: "refs/heads/${branch}:refs/heads/${branch} ${refspec}:${refspec}",
							url: "${url}/${env.GERRIT_PROJECT}"]]]		
			} catch (e) { 
				echo e.toString()
				echo e.getStackTrace().toString()
				throw e
			}
			shell """\
			pushd ${destination}
			git remote add local /srv/git/${repo} 2>&1 || : 
			git push local ${branch}:${branch} --force
			popd
			"""
		}
	}
}
