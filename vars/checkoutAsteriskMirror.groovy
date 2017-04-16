
def call(branch, destination) {
	stage('checkout-asterisk-mirror') {
		lock('asterisk.mirror') {
			shell """\
				if [ ! -d /srv/git/asterisk.mirror ] ; then
					git clone --bare git://git.asterisk.org/asterisk/asterisk.git /srv/git/asterisk.mirror
				fi
				sudo rm -rf ${destination} 
				git clone /srv/git/asterisk.mirror ${destination}
				pushd ${destination}
				git remote rename origin local
				git remote add origin git://git.asterisk.org/asterisk/asterisk.git
				git checkout ${branch}
				git push local ${branch}:${branch}
				popd
			"""
		}
    }
}
