---
type: is
id: is-01m3hknhqg0jpr477qbhg644ep
title: Add git-cliff based changelog generation to release workflow
kind: task
status: open
priority: 3
version: 1
labels: []
dependencies: []
created_at: 2026-09-27T14:17:55.183Z
updated_at: 2026-09-27T14:17:55.183Z
---
gh release create --generate-notes produces only a Full Changelog link because this repo has zero merged PRs (GitButler direct-commit workflow); GitHub's auto-notes feature is PR-based and has nothing to categorize. Commits already follow the type(scope): summary convention consistently, so a conventional-commit changelog generator would work with no restructuring.

Scope: install git-cliff, add a cliff.toml, wire it into .github/workflows/release.yml (or a separate step) to produce a categorized CHANGELOG (Features/Fixes/etc.) from conventional commits, and feed it to gh release create --notes-file instead of --generate-notes.

No CHANGELOG.md or cliff.toml exists yet.
