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
	topPortlets {
		jobsPortlet {
			name('Jobs')
			columnCount(4)
			fillColumnFirst(true)
		}
		jenkinsJobsList {
			displayName('Detail')
		}
	}
	bottomPortlets {
		buildStatistics {
			displayName('Build Stats')
		}
		latestBuilds {
			name('Latest Builds')
			numBuilds(10)
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
	topPortlets {
		jobsPortlet {
			name('Jobs')
			columnCount(4)
			fillColumnFirst(true)
		}
		jenkinsJobsList {
			displayName('Detail')
		}
	}
	bottomPortlets {
		buildStatistics {
			displayName('Build Stats')
		}
		latestBuilds {
			name('Latest Builds')
			numBuilds(10)
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
	topPortlets {
		jobsPortlet {
			name('Jobs')
			columnCount(3)
			fillColumnFirst(true)
		}
		jenkinsJobsList {
			displayName('Detail')
		}
	}
	bottomPortlets {
		buildStatistics {
			displayName('Build Stats')
		}
		latestBuilds {
			name('Latest Builds')
			numBuilds(10)
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
	topPortlets {
		jobsPortlet {
			name('Jobs')
			columnCount(3)
			fillColumnFirst(true)
		}
		jenkinsJobsList {
			displayName('Detail')
		}
	}
	bottomPortlets {
		buildStatistics {
			displayName('Build Stats')
		}
		latestBuilds {
			name('Latest Builds')
			numBuilds(10)
		}
	}
}

println "Creating asterisk check jobs"
for (br in globals.ast_branches) {
	for (arch in br.value.arches) {
		pipelineJob("check-ast-${br.key}-${arch}") {
			definition {
				cps {
					script("""\
						manager.build.displayName = "\${env.GERRIT_CHANGE_NUMBER}"
						timestamps() {
							node ('job:check && bits:${arch}') {
								checkAsterisk('${br.key}', '${arch}')
							}
						}""")
					sandbox(false)
				}
			}
			triggers {
				gerritTrigger {
					serverName(br.value.gerrit_trigger)
					silentMode(false)
					silentStartMode(true)
					gerritBuildFailedVerifiedValue(-1)
					gerritBuildSuccessfulVerifiedValue(1)
					gerritBuildUnstableVerifiedValue(-1)
					notificationLevel("OWNER_REVIEWERS")
					triggerOnEvents {
						commentAddedContains { commentAddedCommentContains('^Patch Set [0-9]+:..recheck$') }
						changeRestored()
						draftPublished()
						patchsetCreated {
							excludeDrafts(false)
							excludeTrivialRebase(false)
							excludeNoCodeChange(false)
						}
					}
					gerritProjects {
						gerritProject {
							compareType("REG_EXP")
							pattern('^asterisk$')
							branches {
								branch {
									compareType("REG_EXP")
									pattern("(certified/)?${br.key}(.[0-9]+)?")
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
for (br in globals.testsuite_branches) {
	for (arch in br.value.arches) {
		pipelineJob("check-testsuite-${br.key}-${arch}") {
			definition {
				cps {
					script("""\
						manager.build.displayName = "\${env.GERRIT_CHANGE_NUMBER}"
						timestamps() {
							node ('job:check && bits:${arch}') {
								checkTestsuite('${br.key}', '${arch}')
							}
						}""")
					sandbox(false)
				}
			}
			triggers {
				gerritTrigger {
					serverName(br.value.gerrit_trigger)
					silentMode(false)
					silentStartMode(true)
					gerritBuildFailedVerifiedValue(-1)
					gerritBuildSuccessfulVerifiedValue(1)
					gerritBuildUnstableVerifiedValue(-1)
					notificationLevel("OWNER_REVIEWERS")
					triggerOnEvents {
						commentAddedContains { commentAddedCommentContains('^Patch Set [0-9]+:..recheck$') }
						changeRestored()
						draftPublished()
						patchsetCreated {
							excludeDrafts(false)
							excludeTrivialRebase(false)
							excludeNoCodeChange(false)
						}
					}
					gerritProjects {
						gerritProject {
							compareType("REG_EXP")
							pattern('^testsuite$')
							branches {
								branch {
									compareType("REG_EXP")
									pattern("${br.key}(.[0-9]+)?")
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

pipelineJob("check-testsuite-pep8") {
	definition {
		cps {
			script("""\
				manager.build.displayName = "\${env.GERRIT_CHANGE_NUMBER}"
				timestamps() {
					node ('job:check') {
						checkTestsuitePEP8()
					}
				}""")
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
					script("""\
						manager.build.displayName = "\${env.GERRIT_CHANGE_NUMBER}"
						timestamps() {
							node ('job:gate') {
								gateAsterisk('${br.key}', '${gt}')
							}
						}""")
					sandbox(false)
				}
			}
			triggers {
				gerritTrigger {
					serverName(br.value.gerrit_trigger)
					silentMode(false)
					silentStartMode(true)
					gerritBuildFailedVerifiedValue(-1)
					gerritBuildSuccessfulVerifiedValue(2)
					gerritBuildUnstableVerifiedValue(-1)
					notificationLevel("OWNER_REVIEWERS")
					triggerOnEvents {
						commentAdded {
							verdictCategory("CodeReview")
							commentAddedTriggerApprovalValue("+2")
						}
						commentAddedContains { commentAddedCommentContains('^Patch Set [0-9]+: Code-Review[+]2$') }
						commentAddedContains { commentAddedCommentContains('^Patch Set [0-9]+:..regate$') }
					}
					gerritProjects {
						gerritProject {
							compareType("REG_EXP")
							pattern('^asterisk$')
							branches {
								branch {
									compareType("PLAIN")
									pattern(br.key)
								}
								branch {
									compareType("REG_EXP")
									pattern("(certified/)?${br.key}(.[0-9]+)?")
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

println "Creating testsuite gate job"
pipelineJob("gate-testsuite") {
	definition {
		cps {
			script("""\
				manager.build.displayName = "\${env.GERRIT_CHANGE_NUMBER}"
			""")
			sandbox(false)
		}
	}
	triggers {
		gerritTrigger {
			serverName(globals.testsuite_branches.master.gerrit_trigger)
			silentMode(false)
			silentStartMode(true)
			gerritBuildFailedVerifiedValue(-1)
			gerritBuildSuccessfulVerifiedValue(2)
			gerritBuildUnstableVerifiedValue(-1)
			notificationLevel("OWNER_REVIEWERS")
			triggerOnEvents {
				commentAdded {
					verdictCategory("CodeReview")
					commentAddedTriggerApprovalValue("+2")
				}
				commentAddedContains { commentAddedCommentContains('^Patch Set [0-9]+: Code-Review[+]2$') }
				commentAddedContains { commentAddedCommentContains('^Patch Set [0-9]+:..regate$') }
			}
			gerritProjects {
				gerritProject {
					compareType("REG_EXP")
					pattern('^testsuite$')
					branches {
						branch {
							compareType("REG_EXP")
							pattern("(master|13|14|15)")
						}
					}
					disableStrictForbiddenFileVerification(false)
				}
			}
		}
	}
}

