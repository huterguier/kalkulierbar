package kalkulierbar.nonclausaltableaux

import kalkulierbar.CloseMessage
import kalkulierbar.IllegalMove
import kalkulierbar.JSONCalculus
import kalkulierbar.JsonParseException
import kalkulierbar.logic.FoTermModule
import kalkulierbar.logic.LogicModule
import kalkulierbar.logic.transform.NegationNormalForm
import kalkulierbar.parsers.FirstOrderParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.plus

class NonClausalTableaux : JSONCalculus<NcTableauxState, NcTableauxMove, Unit>() {

    private val serializer = Json(context = FoTermModule + LogicModule + NcMoveModule)

    override val identifier = "nc-tableaux"

    override fun parseFormulaToState(formula: String, params: Unit?): NcTableauxState {
        val parsedFormula = NegationNormalForm.transform(FirstOrderParser.parse(formula))

        return NcTableauxState(parsedFormula)
    }

    override fun applyMoveOnState(state: NcTableauxState, move: NcTableauxMove): NcTableauxState {
        // Clear status message
        state.statusMessage = null

        // Pass moves to relevant subfunction
        return when (move) {
            is AlphaMove -> applyAlpha(state, move.nodeID)
            is BetaMove -> applyBeta(state, move.nodeID)
            is GammaMove -> applyGamma(state, move.nodeID)
            is DeltaMove -> applyDelta(state, move.nodeID)
            is CloseMove -> applyClose(state, move.nodeID, move.closeID, move.getVarAssignTerms())
            is UndoMove -> applyUndo(state)
            else -> throw IllegalMove("Unknown move")
        }
    }

    /**
     * Undo a rule application by re-building the state from the move history
     * @param state State in which to apply the undo
     * @return Equivalent state with the most recent rule application removed
     */
    private fun applyUndo(state: NcTableauxState): NcTableauxState {
        if (!state.backtracking)
            throw IllegalMove("Backtracking is not enabled for this proof")

        // Can't undo any more moves in initial state
        if (state.moveHistory.isEmpty())
            return state

        // Create a fresh clone-state with the same parameters and input formula
        var freshState = NcTableauxState(state.formula)
        freshState.usedBacktracking = true

        // We don't want to re-do the last move
        state.moveHistory.removeAt(state.moveHistory.size - 1)

        // Re-build the proof tree in the clone state
        state.moveHistory.forEach {
            freshState = applyMoveOnState(freshState, it)
        }

        return freshState
    }

    override fun checkCloseOnState(state: NcTableauxState): CloseMessage {
        var msg = "The proof tree is not closed"

        if (state.nodes[0].isClosed) {
            val withWithoutBT = if (state.usedBacktracking) "with" else "without"
            msg = "The proof is closed and valid in non-clausal tableaux $withWithoutBT backtracking"
        }

        return CloseMessage(state.nodes[0].isClosed, msg)
    }

    /**
     * Parses a JSON state representation into a TableauxState object
     * @param json JSON state representation
     * @return parsed state object
     */
    @Suppress("TooGenericExceptionCaught")
    override fun jsonToState(json: String): NcTableauxState {
        try {
            val parsed = serializer.parse(NcTableauxState.serializer(), json)

            // Ensure valid, unmodified state object
            if (!parsed.verifySeal())
                throw JsonParseException("Invalid tamper protection seal, state object appears to have been modified")

            return parsed
        } catch (e: Exception) {
            val msg = "Could not parse JSON state: "
            throw JsonParseException(msg + (e.message ?: "Unknown error"))
        }
    }

    /**
     * Serializes internal state object to JSON
     * @param state State object
     * @return JSON state representation
     */
    override fun stateToJson(state: NcTableauxState): String {
        state.render()
        state.computeSeal()
        return serializer.stringify(NcTableauxState.serializer(), state)
    }

    /*
     * Parses a JSON move representation into a TableauxMove object
     * @param json JSON move representation
     * @return parsed move object
     */
    @Suppress("TooGenericExceptionCaught")
    override fun jsonToMove(json: String): NcTableauxMove {
        try {
            return serializer.parse(NcTableauxMove.serializer(), json)
        } catch (e: Exception) {
            val msg = "Could not parse JSON move: "
            throw JsonParseException(msg + (e.message ?: "Unknown error"))
        }
    }

    /*
     * Parses a JSON parameter representation into a TableauxParam object
     * @param json JSON parameter representation
     * @return parsed param object
     */
    override fun jsonToParam(json: String) = Unit
}
