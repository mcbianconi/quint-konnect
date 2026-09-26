---
type: is
id: is-01m3dzgw2q6c71wttv20e675x7
title: Gradle plugin for setup, spec paths and quint version check
kind: feature
status: closed
priority: 2
version: 6
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3e5abnwyqq8tajfbmjbjp6g
  - type: blocks
    target: is-01m3e5ad9x9mj8v135bn9vypym
  - type: blocks
    target: is-01m3e5adhcxqj4qjc9x4emcrmm
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T04:28:07.127Z
updated_at: 2026-09-26T15:56:41.000Z
closed_at: 2026-09-26T15:56:40.999Z
close_reason: Added :gradle-plugin (io.github.mcbianconi.quint-konnect), core spec-path resolution, docs. Commits qmq/twl on branch gradle-plugin. Full build green (208 tests), publishToMavenLocal verified with plugin marker.
resolution: null
duplicate_of: null
---
Apply KSP and dependencies, resolve spec paths relative to the project (today relative to working dir, which breaks some IDE runs), check quint is installed and a supported version.
