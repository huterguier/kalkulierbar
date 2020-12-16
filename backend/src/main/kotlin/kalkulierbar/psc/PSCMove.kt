package kalkulierbar.psc

import kalkulierbar.InvalidFormulaFormat
import kalkulierbar.logic.FirstOrderTerm
import kalkulierbar.parsers.FirstOrderParser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule

// Context object for move serialization
// Tells kotlinx.serialize about child types of PSCMove
val PSCMoveModule = SerializersModule {
    polymorphic(PSCMove::class) {
        NotRight::class with NotRight.serializer()
        NotLeft::class with NotLeft.serializer()
        OrRight::class with OrRight.serializer()
        OrLeft::class with OrLeft.serializer()
        AndRight::class with AndRight.serializer()
        AndLeft::class with AndLeft.serializer()
        UndoMove::class with UndoMove.serializer()
    }
}

@Serializable
abstract class PSCMove


@Serializable
@SerialName("notRight")
class NotRight(
    val nodeID: Int,
    val listIndex: Int
) : PSCMove() {
}

@Serializable
@SerialName("notLeft")
class NotLeft(
    val nodeID: Int,
    val listIndex: Int
) : PSCMove() {
}

@Serializable
<<<<<<< HEAD
@SerialName("orRight")
class OrRight(
        val nodeID: Int,
        val listIndex: Int
=======
@SerialName("andRight")
class AndRight(
    val nodeID: Int,
    val listIndex: Int
>>>>>>> feature/us17_andLeft
) : PSCMove() {
}

@Serializable
<<<<<<< HEAD
@SerialName("orLeft")
class OrLeft(
        val nodeID: Int,
        val listIndex: Int
=======
@SerialName("andLeft")
class AndLeft(
    val nodeID: Int,
    val listIndex: Int
>>>>>>> feature/us17_andLeft
) : PSCMove() {
}

@Serializable
@SerialName("undo")
class UndoMove : PSCMove() {
    override fun toString() = "(undo)"
}
