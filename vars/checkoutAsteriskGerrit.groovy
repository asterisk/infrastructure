
def call(destination) {
	stage('checkout-asterisk-gerrit') {
		def changeid = "${env.GERRIT_BRANCH}-${env.GERRIT_CHANGE_NUMBER}"
		def refspec = env.GERRIT_REFSPEC
		def url = getGerritServerUrl(env.GERRIT_NAME)
		def branch = env.GERRIT_BRANCH
		lock('asterisk.gerrit') {
			shell """\
			if [ ! -d /srv/git/asterisk.gerrit ] ; then
				git clone --bare "${url}/${env.GERRIT_PROJECT}" /srv/git/asterisk.gerrit
			fi
			sudo rm -rf ${destination} 
			git clone /srv/git/asterisk.gerrit ${destination}
			pushd ${destination}
			git remote rename origin local
			git remote add origin "${url}/${env.GERRIT_PROJECT}"
			git checkout ${branch}
			git push local ${branch}:${branch}
			git fetch --tags --progress origin +${refspec}:refs/heads/${changeid} --prune
			git checkout ${changeid}
			popd
			"""
		}
	}
}
