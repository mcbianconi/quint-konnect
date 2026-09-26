---
type: is
id: is-01m3e2j1pz2js33v1dqzd8ksp5
title: Package skill as Claude Code plugin and npx skills source
kind: feature
status: closed
priority: 2
version: 4
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3e2j2kdtvex13knhqjv1703
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T05:21:11.391Z
updated_at: 2026-09-26T20:21:07.075Z
closed_at: 2026-09-26T20:21:07.074Z
close_reason: "Root .claude-plugin/marketplace.json + plugin.json (source \"./\", skills: [\"./skills/quint-konnect\"]) package the existing skill without moving it. Verified with 'claude plugin validate .' and an isolated marketplace-add/install/details cycle (1 skill, 0 agents/hooks/MCP). Manual-only remainder: the interactive '/plugin marketplace add <path>' UX itself (the CLI-equivalent flow was exercised, not the interactive command). Also confirmed npx skills add still surfaces .claude/skills/* dev skills regardless of this packaging (recorded in docs/decisions/agent-skill-plugin.md)."
resolution: null
duplicate_of: null
---
Root .claude-plugin/marketplace.json pointing to a plugin dir containing .claude-plugin/plugin.json (name, description, version tracking library version, license Apache-2.0, repository, keywords) and only the quint-konnect skill. Model on quint-llm-kit layout; verify current schema with ctx7 and the layout npx skills add expects. Verify /plugin marketplace add <local path> installs only quint-konnect, not ship-it/tbd.
