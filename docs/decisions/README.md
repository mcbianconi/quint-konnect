# Decisions

Project decisions and standing constraints that aren't derivable from the code itself.
One file per decision. Keep entries short: the decision, why, and how it should shape
future work.

- [No Windows support](no-windows-support.md) — Windows isn't a target platform
- [ITF collection mapping](itf-collection-mapping.md) — `Set<T>` for sets, `Map<List<Long>, V>` for tuple-keyed maps
- [ITF Option and BigInt mapping](itf-option-and-bigint.md) — `Option[T]` is `T?`, unwrapped only for nullable descriptors
- [Runner failure contract](runner-failure-contract.md) — step failures become `AssertionError` with location; seed always printed
- [Quint version pin](quint-version-pin.md) — CI pins quint 0.32.0; bump deliberately
- [Parallel agent work](parallel-agent-work.md) — beads, disjoint files, one GitButler branch per agent, stacking allowed
- [Ship it lands directly](ship-it-lands-directly.md) — "ship it" = `but land` onto main, push, watch CI, sync tbd
