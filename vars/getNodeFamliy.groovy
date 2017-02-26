@NonCPS
def call(node_name) {
	for (s in hudson.model.Hudson.instance.slaves) {
		if (s.getNodeName() != node_name) {
			continue
		}
  		for (l in s.getAssignedLabels()) {
          if (l.getName().startsWith('family:')) {
          	return l
          }
        }
	}
	return ""
}
