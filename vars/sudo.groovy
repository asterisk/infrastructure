def call(script)
{
	def scr = "set -e\n" + script.stripIndent()
	long number = (long) Math.floor(Math.random() * 9000000000000000L) + 1000000000000000L
	def fn = "sudo-${number}.sh"
	writeFile file: fn, text: scr
	sh "sudo env bash ${fn}"
	sh "sudo rm ${fn} >/dev/null 2>&1 || :" 
}
