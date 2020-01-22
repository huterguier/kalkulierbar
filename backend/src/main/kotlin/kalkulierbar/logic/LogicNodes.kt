package kalkulierbar.logic

import kalkulierbar.logic.transform.LogicNodeVisitor
import kotlinx.serialization.Serializable

class Var(var spelling: String) : LogicNode() {

    /**
     * Translates arbitrary formulae into equivalent representations
     * using only basic operations (var, not, and, or)
     * @return representation of this LogicNode using only basic logic operations
     */
    override fun toBasicOps() = this

    override fun toString() = spelling

    override fun <ReturnType> accept(visitor: LogicNodeVisitor<ReturnType>) = visitor.visit(this)
}

class Not(child: LogicNode) : UnaryOp(child) {

    override fun toString() = "!$child"

    override fun <ReturnType> accept(visitor: LogicNodeVisitor<ReturnType>) = visitor.visit(this)
}

class And(leftChild: LogicNode, rightChild: LogicNode) : BinaryOp(leftChild, rightChild) {

    override fun toString() = "($leftChild ∧ $rightChild)"

    override fun <ReturnType> accept(visitor: LogicNodeVisitor<ReturnType>) = visitor.visit(this)
}

class Or(leftChild: LogicNode, rightChild: LogicNode) : BinaryOp(leftChild, rightChild) {

    override fun toString() = "($leftChild ∨ $rightChild)"

    override fun <ReturnType> accept(visitor: LogicNodeVisitor<ReturnType>) = visitor.visit(this)
}

class Impl(leftChild: LogicNode, rightChild: LogicNode) : BinaryOp(leftChild, rightChild) {

    /**
     * Translates arbitrary formulae into equivalent representations
     * using only basic operations (var, not, and, or)
     * @return representation of this LogicNode using only basic logic operations
     */
    override fun toBasicOps(): LogicNode {
        leftChild = leftChild.toBasicOps()
        rightChild = rightChild.toBasicOps()
        return Or(Not(leftChild), rightChild)
    }

    override fun toString() = "($leftChild --> $rightChild)"

    override fun <ReturnType> accept(visitor: LogicNodeVisitor<ReturnType>) = visitor.visit(this)
}

class Equiv(leftChild: LogicNode, rightChild: LogicNode) : BinaryOp(leftChild, rightChild) {

    /**
     * Translates arbitrary formulae into equivalent representations
     * using only basic operations (var, not, and, or)
     * @return representation of this LogicNode using only basic logic operations
     */
    override fun toBasicOps(): LogicNode {
        leftChild = leftChild.toBasicOps()
        rightChild = rightChild.toBasicOps()
        return Or(And(leftChild, rightChild), And(Not(leftChild), Not(rightChild)))
    }

    override fun toString() = "($leftChild <=> $rightChild)"

    override fun <ReturnType> accept(visitor: LogicNodeVisitor<ReturnType>) = visitor.visit(this)
}

@Serializable
class Relation(val spelling: String, var arguments: List<FirstOrderTerm>) : LogicNode() {
    override fun toBasicOps() = this

    override fun toString() = "$spelling(${arguments.joinToString(", ")})"

    override fun <ReturnType> accept(visitor: LogicNodeVisitor<ReturnType>) = visitor.visit(this)

    override fun equals(other: Any?): Boolean {
        if (other == null || !(other is Relation))
            return false

        return spelling == other.spelling && arguments == other.arguments
    }

    override fun hashCode() = toString().hashCode()
}

class UniversalQuantifier(
    var varName: String,
    child: LogicNode,
    val boundVariables: MutableList<QuantifiedVariable>
) : UnaryOp(child) {

    override fun toString() = "(∀$varName: $child)"

    override fun <ReturnType> accept(visitor: LogicNodeVisitor<ReturnType>) = visitor.visit(this)
}

class ExistentialQuantifier(
    var varName: String,
    child: LogicNode,
    val boundVariables: MutableList<QuantifiedVariable>
) : UnaryOp(child) {

    override fun toString() = "(∃$varName: $child)"

    override fun <ReturnType> accept(visitor: LogicNodeVisitor<ReturnType>) = visitor.visit(this)
}
