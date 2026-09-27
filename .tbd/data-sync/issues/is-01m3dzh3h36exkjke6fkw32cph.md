---
type: is
id: is-01m3dzh3h36exkjke6fkw32cph
title: Publish artifacts to Maven Central
kind: feature
status: closed
priority: 1
version: 7
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3e2j2kdtvex13knhqjv1703
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
created_at: 2026-09-26T04:28:14.754Z
updated_at: 2026-09-27T13:42:38.091Z
closed_at: 2026-09-27T13:42:38.090Z
close_reason: "Released v0.1.0: release.yml run 36323123812 uploaded annotations, itf-kotlin, core, ksp, gradle-plugin + plugin marker; Central deployment 4598a563-5f9c-40f1-a72c-63e09a1224b8 validated and publishing."
resolution: null
duplicate_of: null
---
No maven-publish, POM or signing; README tells users to depend on project(':core'). Publish annotations, core and ksp (maven-publish + signing or vanniktech plugin) and update README dependency snippet.

## Notes

Secrets available in CI: MAVEN_CENTRAL_USERNAME, MAVEN_CENTRAL_PASSWORD, SIGNING_KEY (ASCII-armored private key), SIGNING_KEY_ID (9C52228F), SIGNING_KEY_PASSWORD.

Publish annotations, itf (artifact itf-kotlin, package io.github.mcbianconi.itf, see qk-km9v), core and ksp. README dependency snippet: users add core and ksp; itf-kotlin comes transitively via core's api dependency.

Configured on branch maven-central-publish; release by pushing tag v0.1.0.
