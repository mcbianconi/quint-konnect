---
name: agent-skill-plugin
date: 2026-09-26
---

The user-facing skill at `skills/quint-konnect/` is also packaged as a Claude Code plugin,
rooted at the repository root: `.claude-plugin/marketplace.json` has one plugin entry with
`"source": "./"` and `"skills": ["./skills/quint-konnect"]`, and `.claude-plugin/plugin.json`
sits alongside it in the same directory. The skill itself is not moved.

**Why:** Every Claude Code plugin path field (`skills`, `commands`, `agents`, ...) must start
with `./` and stay inside the plugin's own root — a path that escapes it (e.g. `../skills/...`
from a `plugins/quint-konnect/` subdirectory back up to the repo's `skills/`) is rejected with
"path escapes plugin directory". Pointing the plugin's `source` at the repository root instead
of a nested `plugins/` directory keeps the plugin definition close to the skill without moving
it, at the cost of the repo root being the plugin root (so a future root-level `commands/`,
`agents/`, `hooks/`, or `.mcp.json` would also load as part of this plugin — check for that
before adding one). `"skills": ["./skills/quint-konnect"]` is required, not cosmetic: for a
marketplace entry whose `source` resolves to the marketplace root, an explicit skill list
replaces the default `skills/` scan, so this is what keeps the plugin to exactly one skill
(there's only one skill under `skills/` today, but this survives a second one being added
later without also being pulled into this plugin).

Verified with `claude plugin validate .` (passes) and an isolated install
(`CLAUDE_CONFIG_DIR=<scratch> claude plugin marketplace add .` +
`claude plugin install quint-konnect@quint-konnect -y` +
`claude plugin details quint-konnect`): the component inventory shows exactly one skill, zero
agents/hooks/MCP servers — the dev skills under `../../.claude/skills/` (`ship-it`, `tbd`,
`work-epic`) are not reachable through the plugin, because Claude Code's plugin loader only
scans the plugin root's own `skills/` (and any explicit `skills` paths), never `../../.claude/skills/`.

**`npx skills` is a separate, wider net.** The `skills` npm package (`npx skills add`) does not
read `plugin.json`/`marketplace.json` to *restrict* discovery — it only uses them to *add*
extra search directories. Its own hardcoded directory list (`AGENT_PROJECT_SKILL_DIRS`)
includes `../../.claude/skills`, scanned to depth 3, regardless of any plugin manifest. Confirmed with
`npx skills add . --list` from the repo root: it finds all four SKILL.md files (`quint-konnect`,
`ship-it`, `tbd`, `work-epic`), not just the published one. There is no marketplace/plugin-level
fix for this; the only exclusion mechanism in that CLI is `metadata: { internal: true }` in a
given `SKILL.md`'s own frontmatter, which is the dev skills' own file to set, not this plugin's.

**How to apply:**
- Don't add a root `commands/`, `agents/`, `hooks/` dir, `.mcp.json`, or other default
  plugin-component location without checking whether it should also ship as part of this
  plugin (it will, once it exists at the repo root).
- `plugin.json`'s `version` is the single source of truth and must equal the library version
  (`build-logic/src/main/kotlin/quintkonnect.kotlin-jvm.gradle.kts`'s `version`); bump both in
  the same commit as part of the existing release steps (AGENTS.md's "Releasing" section).
  Don't also set `version` on the marketplace entry — `claude plugin validate` warns on a
  mismatch, and `plugin.json` wins over it anyway.
- Point users at `npx skills add mcbianconi/quint-konnect --skill quint-konnect` (or the
  `.../tree/main/skills/quint-konnect` URL form), not a bare `npx skills add
  mcbianconi/quint-konnect`, until/unless the dev skills are marked `internal` in their own
  frontmatter.
- `/plugin marketplace add <local path>` itself (the interactive command, as opposed to the
  `claude plugin marketplace add`/`install` CLI pair used above) still wants a manual check by
  the owner in an interactive session.
