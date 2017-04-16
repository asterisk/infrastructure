def call(branch) {
	stage("checkout-astxml2wiki") {
		checkout changelog: false, poll: false, scm: [
			$class: "GitSCM",
			branches: [[name: "master"]],
			doGenerateSubmoduleConfigurations: false,
			extensions: [
				[$class: "RelativeTargetDirectory", relativeTargetDir: "astxml2wiki"],
				[$class: "CleanBeforeCheckout"],
				[$class: "CloneOption", shallow: true]
			],
			submoduleCfg: [],
			userRemoteConfigs: [
				[url: "https://github.com/asterisk/publish-docs.git"]]
		]
	}

	checkoutProjectMirror("asterisk", branch, "astxml2wiki/asterisk")

	stage("publish-to-wiki") {
		dir("astxml2wiki/asterisk") { sh "../publish.sh ${branch}" }
	}
}