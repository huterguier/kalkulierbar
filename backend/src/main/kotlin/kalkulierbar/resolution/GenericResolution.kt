package kalkulierbar.resolution

import kalkulierbar.CloseMessage
import kalkulierbar.IllegalMove
import kalkulierbar.InvalidFormulaFormat
import kalkulierbar.clause.Atom
import kalkulierbar.clause.Clause
import kalkulierbar.clause.ClauseSet
import kalkulierbar.logic.SyntacticEquality
import kalkulierbar.parsers.FirstOrderParser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule

interface GenericResolution<AtomType> {

    /**
     * Get information about a proof state
     * @param state Current proof state
     * @return CloseMessage containing information about the proof status
     */
    fun getCloseMessage(state: GenericResolutionState<AtomType>): CloseMessage {
        val hasEmptyClause = state.clauseSet.clauses.any { it.atoms.isEmpty() }
        val msg = if (hasEmptyClause) "The proof is closed" else "The proof is not closed"
        return CloseMessage(hasEmptyClause, msg)
    }

    /**
     * Create a new clause by resolving two existing clauses and add it to the clause set
     * If the given literal is null, a suitable literal will be chosen automatically
     * @param state Proof state to apply resolution in
     * @param clause1 ID of the first clause to use for resolution
     * @param clause2 ID of the second clause to use for resolution
     * @param literal Literal present in both clauses to use for resolution
     */
    @Suppress("ThrowsCount")
    fun resolve(state: GenericResolutionState<AtomType>, clause1: Int, clause2: Int, literal: AtomType?) {
        val clauses = state.clauseSet.clauses

        // Verify that the clause ids are valid
        if (clause1 == clause2)
            throw IllegalMove("Both ids refer to the same clause")
        if (clause1 < 0 || clause1 >= clauses.size)
            throw IllegalMove("There is no clause with id $clause1")
        if (clause2 < 0 || clause2 >= clauses.size)
            throw IllegalMove("There is no clause with id $clause2")

        val c1 = clauses[clause1]
        val c2 = clauses[clause2]
        val resCandidates: Pair<Atom<AtomType>, Atom<AtomType>>

        // If the frontend did not pass a resolution target, we'll try to find one ourselves
        if (literal == null) {
            resCandidates = getAutoResolutionCandidates(c1, c2)
        } else {
            // Filter clauses for atoms with correct literal
            val atomsInC1 = c1.atoms.filter { literalsAreEqual(it.lit, literal) }
            val atomsInC2 = c2.atoms.filter { literalsAreEqual(it.lit, literal) }
            if (atomsInC1.isEmpty())
                throw IllegalMove("Clause '$c1' does not contain atom '$literal'")
            if (atomsInC2.isEmpty())
                throw IllegalMove("Clause '$c2' does not contain atom '$literal'")

            val msg = "Clauses '$c1' and '$c2' do not contain atom '$literal' in both positive and negated form"
            resCandidates = findResCandidates(atomsInC1, atomsInC2)
                    ?: throw IllegalMove(msg)
        }

        val (a1, a2) = resCandidates

        // Add the new node where the second one was. This should be pretty nice for the user
        state.newestNode = clause2

        clauses.add(state.newestNode, buildClause(c1, a1, c2, a2))
    }

    /**
     * Hide a clause from the main view
     * @param state Current proof state
     * @param clauseID ID of the clause to be hidden
     */
    fun hide(state: GenericResolutionState<AtomType>, clauseID: Int) {
        if (clauseID < 0 || clauseID >= state.clauseSet.clauses.size)
            throw IllegalMove("There is no clause with id $clauseID")

        // Move clause from main clause set to hidden clause set
        val clauseToHide = state.clauseSet.clauses.removeAt(clauseID)
        state.hiddenClauses.add(clauseToHide)
        state.newestNode = -1
    }

    /**
     * Show all hidden clauses
     * @param state Current proof state
     */
    fun show(state: GenericResolutionState<AtomType>) {
        state.clauseSet.unite(state.hiddenClauses)
        state.hiddenClauses.clauses.clear()
        state.newestNode = -1
    }

