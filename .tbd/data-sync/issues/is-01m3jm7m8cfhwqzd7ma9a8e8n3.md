---
type: is
id: is-01m3jm7m8cfhwqzd7ma9a8e8n3
title: Add ktlint as a build-logic convention plugin
kind: task
status: closed
priority: 2
version: 6
delegate: claude-code@vm
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3jm8wrx99q66pvaq0x5e6d2
  - type: blocks
    target: is-01m3jm8x0j7b257b8s96c5ta8d
  - type: blocks
    target: is-01m3jm95saj2s5c0gh6d9vmgat
parent_id: is-01m3jm7b8yafz6jr653btzjykf
hold: null
hold_until: null
created_at: 2026-09-27T23:47:02.027Z
updated_at: 2026-09-28T11:24:12.415Z
started_at: 2026-09-28T11:10:19.825Z
closed_at: 2026-09-28T11:24:12.415Z
close_reason: Spotless 8.10.3 + ktlint 1.8.0 via build-logic's quintkonnect.ktlint (applied through quintkonnect.kotlin-jvm and by the root project, which also lints build-logic/ and example/); intellij_idea code style in .editorconfig; check wiring off until qk-lafs.
resolution: null
duplicate_of: null
---
Pick ktlint-gradle vs kotlinter-gradle vs Spotless+ktlint; verify the current stable version against Maven Central (don't trust the Gradle Plugin Portal page alone).

Apply it as a build-logic convention plugin (alongside quintkonnect.kotlin-jvm/.library/.ksp/.publish) across :annotations, :itf, :core, :ksp, :gradle-plugin, :integration-tests, build-logic itself, and *.gradle.kts files. Exclude KSP-generated sources (build/generated/ksp — KSP registers it as a source dir, so it gets linted by default otherwise). Keep it in build-logic, not in the published gradle-plugin module, so it doesn't ship to consumers (that would trigger the CLAUDE.md rule to sync skills/quint-konnect/). Leave example/ out unless cheap to include (see epic).

Most ktlint Gradle plugins wire ktlintCheck into the check task by default. Don't let that go live until the repo-wide reformat (next task) has landed, or CI goes red on existing code — either land this together with the reformat pass, or land it with the check-wiring disabled and flip it on afterward.
