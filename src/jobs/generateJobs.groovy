import globals

println "Creating views"
dashboardView('Checks') {
	jobs {
		regex(/check-.*/)
	}
	columns {
		name()
		status()
		weather()
		lastSuccess()
		lastFailure()
		lastDuration()
		lastBuildNode()
		progressBar()
	}
	topPortlets {
		jenkinsJobsList {
			displayName('Checks')
		}
	}
}

dashboardView('Gates') {
	jobs {
		regex(/gate-.*/)
	}
	columns {
		name()
		status()
		weather()
		lastSuccess()
		lastFailure()
		lastDuration()
		lastBuildNode()
		progressBar()
	}
	topPortlets {
		jenkinsJobsList {
			displayName('Gates')
		}
	}
}

dashboardView('Periodics') {
	jobs {
		regex(/periodic-.*/)
	}
	columns {
		name()
		status()
		weather()
		lastSuccess()
		lastFailure()
		lastDuration()
		lastBuildNode()
		progressBar()
	}
	topPortlets {
		jenkinsJobsList {
			displayName('Periodics')
		}
	}
}

println "Creating asterisk check jobs"
for (br in globals.ast_branches) {
	pipelineJob("check-ast-${br.key}") {
		definition {
			cps {
				script("checkAsteriskControl()")
				sandbox(true)
			}
		}
		parameters {
			stringParam('PARENT_BUILD_ID', '0', 'Must be 0 for control job')
			stringParam('BRANCH', br.key, 'Branch')
		}
		triggers {
			gerritTrigger {
				serverName(br.value.gerrit_trigger)
				silentMode(true)
				silentStartMode(true)
				gerritBuildFailedVerifiedValue(-1)
				gerritBuildSuccessfulVerifiedValue(1)
				gerritBuildUnstableVerifiedValue(-1)
				notificationLevel("NONE")
				triggerOnEvents {
					commentAddedContains { commentAddedCommentContains('^(recheck|reverify)$') }
				}
				gerritProjects {
					gerritProject {
						compareType("PLAIN")
						pattern("asterisk")
						branches {
							branch {
								compareType("PLAIN")
								pattern(br.key)
							}
							branch {
								compareType("REG_EXP")
								pattern("certified/${br.key}")
							}
						}
						disableStrictForbiddenFileVerification(false)
					}
				}
			}
		}
	}
}

println "Creating testsuite check jobs"
pipelineJob("check-tst") {
	definition {
		cps {
			script("checkTestsuite()")
			sandbox(true)
		}
	}
	triggers {
		gerritTrigger {
			serverName(globals.testsuite.gerrit_trigger)
			silentMode(true)
			silentStartMode(true)
			gerritBuildFailedVerifiedValue(-1)
			gerritBuildSuccessfulVerifiedValue(1)
			gerritBuildUnstableVerifiedValue(-1)
			notificationLevel("NONE")
			triggerOnEvents {
				draftPublished()
				changeRestored()
				patchsetCreated {
					excludeDrafts(false)
					excludeNoCodeChange(true)
					excludeTrivialRebase(false)
				}
				commentAddedContains { commentAddedCommentContains('^(recheck|reverify)$') }
			}
			gerritProjects {
				gerritProject {
					compareType("PLAIN")
					pattern("testsuite")
					branches {
						branch {
							compareType("PLAIN")
							pattern("master")
						}
					}
					disableStrictForbiddenFileVerification(false)
				}
			}
		}
	}
}
pipelineJob("check-tst-pep8") {
	definition {
		cps {
			script("""
					checkTestsuitePEP8()
				""".stripIndent())
			sandbox(false)
		}
	}
}

println "Creating asterisk gate jobs"
for (br in globals.ast_branches) {
	for (gt in br.value.gate_types) {
		pipelineJob("gate-ast-${br.key}-${gt}") {
			definition {
				cps {
					script("gateAsteriskControl()")
					sandbox(true)
				}
			}
			parameters {
				stringParam('PARENT_BUILD_ID', '0', 'Must be 0 for control job')
				stringParam('BRANCH', br.key, 'Branch')
			}
		}
	}
	pipelineJob("gate-ast-${br.key}") {
		definition {
			cps {
				script("gateAsteriskControl()")
				sandbox(true)
			}
		}
		parameters {
			stringParam('PARENT_BUILD_ID', '0', 'Must be 0 for control job')
			stringParam('BRANCH', br.key, 'Branch')
		}
		triggers {
			gerritTrigger {
				serverName(br.value.gerrit_trigger)
				silentMode(true)
				silentStartMode(true)
				gerritBuildFailedVerifiedValue(-1)
				gerritBuildSuccessfulVerifiedValue(2)
				gerritBuildUnstableVerifiedValue(-1)
				notificationLevel("NONE")
				triggerOnEvents {
					draftPublished()
					changeRestored()
					patchsetCreated {
						excludeDrafts(false)
						excludeNoCodeChange(true)
						excludeTrivialRebase(false)
					}
					commentAdded {
						verdictCategory("CodeReview")
						commentAddedTriggerApprovalValue("2")
					}
					commentAddedContains { commentAddedCommentContains('^regate$') }
				}
				gerritProjects {
					gerritProject {
						compareType("PLAIN")
						pattern("asterisk")
						branches {
							branch {
								compareType("PLAIN")
								pattern(br.key)
							}
							branch {
								compareType("REG_EXP")
								pattern("certified/${br.key}")
							}
						}
						disableStrictForbiddenFileVerification(false)
					}
				}
			}
		}
	}
}

println "Creating asterisk periodic jobs"
for (br in globals.ast_branches) {
	for (pt in br.value.periodic_types) {
		pipelineJob("periodic-ast-${br.key}-${pt}") {
			definition {
				cps {
					script("periodicAsterisk(\"${br.key}\", \"${pt}\")")
					sandbox(true)
				}
			}
		}
	}
}