    /**
     * Automatically find a resolution candidate for two given clauses
     * @param c1 First clause to resolve
     * @param c2 Second clause to resolve
     * @return Pair of suitable atoms in c1 and c2 for resolution
     */
    fun getAutoResolutionCandidates(c1: Clause<AtomType>, c2: Clause<AtomType>): Pair<Atom<AtomType>, Atom<AtomType>> {

        // Find literals present in both clauses
        var sharedAtoms = c1.atoms.filter {
            val c1atom = it
            c2.atoms.any { literalsAreEqual(c1atom.lit, it.lit) }
        }

        if (sharedAtoms.isEmpty())
            throw IllegalMove("Clauses '$c1' and '$c2' contain no common literals")

        // Sort out atoms not present in opposite polarity in c2 (shared atoms came from c1 originally)
        sharedAtoms = sharedAtoms.filter {
            c2.atoms.contains(it.not())
        }

        if (sharedAtoms.isEmpty())
            throw IllegalMove("Clauses '$c1' and '$c2' contain no common literals that appear" +
                "in positive and negated form")

        // Choose the first shared literal
        val a1 = sharedAtoms[0]
        val a2 = a1.not()

        return Pair(a1, a2)
    }

    /**
	 * Searches two atom lists for resolution candidates and returns the first.
	 * The lists have to be filtered for the spelling already.
	 * @param atoms1 The first list of atoms
	 * @param atoms2 The second list of atoms
	 * @return A pair of the two atoms for resolution.
	 */
    fun findResCandidates(
        atoms1: List<Atom<AtomType>>,
        atoms2: List<Atom<AtomType>>
    ): Pair<Atom<AtomType>, Atom<AtomType>>? {
        val (pos, neg) = atoms2.partition { !it.negated }

        for (a1 in atoms1) {
            val other = if (a1.negated) pos else neg
            if (other.isEmpty())
                continue
            val a2 = other[0]
            return Pair(a1, a2)
        }

        return null
    }

    /**
	 * Builds a new clause according to resolution.
	 * @param c1 The first clause for resolution
	 * @param a1 The atom to filter out of c1
	 * @param c2 The second clause for resolution
	 * @param a2 The atom to filter out of c2
	 * @return A new clause that contains all elements of c1 and c2 except for a1 and a2
	 */
    fun buildClause(
        c1: Clause<AtomType>,
        a1: Atom<AtomType>,
        c2: Clause<AtomType>,
        a2: Atom<AtomType>
    ): Clause<AtomType> {
        val atoms = c1.atoms.filter { it != a1 }.toMutableList() +
                c2.atoms.filter { it != a2 }.toMutableList()
        return Clause(atoms.distinct().toMutableList())
    }

    fun literalsAreEqual(a: AtomType, b: AtomType): Boolean {
        val eq: Boolean
        // Use syntactic equality for literal comparison if defined
        if (a is SyntacticEquality && b is SyntacticEquality)
            eq = a.synEq(b)
        else
            eq = (a == b)
        return eq
    }
}

interface GenericResolutionState<AtomType> {
    val clauseSet: ClauseSet<AtomType>
    val hiddenClauses: ClauseSet<AtomType>
    val highlightSelectable: Boolean
    var newestNode: Int
}

// Context object for FO term serialization
// Tells kotlinx.serialize about child types of ResolutionMove
val resolutionMoveModule = SerializersModule {
    polymorphic(ResolutionMove::class) {
        MoveResolve::class with MoveResolve.serializer()
        MoveResolveUnify::class with MoveResolveUnify.serializer()
        MoveInstantiate::class with MoveInstantiate.serializer()
        MoveHide::class with MoveHide.serializer()
        MoveShow::class with MoveShow.serializer()
    }
}

@Serializable
abstract class ResolutionMove

@Serializable
@SerialName("res-resolve")
data class MoveResolve(val c1: Int, val c2: Int, val literal: String?) : ResolutionMove()

@Serializable
@SerialName("res-resolveunify")
data class MoveResolveUnify(val c1: Int, val c2: Int, val l1: Int, val l2: Int) : ResolutionMove()

@Serializable
@SerialName("res-instantiate")
data class MoveInstantiate(val c1: Int, val varAssign: Map<String, String>) : ResolutionMove() {
    fun getVarAssignTerms() = varAssign.mapValues {
        try {
            FirstOrderParser.parseTerm(it.value)
        } catch (e: InvalidFormulaFormat) {
            throw InvalidFormulaFormat("Could not parse term '${it.value}': ${e.message}")
        }
    }
}

@Serializable
@SerialName("res-hide")
data class MoveHide(val c1: Int) : ResolutionMove()

@Serializable
@SerialName("res-show")
class MoveShow : ResolutionMove()