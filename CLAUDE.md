## Project Decisions

See `docs/decisions/` for standing project decisions and constraints (e.g. platform
support). Check it for context. Add a new entry there only when the decision isn't
already derivable from the code, tests, KDoc, or other docs — if it is, put it there
instead (a code comment, an error message) rather than in a separate file.

## Representing Quint Types in Kotlin

See `skills/quint-konnect/references/types.md` for the Quint-to-Kotlin type table; it's the
canonical copy, not duplicated here or in README.md.

## Keeping the agent skill in sync

A change to `annotations`, `Driver`, `TypedState`, `DriverConfig`, the Gradle plugin, or the
`QUINT_SEED`/`QUINT_VERBOSE`/`QUINT_COLOR` env vars or Gradle properties must update
`skills/quint-konnect/` in the same change.
