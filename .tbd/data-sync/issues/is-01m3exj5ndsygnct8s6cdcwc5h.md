---
type: is
id: is-01m3exj5ndsygnct8s6cdcwc5h
title: KSP processor does not reject duplicate @QuintAction names
kind: bug
status: closed
priority: 2
version: 3
labels: []
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T13:13:06.989Z
updated_at: 2026-09-26T13:32:32.413Z
closed_at: 2026-09-26T13:32:32.412Z
close_reason: null
resolution: null
duplicate_of: null
---
QuintKonnectProcessor (ksp/src/main/.../QuintKonnectProcessor.kt) and StepMethodGenerator never call KSPLogger.error, so two @QuintAction-annotated methods on the same @QuintRun/@QuintTest driver class sharing the same action name (explicit or defaulted) compile without error. The generated generatedStep() 'when' has two branches with the same string label; Kotlin does not reject duplicate 'when' branch labels, so the first branch silently shadows the second at runtime, and the second method is never dispatched to. Found via ksp/src/test/kotlin/.../ProcessorErrorTest.kt (qk-49by), which has a disabled test asserting this should be a compile-time error. Fix: StepMethodGenerator should collect action names per class and call logger.error(...) on a duplicate before generating, so KSP reports it as a compilation error instead of silently generating broken dispatch.
