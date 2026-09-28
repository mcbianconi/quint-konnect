---
type: is
id: is-01m3jm7b8yafz6jr653btzjykf
title: "[epic] Kotlin lint/format tooling: local hook and CI gate"
kind: epic
status: open
priority: 2
version: 8
labels:
  - roadmap
dependencies: []
child_order_hints:
  - is-01m3jm7m8cfhwqzd7ma9a8e8n3
  - is-01m3jm8wrx99q66pvaq0x5e6d2
  - is-01m3jm8x0j7b257b8s96c5ta8d
  - is-01m3jm95saj2s5c0gh6d9vmgat
  - is-01m3jm960y58d5swkkv3aw8e9p
created_at: 2026-09-27T23:46:52.829Z
updated_at: 2026-09-27T23:48:05.749Z
---
Add ktlint as the linter/formatter for all Kotlin and Gradle Kotlin DSL sources, enforce it locally via a git hook, and gate CI on it failing the build on violations.

Default candidate: org.jlleitschuh.gradle.ktlint (wraps pinterest ktlint; ships ktlintCheck/ktlintFormat tasks and addKtlintCheckGitPreCommitHook/addKtlintFormatGitPreCommitHook installers). kotlinter-gradle and Spotless+ktlint are alternatives to weigh in the first child task; confirm the current stable version against Maven Central rather than trusting the Gradle Plugin Portal page.

Repo-specific wrinkle: commits here go through GitButler (but commit), which does not appear to invoke git's pre-commit hook — only 'but push'/'but pr new' run pre-push hooks (skippable with --no-hooks/--no-verify). The effective local gate in this workflow may need to live on pre-push, not pre-commit, even though that's the industry-standard term used in the epic title. Verify before wiring the hook (see child task).

example/ is a separate composite Gradle build (qk-hxzt, built via './gradlew -p example build') and is not reached by a build-logic convention plugin; it's out of scope unless a child task finds it cheap to include.
