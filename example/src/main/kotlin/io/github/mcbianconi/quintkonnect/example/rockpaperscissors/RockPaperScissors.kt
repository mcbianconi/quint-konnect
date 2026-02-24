package io.github.mcbianconi.quintkonnect.example.rockpaperscissors

enum class Move { INIT, ROCK, PAPER, SCISSORS }
enum class Player { PLAYER1, PLAYER2 }

sealed class GameStatus {
    object Started : GameStatus()
    object Pending : GameStatus()
    object Draw : GameStatus()
    data class Winner(val player: Player) : GameStatus()
}

class RockPaperScissors {
    var p1Move: Move = Move.INIT
    var p2Move: Move = Move.INIT
    var status: GameStatus = GameStatus.Started

    fun init() {
        p1Move = Move.INIT
        p2Move = Move.INIT
        status = GameStatus.Started
    }

    fun decideMoves(move1: Move, move2: Move) {
        p1Move = move1
        p2Move = move2
        status = GameStatus.Pending
    }

    fun findWinner() {
        status = when {
            beats(p1Move, p2Move) -> GameStatus.Winner(Player.PLAYER1)
            beats(p2Move, p1Move) -> GameStatus.Winner(Player.PLAYER2)
            else -> GameStatus.Draw
        }
    }

    private fun beats(m1: Move, m2: Move): Boolean = when {
        m1 == Move.ROCK && m2 == Move.SCISSORS -> true
        m1 == Move.PAPER && m2 == Move.ROCK -> true
        m1 == Move.SCISSORS && m2 == Move.PAPER -> true
        else -> false
    }
}
