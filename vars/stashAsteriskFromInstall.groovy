import globals

def call(dest, stashname)
{
	if (dest.length() == 0) {
		error "Destination can't be empty"
	}
	echo "Stashing ${dest}/** to ${stashname}" 
	sh """\
		if [ -d ${dest}/usr/lib64 ] ; then
			sudo mv ${dest}/usr/lib64 ${dest}/usr/lib
		fi
		sudo rm -rf ${dest}/usr/include
		sudo rm -rf ${dest}/usr/share
	""".stripIndent()
	stash includes: "${dest}/**", name: stashname
}
