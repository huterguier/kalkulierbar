package kalkulierbar.logic.util

import kalkulierbar.UnificationImpossible
import kalkulierbar.logic.FirstOrderTerm
import kalkulierbar.logic.Function
import kalkulierbar.logic.QuantifiedVariable
import kalkulierbar.logic.Relation
import kalkulierbar.logic.transform.TermContainsVariable
import kalkulierbar.logic.transform.VariableInstantiator

/**
 * Implements functions for a first order unification
 */
class Unification {
    companion object {

        /**
         * Unifies two given relations by using "Robinson's Algorithm" (1963)
         * @param r1 the first relation to be unified with r2
         * @param r2 the second relation to be unified with r1
         * @return a map of the executed substitutions
         */
        fun unify(r1: Relation, r2: Relation): Map<String, FirstOrderTerm> {
            var rel1 = r1
            var rel2 = r2
            val arg1 = rel1.arguments
            val arg2 = rel2.arguments

            // Spelling has to be the same
            if (rel1.spelling != rel2.spelling)
                throw UnificationImpossible("Relations '$r1' and '$r2' have different names")
            // Arg size has to be the same length
            if (arg1.size != arg2.size)
                throw UnificationImpossible("Relations '$r1' and '$r2' have different numbers of arguments")

            val terms = mutableListOf<Pair<FirstOrderTerm, FirstOrderTerm>>()

            for (i in arg1.indices)
                terms.add(Pair(arg1[i], arg2[i]))

            return unifyTerms(terms)
        }

        /**
         * Unify a set of term pairs according to Robinson's algorithm
         * @param terms List of term pairs to be unified
         * @return Variable instantiation map
         */
        @Suppress("ComplexMethod")
        private fun unifyTerms(terms: MutableList<Pair<FirstOrderTerm, FirstOrderTerm>>): Map<String, FirstOrderTerm> {
            val map = mutableMapOf<String, FirstOrderTerm>()

            // As long as both relations aren't the same
            while (terms.isNotEmpty()) {

                var (term1, term2) = terms.removeAt(0)

                // Apply gathered substitutions just-in-time
                term1 = VariableInstantiator.transform(term1, map)
                term2 = VariableInstantiator.transform(term2, map)

                // Skip terms if already equal
                if (term1.synEq(term2)) {
                    continue
                } else if (term1 is QuantifiedVariable) {
                    // Unification not possible if one is variable and appears in others arguments
                    if (term2 is Function && TermContainsVariable.check(term2, term1.spelling))
                        throw UnificationImpossible("Variable '$term1' appears in '$term2'")

                    // Add substitution to map
                    map.put(term1.spelling, term2)
                } else if (term2 is QuantifiedVariable) {
                    // Swap them around to be processed later
                    terms.add(Pair(term2, term1))
                } else if (isCompatibleFunction(term1, term2)) {
                    val t1 = term1 as Function
                    val t2 = term2 as Function
                    // Break down into subterms
                    for (i in t1.arguments.indices)
                        terms.add(Pair(t1.arguments[i], t2.arguments[i]))
                } else {
                    throw UnificationImpossible("'$term1' and '$term2' cannot be unified")
                }
            }
            return map
        }

        /**
         * Determine if two terms represent the same function (same name and arity)
         * Arguments of the function may differ
         * @param t1 First term
         * @param t2 Second term
         * @return true iff the two terms represent the same functions
         */
        private fun isCompatibleFunction(t1: FirstOrderTerm, t2: FirstOrderTerm): Boolean {
            if (t1 is Function && t2 is Function)
                return t1.spelling == t2.spelling && t1.arguments.size == t2.arguments.size

            return false
        }
    }
}
