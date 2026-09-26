---
type: is
id: is-01m3e18zty2vgv46764amj7cxp
title: "CI: move Node 20 to a supported LTS"
kind: chore
status: closed
priority: 3
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
created_at: 2026-09-26T04:58:45.981Z
updated_at: 2026-09-26T06:21:51.569Z
closed_at: 2026-09-26T06:21:51.568Z
close_reason: Moved ci.yml node-version from 20 to 24 (current Active LTS per nodejs/Release schedule as of 2026-09-26; Node 22 is now Maintenance LTS). Confirmed quint@0.32.0 declares engines.node >=18 via npm view. Committed as mry on docs-license-ci.
resolution: null
duplicate_of: null
---
.github/workflows/ci.yml uses node-version 20, which reached end of life in April 2026. Switch to 22 or 24 and confirm quint 0.32.0 installs on it.
