// ktlint through Spotless: https://github.com/diffplug/spotless/tree/main/plugin-gradle#ktlint
// Spotless rather than ktlint-gradle or kotlinter (qk-fhdg): it takes explicit file targets, so
// generated sources stay out and the root project can lint build-logic/ and example/ too, and its
// spotlessInstallGitPrePushHook installs a pre-push hook, which `but push` runs and `but commit`
// doesn't run pre-commit. Code style and rule settings live in the root .editorconfig.
// Applied by quintkonnect.kotlin-jvm and the root project; build-logic/ and example/ are separate
// builds, linted from the root project (see its build.gradle.kts).
plugins {
    // spotlessCheck hooks into `check`, which the root project only has with `base`.
    base
    id("com.diffplug.spotless")
}

val ktlintVersion = extensions.getByType<VersionCatalogsExtension>().named("libs")
    .findVersion("ktlint").get().requiredVersion

spotless {
    // Off until the repo-wide reformat (qk-lafs) lands, so `check` stays green meanwhile.
    isEnforceCheck = false

    kotlin {
        // Only files under src/: KSP's output and gradle-plugin's PluginVersion.kt are generated
        // under build/ and registered as source dirs, so a source-set-based target would lint them.
        target("src/**/*.kt")
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(ktlintVersion)
    }
}
