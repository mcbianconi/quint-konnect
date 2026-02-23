@file:OptIn(ExperimentalSerializationApi::class)

package io.github.mcbianconi.quintkonnect.example.tictactoe

import io.github.mcbianconi.quintkonnect.TypedState
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.serializer

// Mirrors Quint: type Player = X | O
@Serializable
@JsonClassDiscriminator("tag")
sealed class PlayerSer {
    @Serializable @SerialName("X") data object X : PlayerSer()
    @Serializable @SerialName("O") data object O : PlayerSer()
}

// Mirrors Quint: type Square = Occupied(Player) | Empty
@Serializable
@JsonClassDiscriminator("tag")
sealed class SquareSer {
    @Serializable
    @SerialName("Occupied")
    data class Occupied(val value: PlayerSer) : SquareSer()

    @Serializable
    @SerialName("Empty")
    data object Empty : SquareSer()
}

// Mirrors Quint state: board: int -> (int -> Square), nextTurn: Player
@Serializable
data class GameState(
    val board: Map<Long, Map<Long, SquareSer>>,
    val nextTurn: PlayerSer,
)

class TicTacToeState : TypedState<TicTacToeDriver, GameState>(serializer()) {
    override fun extractFromDriver(driver: TicTacToeDriver): GameState {
        // board[col][row] = cell at Quint coord (col, row), 1-indexed
        // driver.game.board[rowIdx][colIdx] = cell at (col=colIdx+1, row=rowIdx+1)
        val board = buildMap<Long, Map<Long, SquareSer>> {
            for (colIdx in 0..2) {
                val col = (colIdx + 1).toLong()
                val rows = buildMap<Long, SquareSer> {
                    for (rowIdx in 0..2) {
                        val row = (rowIdx + 1).toLong()
                        val cell = driver.game.board[rowIdx][colIdx]
                        put(row, if (cell == null) SquareSer.Empty else SquareSer.Occupied(cell.toSer()))
                    }
                }
                put(col, rows)
            }
        }
        return GameState(board = board, nextTurn = driver.game.nextTurn.toSer())
    }

    private fun Player.toSer(): PlayerSer = when (this) {
        Player.X -> PlayerSer.X
        Player.O -> PlayerSer.O
    }
}
