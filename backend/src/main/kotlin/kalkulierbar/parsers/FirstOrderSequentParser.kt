package kalkulierbar.parsers

import kalkulierbar.InvalidFormulaFormat
import kalkulierbar.EmptyFormulaException
import kalkulierbar.logic.And
import kalkulierbar.logic.Equiv
import kalkulierbar.logic.Impl
import kalkulierbar.logic.LogicNode
import kalkulierbar.logic.Not
import kalkulierbar.logic.Or
import kalkulierbar.logic.Var
import kalkulierbar.sequentCalculus.fosc.FOSCState;

@Suppress("TooManyFunctions")
open class FirstOrderSequentParser {

    open fun parse(formula: String): FOSCState {
        val sides = formula.split("|-") 
        
        if (sides.size > 2) {
            var i = 0;
            i += sides[0].length;
            for (j in 1..(sides.size - 2)) {
                i += sides[j].length;
                i += 2;
            }
            throw InvalidFormulaFormat("Incorrect formula syntax at char $i")
        }
        else if (sides.size == 2) {
            val leftFormulas = mutableListOf<LogicNode>();
            val rightFormulas = mutableListOf<LogicNode>();

            val leftArray = this.splitToFormulas(sides[0]);
            val rightArray = this.splitToFormulas(sides[1]);

            var currentIndex = 0;
            for (strIndex in leftArray.indices) {
                try {
                    leftFormulas.add(FirstOrderParser().parse(leftArray[strIndex], currentIndex))
                } catch(e: EmptyFormulaException) {

                }
                currentIndex += leftArray[strIndex].length;
                if (strIndex < leftArray.size - 1) {
                    currentIndex++;
                }
            }
            currentIndex += 2;
            for (strIndex in rightArray.indices) {
                try {
                    rightFormulas.add(FirstOrderParser().parse(rightArray[strIndex], currentIndex))
                } catch(e: EmptyFormulaException) {
                    
                }
                currentIndex += rightArray[strIndex].length;
                if (strIndex < rightArray.size - 1) {
                    currentIndex++;
                }
            }

            return FOSCState(leftFormulas, rightFormulas);
        }
        else {
            val leftFormulas = mutableListOf<LogicNode>();
            val rightFormulas = mutableListOf<LogicNode>();

            val rightArray = this.splitToFormulas(sides[0]);

            var currentIndex = 0;
            for (strIndex in rightArray.indices) {
                try {
                    rightFormulas.add(FirstOrderParser().parse(rightArray[strIndex], currentIndex))
                } catch(e: EmptyFormulaException) {

                }
                currentIndex += rightArray[strIndex].length;
                if (strIndex < rightArray.size - 1) {
                    currentIndex++;
                }
            }
            
            return FOSCState(leftFormulas, rightFormulas);
        }
    }

    private fun splitToFormulas(str: String): List<String> {
        val foundFormulas = mutableListOf<String>();
        val it = str.iterator();

        var openParentheses = 0;
        var startOfFormula = 0;
        
        for ((index, value) in it.withIndex()) {
            if (value == '(')
                openParentheses++;
            if (value == ')')
                openParentheses--;
            
            if (openParentheses != 0)
                continue;

            if (value == ',') {
                foundFormulas.add(str.subSequence(startOfFormula, index).toString());
                startOfFormula = index + 1;
            }
        }
        foundFormulas.add(str.subSequence(startOfFormula, str.length).toString());
        
        return foundFormulas;
    }
}
