package kalkulierbar.parsers

import kalkulierbar.InvalidFormulaFormat
import kalkulierbar.logic.Constant
import kalkulierbar.logic.ExistentialQuantifier
import kalkulierbar.logic.FirstOrderTerm
import kalkulierbar.logic.Function
import kalkulierbar.logic.LogicNode
import kalkulierbar.logic.Not
import kalkulierbar.logic.QuantifiedVariable
import kalkulierbar.logic.Relation
import kalkulierbar.logic.UniversalQuantifier

/**
 * Recursive descent parser for first-order logic
 * For format specification, see docs/FirstOrderFormula.md
 */
@Suppress("TooManyFunctions")
class FirstOrderParser : PropositionalParser() {

    // List of quantifier scopes for correct variable binding
    private val quantifierScope = mutableListOf<MutableList<QuantifiedVariable>>()

    /**
     * Parses a first order formula
     * @param formula input formula
     * @return LogicNode representing the formula
     */
    override fun parse(formula: String): LogicNode {
        // Clear quantifier scope to avoid problems on instance re-use
        quantifierScope.clear()
        return super.parse(formula)
    }

    /**
     * Parses a unary not
     * @return LogicNode representing the negated formula
     */
    protected override fun parseNot(): LogicNode {

        if (nextTokenIs(TokenType.NOT)) {
            consume()
            return Not(parseQuantifier())
        } else {
            return parseQuantifier()
        }
    }

    /**
     * Parses a series of 0 or more quantifiers, both existential and universal
     * @return LogicNode representing the series of quantifiers
     */
    private fun parseQuantifier(): LogicNode {

        if (!nextTokenIs(TokenType.UNIVERSALQUANT) && !nextTokenIs(TokenType.EXISTENTIALQUANT))
            return parseParen()

        val quantType = tokens.first().type
        consume()

        if (!nextTokenIs(TokenType.CAPID))
            throw InvalidFormulaFormat("Expected quantified variable but got ${gotMsg()}")

        val varName = tokens.first().spelling
        consume()
        consume(TokenType.COLON)

        // Open a new scope for this quantifier
        // All QuantifiedVariables encountered in the subexpression will be added there
        quantifierScope.add(mutableListOf<QuantifiedVariable>())

        val subexpression = parseNot()

        // Bound variables are variables with the spelling specified in the quantifier
        // Variables encountered with other spellings may be bound by surrounding quantifiers
        val boundVariables = quantifierScope.last().filter { it.spelling == varName }
        val boundBefore = quantifierScope.last().filter { it.spelling != varName }
        quantifierScope.removeAt(quantifierScope.size - 1) // Close the scope

        // If not yet bound variables remain in our scope, add them to the next higher scope
        // If no scope exists, the variables are unbound and the formula is incorrect
        if (quantifierScope.size == 0 && boundBefore.size > 0)
            throw InvalidFormulaFormat("Unbound Variables found: ${boundBefore.joinToString(", ")}")
        else if (boundBefore.size > 0)
            quantifierScope.last().addAll(boundBefore)

        val res: LogicNode

        if (quantType == TokenType.UNIVERSALQUANT)
            res = UniversalQuantifier(varName, subexpression, boundVariables)
        else
            res = ExistentialQuantifier(varName, subexpression, boundVariables)

        return res
    }

    /**
     * Parses a parenthesis in a formula
     * @return LogicNode representing the contents of the parenthesis
     */
    protected override fun parseParen(): LogicNode {
        if (nextTokenIs(TokenType.LPAREN)) {
            consume()
            val exp = parseEquiv()
            consume(TokenType.RPAREN)
            return exp
        } else {
            return parseAtomic()
        }
    }

    /**
     * Parses an atomic formula / a relation
     * @return LogicNode representing the parsed Relation
     */
    private fun parseAtomic(): LogicNode {
        if (!nextTokenIs(TokenType.CAPID))
            throw InvalidFormulaFormat("Expected relation identifier but got ${gotMsg()}")

        val relationIdentifier = tokens.first().spelling
        consume()
        consume(TokenType.LPAREN)

        // Relation may have an arbitrary amount of argument terms
        val arguments = mutableListOf(parseTerm())
        while (nextTokenIs(TokenType.COMMA)) {
            consume()
            arguments.add(parseTerm())
        }

        consume(TokenType.RPAREN)

        return Relation(relationIdentifier, arguments)
    }

    /**
     * Parses a first-order term made up of quantified variables,
     * constants, and functions
     * @return FirstOrderTerm representing the parsed term
     */
    private fun parseTerm(): FirstOrderTerm {
        if (!nextTokenIsIdentifier())
            throw InvalidFormulaFormat("Expected identifier but got ${gotMsg()}")

        val res: FirstOrderTerm

        if (nextTokenIs(TokenType.CAPID)) {
            // Next token is quantified variable
            res = parseQuantifiedVariable()
        } else {
            // Next token is constant or function
            val identifier = tokens.first().spelling
            consume()
            if (nextTokenIs(TokenType.LPAREN))
                res = parseFunction(identifier)
            else
                res = Constant(identifier)
        }

        return res
    }

    /**
     * Parses a function invocation
     * Note: This is called when the name of the function is already consumed
     *       due to ambiguity between functions and constants
     *       The consumed name is passed as an argument to this function instead
     * @param identifier Name of the function invoked
     * @return FirstOrderTerm representing the function invocation
     */
    private fun parseFunction(identifier: String): FirstOrderTerm {
        consume(TokenType.LPAREN)

        val arguments = mutableListOf(parseTerm())
        while (nextTokenIs(TokenType.COMMA)) {
            consume()
            arguments.add(parseTerm())
        }

        consume(TokenType.RPAREN)

        return Function(identifier, arguments)
    }

    /**
     * Parses a quantified variable and adds it to the current scope
     * @return FirstOrderTerm representing the quantified variable
     */
    private fun parseQuantifiedVariable(): FirstOrderTerm {
        val res = QuantifiedVariable(tokens.first().spelling)

        // Add variable to list of bound variables so the binding quantifier is informed of its existence
        if (quantifierScope.size == 0)
            throw InvalidFormulaFormat("Unbound variable ${gotMsg()}")

        quantifierScope.last().add(res)

        consume(TokenType.CAPID)
        return res
    }
}