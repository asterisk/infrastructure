import globals

println "Creating views"

dashboardView(' Summary') {
	jobs {
		regex(/.*/)
	}
	columns {
		name()
		status()
		weather()
		testResult(1)
		lastBuildConsole()
		lastSuccess()
		lastFailure()
		lastDuration()
		progressBar()
	}
	configure { view ->
		view / topPortlets << "hudson.plugins.view.dashboard.core.JobsPortlet" {
			'id'('dashboard_portlet_44597')
			'name'(' ')
			'columnCount'('4')
			'fillColumnFirst'('true')
		}
	}
	topPortlets {
		jenkinsJobsList {
			displayName('Detail')
		}
	}
}

dashboardView('Checks') {
	jobs {
		regex(/check-.*/)
	}
	columns {
		name()
		status()
		weather()
		testResult(1)
		lastBuildConsole()
		lastSuccess()
		lastFailure()
		lastDuration()
		progressBar()
	}
	configure { view ->
		view / topPortlets << "hudson.plugins.view.dashboard.core.JobsPortlet" {
			'id'('dashboard_portlet_44590')
			'name'(' ')
			'columnCount'('4')
			'fillColumnFirst'('true')
		}
	}
	topPortlets {
		jenkinsJobsList {
			displayName('Detail')
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
		testResult(1)
		lastBuildConsole()
		lastSuccess()
		lastFailure()
		lastDuration()
		progressBar()
	}
	configure { view ->
		view / topPortlets << "hudson.plugins.view.dashboard.core.JobsPortlet" {
			'id'('dashboard_portlet_44591')
			'name'(' ')
			'columnCount'('4')
			'fillColumnFirst'('true')
		}
	}
	topPortlets {
		jenkinsJobsList {
			displayName('Detail')
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
		testResult(1)
		lastBuildConsole()
		lastSuccess()
		lastFailure()
		lastDuration()
		progressBar()
		buildButton()
	}
	configure { view ->
		view / topPortlets << "hudson.plugins.view.dashboard.core.JobsPortlet" {
			'id'('dashboard_portlet_44592')
			'name'(' ')
			'columnCount'('4')
			'fillColumnFirst'('true')
		}
	}
	topPortlets {
		jenkinsJobsList {
			displayName('Detail')
		}
	}
}

println "Creating asterisk check jobs"
for (br in globals.ast_branches) {
	for (arch in br.value.arches) {
		pipelineJob("check-ast-${br.key}-${arch}") {
			definition {
				cps {
					script("checkAsterisk('${br.key}', '${arch}')")
					sandbox(true)
				}
			}
			triggers {
				gerritTrigger {
					serverName(br.value.gerrit_trigger)
					silentMode(true)
					silentStartMode(true)
					gerritBuildFailedVerifiedValue(0)
					gerritBuildSuccessfulVerifiedValue(0)
					gerritBuildUnstableVerifiedValue(0)
					notificationLevel("NONE")
					triggerOnEvents {
						commentAddedContains { commentAddedCommentContains('^(recheck|reverify)$') }
						changeRestored()
						patchsetCreated {
							excludeDrafts(false)
							excludeTrivialRebase(false)
							excludeNoCodeChange(true)
						}
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
}

println "Creating testsuite check jobs"
pipelineJob("check-testsuite") {
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
				changeRestored()
				patchsetCreated {
					excludeDrafts(false)
					excludeTrivialRebase(false)
					excludeNoCodeChange(true)
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
pipelineJob("check-testsuite-pep8") {
	definition {
		cps {
			script("""
					checkTestsuitePEP8()
				""".stripIndent())
			sandbox(false)
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
				changeRestored()
				patchsetCreated {
					excludeDrafts(false)
					excludeTrivialRebase(false)
					excludeNoCodeChange(true)
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

println "Creating asterisk gate jobs"
for (br in globals.ast_branches) {
	for (gt in br.value.gate_types) {
		pipelineJob("gate-ast-${br.key}-${gt}") {
			definition {
				cps {
					script("gateAsterisk('${br.key}', '${gt}')")
					sandbox(true)
				}
			}
			triggers {
				gerritTrigger {
					serverName(br.value.gerrit_trigger)
					silentMode(true)
					silentStartMode(true)
					gerritBuildFailedVerifiedValue(0)
					gerritBuildSuccessfulVerifiedValue(0)
					gerritBuildUnstableVerifiedValue(0)
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
}

println "Creating asterisk periodic jobs"
for (br in globals.ast_branches) {
	for (pt in br.value.periodic_types) {
		pipelineJob("periodic-ast-${br.key}-${pt}") {
			triggers {
				cron('H 1 * * *')
			}
			definition {
				cps {
					script("periodicAsterisk('${br.key}', '${pt}')")
					sandbox(true)
				}
			}
		}
	}
}
