def call() {
	for (ve in params) {
		if (ve.key.startsWith("GERRIT")) {
			env[ve.key]=ve.value
		}
	}
}
