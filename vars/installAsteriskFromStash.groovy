import globals

def call(stashname, source, dest)
{
	sh """\
		sudo rm -rf ${source} || : "
		sudo rm -rf asterisk || : "
	""".stripIndent()
	unstash stashname
	sh """\
echo Installing samples to ${dest}/etc/asterisk 
sudo rm -rf ${dest}/etc/asterisk >/dev/null 2>&1 || :
sudo cp -a ${source}/etc/asterisk ${dest}/etc/

echo Cleaning ${dest}/usr/lib{,64} 
sudo rm -rf ${dest}/usr/lib/asterisk >/dev/null 2>&1 || :
sudo rm -rf ${dest}/usr/lib64/asterisk >/dev/null 2>&1 || :
sudo rm -rf ${dest}/usr/lib/libasterisk* >/dev/null 2>&1 || :
sudo rm -rf ${dest}/usr/lib64/libasterisk* >/dev/null 2>&1 || :

destlib="lib"
if [ -d ${dest}/usr/lib64 -a `uname -m` = 'x86_64' ] ; then
   destlib="lib64"
fi

echo Installing shared libraries toCleaning ${dest}/usr/\$destlib/ 
sudo cp -a ${source}/usr/lib/asterisk ${dest}/usr/\$destlib/
sudo cp -a ${source}/usr/lib/libasterisk* ${dest}/usr/\$destlib/
sudo ldconfig ${dest}/usr/\$destlib

echo Installing ${source}/var/lib/asterisk ${dest}/var/lib/ 
sudo rm -rf ${dest}/var/lib/asterisk >/dev/null 2>&1 || :
sudo cp -a ${source}/var/lib/asterisk ${dest}/var/lib/

echo Installing ${source}/var/spool/asterisk ${dest}/var/spool/ 
sudo rm -rf ${dest}/var/spool/asterisk >/dev/null 2>&1 || :
sudo cp -a ${source}/var/spool/asterisk ${dest}/var/spool/

for bin in ${source}/usr/sbin/* ; do
   bn=`basename \${bin}`
   sudo rm ${dest}/usr/sbin/\${bn} >/dev/null 2>&1 || :
   echo Installing \${bn} to ${dest}/usr/sbin/
   sudo cp -a \${bin} ${dest}/usr/sbin/
done
	""".stripIndent()
}
