
class globals {

	def static basic_build_options = "-v -e DO_CRASH"

	def static default_build_options = "-e app_voicemail -e app_directory -e FILE_STORAGE"

	def static test_options = [
		unittst: [
			build_options: default_build_options
		],
		ari: [
			include_tests: ["tests/rest_api"],
			build_options: default_build_options
		],
		pjsip: [
			include_tests: ["tests/channels/pjsip"],
			build_options: default_build_options
		],
		sip: [
			include_tests: ["tests/channels/SIP"],
			build_options: default_build_options
		],
		iax2_local: [
			include_tests: ["tests/channels/iax2", "tests/channels/local"],
			build_options: default_build_options
		],
		extmwi: [
			include_tests: ["tests/channels/pjsip/publish", "tests/channels/pjsip/subscriptions", "tests/realtime"],
			include_tags: ["mwi_external"],
			build_options: "${basic_build_options} -d app_voicemail -e res_mwi_external -e res_mwi_external_ami -e res_stasis_mailbox"
		],
		other: [
			exclude_tests: ["tests/channels", "tests/realtime", "tests/rest_api"],
			build_options: default_build_options
		],
		doc: [
			build_options: default_build_options
		],
		realtime: [
			include_tests: ["tests/channels/pjsip"],
			exclude_tags: ["realtime-incompatible"],
			build_options: default_build_options,
			db: [
				user: "asterisk",
				host: "localhost",
				dbname: "asterisk",
				dsn: "asterisk-connector"
			]
		]
	]
	
	def static ast_branches = [
		'13': [
			build_options: "${basic_build_options} -e codec_silk",
			gate_types: [
				"pjsip",
				"sip",
				"iax2_local",
				"extmwi",
				"ari"
			],
			periodic_types: [
				"unittst",
				"pjsip",
				"sip",
				"iax2_local",
				"ari",
				"extmwi",
				"other",
				"realtime",
				"doc"
			],
			arches: [ "32", "64" ],
			gerrit_trigger: "Gerrit Public"
		],
		'14': [
			build_options: "${basic_build_options} -e codec_silk -e app_statsd",
			gate_types: [
				"pjsip",
				"sip",
				"iax2_local",
				"extmwi",
				"ari"
			],
			periodic_types: [
				"unittst",
				"pjsip",
				"sip",
				"iax2_local",
				"ari",
				"extmwi",
				"other",
				"realtime",
				"doc"
			],
			arches: [ "32", "64" ],
			gerrit_trigger: "Gerrit Public"
		],
		'15': [
			build_options: "${basic_build_options} -e codec_silk -e app_statsd",
			gate_types: [
				"pjsip",
				"sip",
				"iax2_local",
				"extmwi",
				"ari"
			],
			periodic_types: [
				"unittst",
				"pjsip",
				"sip",
				"iax2_local",
				"ari",
				"extmwi",
				"other",
				"realtime",
				"doc"
			],
			arches: [ "32", "64" ],
			gerrit_trigger: "Gerrit Public"
		],
		'master': [
			build_options: "${basic_build_options} -e codec_silk -e app_statsd",
			gate_types: [
				"pjsip",
				"sip",
				"iax2_local",
				"extmwi",
				"ari"
			],
			periodic_types: [
				"unittst",
				"pjsip",
				"sip",
				"iax2_local",
				"ari",
				"extmwi",
				"other",
				"realtime"
			],
			arches: [ "32", "64" ],
			gerrit_trigger: "Gerrit Public"
		]
	]

	def static testsuite_branches = [
		'13': [
			arches: [ "32", "64" ],
			gerrit_trigger: "Gerrit Public"
		],
		'14': [
			arches: [ "32", "64" ],
			gerrit_trigger: "Gerrit Public"
		],
		'15': [
			arches: [ "32", "64" ],
			gerrit_trigger: "Gerrit Public"
		],
		'master': [
			arches: [ "32", "64" ],
			gerrit_trigger: "Gerrit Public"
		]
	]
}