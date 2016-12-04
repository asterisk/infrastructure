@NonCPS
def call(trigger_name) {
	def plugin = Jenkins.instance.pluginManager.getPlugin('gerrit-trigger')
	def sl = new XmlSlurper().parseText((plugin.getPlugin().getConfigXml().asString()))
	def server = sl.servers.'*'.find { s->
		s.name == trigger_name
	}
	return server.config.gerritFrontEndUrl.text()
}
