def call() {
	def p = []
	for (e in manager.getEnvVars()) {
		if (e.key.startsWith("GERRIT")) {
			p.push(string(name: e.key, value: e.value))
		}
	}
	return p
}
