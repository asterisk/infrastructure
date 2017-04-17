def call(project, branch, destination) {
	stage("checkout-${project}") {
		lock("${NODE_NAME}.${project}.mirror") {
			shell """\
				if [ ! -d /srv/git/${project}.mirror ] ; then
					git clone --bare git://git.asterisk.org/asterisk/${project}.git /srv/git/${project}.mirror
				fi
				rm -rf ${destination}
				git clone /srv/git/${project}.mirror ${destination}
				pushd ${destination}
				git remote rename origin local
				git remote add origin git://git.asterisk.org/asterisk/${project}.git
				git checkout ${branch}
				git push local ${branch}:${branch}
				popd
			"""
		}
    }
}
