package kalkulierbar.tests.tableaux

import kalkulierbar.JsonParseException
import kalkulierbar.tableaux.MoveAutoClose
import kalkulierbar.tableaux.PropositionalTableaux
import kalkulierbar.tableaux.TableauxParam
import kalkulierbar.tableaux.TableauxType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestPropositionalJson {

    val instance = PropositionalTableaux()

    /*
        Test jsonToMove
    */

    @Test
    @kotlinx.serialization.UnstableDefault
    fun testJsonMoveValid() {
        val json = "{\"type\": \"tableaux-close\", \"id1\": 0, \"id2\": 12}"
        val move = instance.jsonToMove(json)
        assertEquals(MoveAutoClose(0, 12), move)
    }

    @Test
    @kotlinx.serialization.UnstableDefault
    fun testJsonMoveNull() {
        val json = "{\"type\": \"tableaux-close\", \"id1\": 0, \"id2\": null}"
        assertFailsWith<JsonParseException> {
            instance.jsonToMove(json)
        }
    }

    @Test
    @kotlinx.serialization.UnstableDefault
    fun testJsonMoveMissingField() {
        val json = "{\"type\": \"tableaux-expand\", \"id2\": 42}"
        assertFailsWith<JsonParseException> {
            instance.jsonToMove(json)
        }
    }

    @Test
    @kotlinx.serialization.UnstableDefault
    fun testJsonMoveTypeMismatch() {
        val json = "{\"type\": \"tableaux-expand\", \"id2\": \"dream\"}"
        assertFailsWith<JsonParseException> {
            instance.jsonToMove(json)
        }
    }

    /*
        Test jsonToState
    */

    @Test
    @kotlinx.serialization.UnstableDefault
    fun testJsonStateEmpty() {
        val json = """{"clauseSet":{"clauses":[{"atoms":[{"lit":"a","negated":false},{"lit":"b","negated":false}]},{"atoms":[{"lit":"a","negated":true}]},{"atoms":[{"lit":"b","negated":true}]}]},"type":"UNCONNECTED","regular":false,"backtracking":false,"nodes":[{"parent":null,"spelling":"true","negated":false,"isClosed":false,"closeRef":null,"children":[]}],"moveHistory":[],"backtracking":false,"seal":"9FB4C2B77422E91BAEB40529CEEBB8660F54A2DA64E0E27BAAFD41E61819D9A1"}"""
        val state = instance.jsonToState(json)
        assertEquals("tableauxstate|UNCONNECTED|false|false|false|{a, b}, {!a}, {!b}|[true;p;null;-;l;o;()]|[]", state.getHash())
    }

    @Test
    @kotlinx.serialization.UnstableDefault
    fun testJsonStateCorrupt() {
        val json = """{"clauseSet":{"clauses":"atoms":[{"lit":"a","negated":false},{"lit":"b","negated":false}]},{"atoms":[{"lit":"a","negated":true}]},{"atoms":[{"lit":"b","negated":true}]}]},"type":"UNCONNECTED","regular":false,"backtracking":false,"nodes":[{"parent":null,"spelling":"true","negated":false,"isClosed":false,"closeRef":null,"children":[]}],"moveHistory":[],"backtracking":false,"seal":"9FB4C2B77422E91BAEB40529CEEBB8660F54A2DA64E0E27BAAFD41E61819D9A1"}"""

        assertFailsWith<JsonParseException> {
            instance.jsonToState(json)
        }
    }

    @Test
    @kotlinx.serialization.UnstableDefault
    fun testJsonStateMissingField() {
        val json = """{"clauseSet":{"clauses":[{"atoms":[{"lit":"a","negated":false},{negated":false}]},{"atoms":[{"lit":"a","negated":true}]},{"atoms":[{"lit":"b","negated":true}]}]},"type":"UNCONNECTED","regular":false,"backtracking":false,"nodes":[{"parent":null,"spelling":"true","negated":false,"isClosed":false,"closeRef":null,"children":[]}],"moveHistory":[],"backtracking":false,"seal":"9FB4C2B77422E91BAEB40529CEEBB8660F54A2DA64E0E27BAAFD41E61819D9A1"}"""

        assertFailsWith<JsonParseException> {
            instance.jsonToState(json)
        }
    }

    @Test
    @kotlinx.serialization.UnstableDefault
    fun testJsonStateModify() {
        val json = """{"clauseSet":{"clauses":[{"atoms":[{"lit":"a","negated":false},{"lit":"c","negated":false}]},{"atoms":[{"lit":"a","negated":true}]},{"atoms":[{"lit":"b","negated":true}]}]},"type":"UNCONNECTED","regular":false,"backtracking":false,"nodes":[{"parent":null,"spelling":"true","negated":false,"isClosed":false,"closeRef":null,"children":[]}],"moveHistory":[],"backtracking":false,"seal":"9FB4C2B77422E91BAEB40529CEEBB8660F54A2DA64E0E27BAAFD41E61819D9A1"}"""
        assertFailsWith<JsonParseException> {
            instance.jsonToState(json)
        }
    }

    @Test
    @kotlinx.serialization.UnstableDefault
    fun testJsonStateTypeMismatch() {
        val json = """{"clauseSet":{"clauses":[{"atoms":[{"lit":3,"negated":false},{"lit":"b","negated":false}]},{"atoms":[{"lit":"a","negated":true}]},{"atoms":[{"lit":"b","negated":true}]}]},"type":"UNCONNECTED","regular":false,"backtracking":false,"nodes":[{"parent":null,"spelling":"true","negated":false,"isClosed":false,"closeRef":null,"children":[]}],"moveHistory":[],"backtracking":false,"seal":"9FB4C2B77422E91BAEB40529CEEBB8660F54A2DA64E0E27BAAFD41E61819D9A1"}"""

        assertFailsWith<JsonParseException> {
            instance.jsonToState(json)
        }
    }

    /*
        Test jsonToParam
    */

    @Test
    @kotlinx.serialization.UnstableDefault
    fun testJsonParamValid() {
        val json = "{\"type\": \"UNCONNECTED\", \"regular\": false, \"backtracking\": false}"
        val param = instance.jsonToParam(json)
        assertEquals(TableauxParam(TableauxType.UNCONNECTED, false, false), param)
    }

    @Test
    @kotlinx.serialization.UnstableDefault
    fun testJsonParamNull() {
        val json = "{\"type\": \"WEAKLYCONNECTED\", \"regular\": null, \"backtracking\": false}"
        assertFailsWith<JsonParseException> {
            instance.jsonToParam(json)
        }
    }

    @Test
    @kotlinx.serialization.UnstableDefault
    fun testJsonParamMissingField() {
        val json = "{\"type\": \"STRONGLYCONNECTED\"}"
        assertFailsWith<JsonParseException> {
            instance.jsonToParam(json)
        }
    }

    @Test
    @kotlinx.serialization.UnstableDefault
    fun testJsonParamTypeMismatch() {
        val json = "{\"type\": \"42\", \"regular\": false, \"backtracking\": false}"
        assertFailsWith<JsonParseException> {
            instance.jsonToParam(json)
        }
    }
}
