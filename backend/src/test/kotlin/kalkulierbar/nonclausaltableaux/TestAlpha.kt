package kalkulierbar.nonclausaltableaux

import kalkulierbar.IllegalMove
import kalkulierbar.parsers.FirstOrderParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestAlpha {

    val instance = NonClausalTableaux()

    @Test
    fun testInvalidFormula() {
        val formula = FirstOrderParser.parse("/all X: P(X)")
        val state = NcTableauxState(formula)

        assertFailsWith<IllegalMove> {
            instance.applyMoveOnState(state, AlphaMove(0))
        }
    }

    @Test
    fun testInvalidIndex() {
        val formula = FirstOrderParser.parse("/all X: P(X)")
        val state = NcTableauxState(formula)

        assertFailsWith<IllegalMove> {
            instance.applyMoveOnState(state, AlphaMove(1))
        }

        assertFailsWith<IllegalMove> {
            instance.applyMoveOnState(state, AlphaMove(-1))
        }
    }

    @Test
    fun testNonLeaf() {
        val formula = FirstOrderParser.parse("P(c) & P(d)")
        var state = NcTableauxState(formula)

        state = instance.applyMoveOnState(state, AlphaMove(0))

        assertFailsWith<IllegalMove> {
            instance.applyMoveOnState(state, AlphaMove(1))
        }
    }

    @Test
    fun testBasic() {
        val formula = FirstOrderParser.parse("P(c) & P(d)")
        var state = NcTableauxState(formula)

        state = instance.applyMoveOnState(state, AlphaMove(0))

        assertEquals("P(d)", state.nodes[1].spelling)
        assertEquals(0, state.nodes[1].parent)
        assertEquals("P(c)", state.nodes[2].spelling)
        assertEquals(1, state.nodes[2].parent)
        assertEquals(3, state.nodes.size)
    }

    @Test
    fun testChained() {
        val formula = FirstOrderParser.parse("P(c) & P(d) & P(e) & P(f)")
        var state = NcTableauxState(formula)

        state = instance.applyMoveOnState(state, AlphaMove(0))

        assertEquals("P(f)", state.nodes[1].spelling)
        assertEquals(0, state.nodes[1].parent)
        assertEquals("P(e)", state.nodes[2].spelling)
        assertEquals(1, state.nodes[2].parent)
        assertEquals("P(d)", state.nodes[3].spelling)
        assertEquals(2, state.nodes[3].parent)
        assertEquals("P(c)", state.nodes[4].spelling)
        assertEquals(3, state.nodes[4].parent)
        assertEquals(5, state.nodes.size)
    }

    @Test
    fun testComplex() {
        val formula = FirstOrderParser.parse("P(c) & (P(d) | Q(c) & Q(q)) & (P(e) & P(f))")
        println(formula)
        var state = NcTableauxState(formula)

        state = instance.applyMoveOnState(state, AlphaMove(0))

        assertEquals("P(f)", state.nodes[1].spelling)
        assertEquals("P(e)", state.nodes[2].spelling)
        assertEquals("(P(d) ∨ (Q(c) ∧ Q(q)))", state.nodes[3].spelling)
        assertEquals("P(c)", state.nodes[4].spelling)
        assertEquals(5, state.nodes.size)
    }
}
