package io.github.mcbianconi.quintkonnect.ksp.ir

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.File

// Fixture generated with (quint 0.32.0, checked in alongside the spec):
//   quint typecheck --out ksp/src/test/resources/ir/fixture.ir.json ksp/src/test/resources/ir/fixture.qnt
class QuintIrTest {

    private val fixtureJson = File("src/test/resources/ir/fixture.ir.json").readText()

    @Test
    fun `parses state variable types, including a record`() {
        val module = parseQuintIr(fixtureJson).single()

        assertEquals(QuintType.IntType, module.variables.getValue("value"))
        assertEquals(
            QuintType.RecordType(mapOf("limit" to QuintType.IntType)),
            module.variables.getValue("config"),
        )
        assertEquals(QuintType.ConstType("Choice"), module.variables.getValue("lastChoice"))
    }

    @Test
    fun `parses a sum typedef with a unit variant and a record-payload variant`() {
        val module = parseQuintIr(fixtureJson).single()

        assertEquals(
            QuintType.SumType(
                mapOf(
                    "Init" to QuintType.TupleType(emptyList()),
                    "Add" to QuintType.RecordType(mapOf("n" to QuintType.IntType)),
                ),
            ),
            module.typeDefs.getValue("Choice"),
        )
    }

    @Test
    fun `collects a nondet reachable only through a helper action`() {
        val module = parseQuintIr(fixtureJson).single()

        // `step = any { pick }` has no `nondet` of its own; `n` is bound inside `pick`, which
        // `step` only reaches through a zero-arg name reference resolved via the IR's `table`.
        val step = module.actions.getValue("step")
        assertEquals(listOf(QuintNondetParam("n", QuintType.IntType)), step.nondetParams)

        val pick = module.actions.getValue("pick")
        assertEquals(listOf(QuintNondetParam("n", QuintType.IntType)), pick.nondetParams)

        val init = module.actions.getValue("init")
        assertEquals(emptyList<QuintNondetParam>(), init.nondetParams)
    }

    @Test
    fun `loadQuintIrModule resolves a spec-relative IR file under irDir`() {
        // loadQuintIrModule looks up "<irDir>/<specPath>.json"; the checked-in fixture is named
        // fixture.ir.json, so the "spec path" it stands in for here is "ir/fixture.ir".
        val irDir = File("src/test/resources")
        val module = loadQuintIrModule(irDir, "ir/fixture.ir", main = null)

        assertEquals("fixture", module?.name)
    }

    @Test
    fun `loadQuintIrModule returns null when the IR file is missing`() {
        val irDir = File("src/test/resources")
        assertNull(loadQuintIrModule(irDir, "ir/does-not-exist.qnt", main = null))
    }
}
