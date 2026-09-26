---
type: is
id: is-01m3dzh3h36exkjke6fkw32cph
title: Publish artifacts to Maven Central
kind: feature
status: open
priority: 1
version: 4
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3e2j2kdtvex13knhqjv1703
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
created_at: 2026-09-26T04:28:14.754Z
updated_at: 2026-09-26T06:46:20.526Z
---
No maven-publish, POM or signing; README tells users to depend on project(':core'). Publish annotations, core and ksp (maven-publish + signing or vanniktech plugin) and update README dependency snippet.

## Notes

Secrets available in CI: MAVEN_CENTRAL_USERNAME, MAVEN_CENTRAL_PASSWORD, SIGNING_KEY (ASCII-armored private key), SIGNING_KEY_ID (9C52228F), SIGNING_KEY_PASSWORD.

Publish annotations, itf (artifact itf-kotlin, package io.github.mcbianconi.itf, see qk-km9v), core and ksp. README dependency snippet: users add core and ksp; itf-kotlin comes transitively via core's api dependency.
