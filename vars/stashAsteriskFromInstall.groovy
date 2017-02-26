import globals

def call(dest, stashname)
{
	if (dest.length() == 0) {
		error "Destination can't be empty"
	}
	echo "Stashing ${dest}/** to ${stashname}" 
	sudo """\
		rm ${stashname}.zip 2>/dev/null || :
		if [ -d ${dest}/usr/lib64 ] ; then
			mv ${dest}/usr/lib64 ${dest}/usr/lib
		fi
		rm -rf ${dest}/usr/include || :
		rm -rf ${dest}/usr/share || :
		rm -rf ${dest}/var/lib/asterisk/sounds || :
		rm -rf ${dest}/var/lib/asterisk/moh || : 
	"""
	sudo """\
		pushd ${dest}
		zip -q -r ../${stashname}.zip .
		popd
	"""
	stash includes: "${stashname}.zip", name: stashname
	sudo "rm ${stashname}.zip 2>/dev/null || :"
}
