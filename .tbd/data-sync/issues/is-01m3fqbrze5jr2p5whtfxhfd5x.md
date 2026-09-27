---
type: is
id: is-01m3fqbrze5jr2p5whtfxhfd5x
title: Gradle plugin does not configure testLogging (exceptionFormat/showStandardStreams)
kind: feature
status: closed
priority: 2
version: 3
labels: []
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T20:44:00.366Z
updated_at: 2026-09-27T13:41:02.612Z
closed_at: 2026-09-27T13:41:02.611Z
close_reason: "Fixed: gradle-plugin configures testLogging (FULL/showStandardStreams/FAILED) by default, opt-out via quintKonnect.configureTestLogging"
resolution: null
duplicate_of: null
---
Applying the quint-konnect Gradle plugin does not set tasks.test { testLogging { exceptionFormat = FULL; showStandardStreams = true } }. Gradle's own defaults hide two things on a quint-konnect test failure: exceptionFormat=SHORT truncates the console to 'java.lang.AssertionError at File.kt:19' (the trace/step/action/diff message is invisible), and stderr is hidden by default (QUINT_VERBOSE output and the 'Reproduce this error with QUINT_SEED=...' line both go to stderr). A user only sees the full failure by reading build/test-results/test/*.xml or adding the testLogging block themselves -- both non-obvious. Found while eval'ing the quint-konnect skill on a scratch project (bead qk-graf), where the console showed only the bare exception class+location on the negative-case failure. Consider having the plugin configure testLogging with exceptionFormat=FULL and showStandardStreams=true by default (possibly toggleable), since a user applying the plugin has opted into quint-konnect's failure-reporting style already. Doc workaround added to skills/quint-konnect/references/gradle-setup.md and SKILL.md in the meantime.
