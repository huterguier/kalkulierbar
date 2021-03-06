package kalkulierbar.tests.logic

import kalkulierbar.clause.Atom
import kalkulierbar.logic.Constant
import kalkulierbar.logic.FirstOrderTerm
import kalkulierbar.logic.Relation
import kalkulierbar.parsers.FirstOrderParser
import kotlin.test.Test
import kotlin.test.assert
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TestSyntacticEquality {

    private val equalPairs = listOf<Pair<String, String>>(
            Pair("f(g(f(q)), c)", "f(g(f(q)), c)"),
            Pair("a", "a"),
            Pair("f(X)", "f(X)")
    )

    private val unequalPairs = listOf<Pair<String, String>>(
            Pair("f(g(f(q)), c)", "f(g(f(q)), d)"),
            Pair("a", "d"),
            Pair("f(X)", "f(X, X)"),
            Pair("f(g(f(c)))", "f(g(f(g(c))))"),
            Pair("X", "Y"),
            Pair("X", "x")
    )

    private val invalid = listOf(
            "\\ex X: R(X) & R(sk-1)"
    )

    @Test
    fun testAtomStringEquality() {
        val a = Atom("a", false)
        val b = Atom("b", false)
        val c = Atom("a", true)
        val d = Atom("a", false)

        assertEquals(a, d)
        assertNotEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun testAtomRelationEquality() {
        val a = Atom(Relation("R", listOf<FirstOrderTerm>(Constant("c1"))), false)
        val b = Atom(Relation("R", listOf<FirstOrderTerm>(Constant("c2"))), false)
        val c = Atom(Relation("Q", listOf<FirstOrderTerm>(Constant("c1"))), false)
        val d = Atom(Relation("R", listOf<FirstOrderTerm>(Constant("c1"))), false)

        assertEquals(a, d)
        assertNotEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun testEqualTerms() {
        for ((a, b) in equalPairs) {
            val parsedA = FirstOrderParser.parseTerm(a)
            val parsedB = FirstOrderParser.parseTerm(b)

            assert(parsedA.synEq(parsedB))
        }
    }

    @Test
    fun testUnequalTerms() {
        for ((a, b) in unequalPairs) {
            val parsedA = FirstOrderParser.parseTerm(a)
            val parsedB = FirstOrderParser.parseTerm(b)

            assert(!parsedA.synEq(parsedB))
        }
    }
}
