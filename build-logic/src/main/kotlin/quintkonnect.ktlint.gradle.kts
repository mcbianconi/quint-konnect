// ktlint through Spotless: https://github.com/diffplug/spotless/tree/main/plugin-gradle#ktlint
// Spotless rather than ktlint-gradle or kotlinter (qk-fhdg): it takes explicit file targets, so
// generated sources stay out and the root project can lint build-logic/ and example/ too, and its
// spotlessInstallGitPrePushHook installs a pre-push hook, which suits GitButler: `but commit` runs
// no git hooks at all, while `but push` and `but pr new` run pre-push (qk-rtgv). Code style and
// rule settings live in the root .editorconfig.
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
