---
type: is
id: is-01m3j42hwqa45ycrv1bjvwyjhe
title: Update gradle-plugin ABI dump for GenerateQuintTracesTask
kind: bug
status: closed
priority: 1
version: 3
delegate: claude-code@macmurillo.local
labels:
  - roadmap
dependencies: []
parent_id: is-01m3e5a9sj92e7ptv02j419tm8
hold: null
hold_until: null
created_at: 2026-09-27T19:04:38.550Z
updated_at: 2026-09-27T19:33:48.507Z
started_at: 2026-09-27T19:33:24.168Z
closed_at: 2026-09-27T19:33:48.506Z
close_reason: Commit vny on trace-task adds GenerateQuintTracesTask to gradle-plugin.api.
resolution: null
duplicate_of: null
---
Branch trace-task (PR #2, qk-adm2) adds the public GenerateQuintTracesTask without regenerating gradle-plugin/api/gradle-plugin.api, so :gradle-plugin:checkKotlinAbi fails (CI red on PR #2, and local 'build' fails with all branches applied). Fix: ./gradlew :gradle-plugin:updateKotlinAbi on trace-task and commit the dump there.
