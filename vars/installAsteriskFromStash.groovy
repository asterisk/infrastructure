import globals

def call(stashname, dest)
{
	def source = stashname
	unstash stashname
	
	sudo """\
		rm -rf ${source} || : 
		rm -rf asterisk || : 
		unzip -q -d ${source} ${stashname}.zip
		rm -f ${stashname}.zip || :
	"""
	if (dest == "/") {
		dest = ""
	}
	sudo """\
echo Installing samples to ${dest}/etc/asterisk 
rm -rf ${dest}/etc/asterisk >/dev/null 2>&1 || :
install -o jenkins -d ${dest}/etc/asterisk
install -o jenkins ${source}/etc/asterisk/* ${dest}/etc/asterisk/

echo Cleaning ${dest}/usr/lib{,64} 
rm -rf ${dest}/usr/lib/asterisk >/dev/null 2>&1 || :
rm -rf ${dest}/usr/lib64/asterisk >/dev/null 2>&1 || :
rm -rf ${dest}/usr/lib/libasterisk* >/dev/null 2>&1 || :
rm -rf ${dest}/usr/lib64/libasterisk* >/dev/null 2>&1 || :

destlib="lib"
if [ -d ${dest}/usr/lib64 -a `uname -m` = 'x86_64' ] ; then
   destlib="lib64"
fi

echo Installing shared libraries to ${dest}/usr/\$destlib/
install  ${source}/usr/lib/libasterisk* ${dest}/usr/\$destlib/
install -d ${dest}/usr/\$destlib/asterisk/modules 
install ${source}/usr/lib/asterisk/modules/*  ${dest}/usr/\$destlib/asterisk/modules/
ldconfig ${dest}/usr/\$destlib

for d in lib log run spool ; do
	echo Installing ${source}/var/\${d}/asterisk ${dest}/var/\${d}/ 
	rm -rf ${dest}/var/\${d}/asterisk >/dev/null 2>&1 || :
	install -o jenkins -d ${dest}/var/\${d}/asterisk
	rsync -q -vaH ${source}/var/\${d}/asterisk/. ${dest}/var/\${d}/asterisk/
	chown -R jenkins:users ${dest}/var/\${d}/asterisk
done

echo Installing sounds to ${dest}/var/lib/asterisk/sounds/en/ 
install -d ${dest}/var/lib/asterisk/sounds/en/ ${dest}/var/lib/asterisk/moh/
fn=asterisk-core-sounds-en-gsm-current.tar.gz
if [ ! -f /srv/cache/sounds/\$fn ] ; then
	mkdir -p /srv/cache/sounds || :
	wget -q -O /srv/cache/sounds/\$fn http://downloads.asterisk.org/pub/telephony/sounds/\$fn
fi
tar -xzf /srv/cache/sounds/\$fn -C ${dest}/var/lib/asterisk/sounds/en/

echo Installing moh to ${dest}/var/lib/asterisk/moh/ 
fn=asterisk-moh-opsound-wav-current.tar.gz
if [ ! -f /srv/cache/sounds/\$fn ] ; then
	mkdir -p /srv/cache/sounds || :
	wget -q -O /srv/cache/sounds/\$fn http://downloads.asterisk.org/pub/telephony/sounds/\$fn
fi
tar -xzf /srv/cache/sounds/\$fn -C ${dest}/var/lib/asterisk/moh/

install --mode=0755 ${source}/usr/sbin/* ${dest}/usr/sbin/

"""
}
