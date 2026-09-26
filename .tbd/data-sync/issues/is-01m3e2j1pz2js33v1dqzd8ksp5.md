---
type: is
id: is-01m3e2j1pz2js33v1dqzd8ksp5
title: Package skill as Claude Code plugin and npx skills source
kind: feature
status: open
priority: 2
version: 2
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3e2j2kdtvex13knhqjv1703
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T05:21:11.391Z
updated_at: 2026-09-26T05:21:12.301Z
---
Root .claude-plugin/marketplace.json pointing to a plugin dir containing .claude-plugin/plugin.json (name, description, version tracking library version, license Apache-2.0, repository, keywords) and only the quint-konnect skill. Model on quint-llm-kit layout; verify current schema with ctx7 and the layout npx skills add expects. Verify /plugin marketplace add <local path> installs only quint-konnect, not ship-it/tbd.
