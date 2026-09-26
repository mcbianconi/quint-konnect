---
name: gradle-plugin-distribution
date: 2026-09-26
---

`quint-konnect-gradle-plugin` (plugin id `io.github.mcbianconi.quint-konnect`) publishes to Maven
Central only, through the existing `quintkonnect.publish` convention plugin, with its plugin
marker artifact. It is not published to the Gradle Plugin Portal.

**Why:** The Gradle Plugin Portal needs its own account/publishing credentials, separate from the
Maven Central namespace already set up (qk-j02b). Publishing the marker to Maven Central lets
`plugins { id("io.github.mcbianconi.quint-konnect") version "..." }` resolve without a Portal
account, as long as the consumer's `settings.gradle.kts` lists `mavenCentral()` in
`pluginManagement.repositories` (`gradlePluginPortal()` stays too, for `com.google.devtools.ksp`
and any other Portal-only plugins the consumer applies directly).

**How to apply:** Don't add `com.gradle.plugin-publish` or Portal credentials without a new
decision here. README's plugin snippet shows the required `pluginManagement` block.
