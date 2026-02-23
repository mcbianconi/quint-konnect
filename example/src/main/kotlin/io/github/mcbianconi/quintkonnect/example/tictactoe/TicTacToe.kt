package io.github.mcbianconi.quintkonnect.example.tictactoe

enum class Player { X, O }

typealias Coord = Pair<Long, Long>
typealias Board = Array<Array<Player?>>

class TicTacToe {
    val board: Board = Array(3) { arrayOfNulls(3) }
    var nextTurn: Player = Player.X

    fun movePlayerTo(player: Player, coord: Coord) {
        val (col, row) = coord
        require(nextTurn == player) { "Player $player is out of turn" }
        require(board[row.toInt() - 1][col.toInt() - 1] == null) { "Cell $coord is already occupied" }
        board[row.toInt() - 1][col.toInt() - 1] = player
        nextTurn = if (player == Player.X) Player.O else Player.X
    }
}
