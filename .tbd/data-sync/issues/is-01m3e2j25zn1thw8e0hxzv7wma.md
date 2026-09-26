---
type: is
id: is-01m3e2j25zn1thw8e0hxzv7wma
title: "Maven Central account setup: namespace and signing key"
kind: chore
status: closed
priority: 1
version: 4
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3dzh3h36exkjke6fkw32cph
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
created_at: 2026-09-26T05:21:11.870Z
updated_at: 2026-09-26T05:50:37.404Z
closed_at: 2026-09-26T05:50:37.403Z
close_reason: "Namespace io.github.mcbianconi verified; user token created; signing key 9C52228F published (ubuntu + openpgp.org, email verified). GitHub secrets: MAVEN_CENTRAL_USERNAME, MAVEN_CENTRAL_PASSWORD, SIGNING_KEY (armored), SIGNING_KEY_ID, SIGNING_KEY_PASSWORD."
resolution: null
duplicate_of: null
---
Verify io.github.mcbianconi namespace on central.sonatype.com, create a publishing token, create a GPG signing key and publish it to a keyserver, and store credentials as CI secrets. Needs the user; blocks qk-j02b.

## Notes

2026-09-26: dedicated signing key generated: rsa4096 sign-only, expires 2029-09-25, fpr 9087426732C4E9745E4EB1DC2620CDAC9C52228F (short 9C52228F). Sent to keyserver.ubuntu.com and keys.openpgp.org (email verification requested). Revocation cert in ~/.gnupg/openpgp-revocs.d/. Remaining: namespace verify, user token, CI secrets.
