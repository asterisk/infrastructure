import hudson.FilePath
import java.io.*

@NonCPS
def call(script)
{
	def scr = "echo 'Executing sudo on ${NODE_NAME}'\nset -ex\n" + script.stripIndent()

	def node = Jenkins.getInstance().getComputer(NODE_NAME).getNode()
	def fp = new FilePath(node.getChannel(), pwd());
	def l = node.createLauncher(manager.listener)
	l.decorateFor(node)
	
	def lps = l.launch()
	lps.cmds(["sudo", "-ns"])
	lps.pwd(fp)
	lps.readStdout()
	lps.readStderr()
	lps.stdin(new ByteArrayInputStream(scr.getBytes()))
	def process = lps.start()
	process.getStdout().eachLine { line, count -> 
		println "$count: $line";
	}
	process.join()
}
