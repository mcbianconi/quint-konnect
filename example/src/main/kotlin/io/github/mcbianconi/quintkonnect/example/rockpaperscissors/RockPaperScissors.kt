package io.github.mcbianconi.quintkonnect.example.rockpaperscissors

import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds


enum class Move {
    ROCK, PAPER, SCISSORS;

    fun beats(other: Move): Boolean {
        return when (this) {
            ROCK -> other == SCISSORS
            PAPER -> other == ROCK
            SCISSORS -> other == PAPER
        }
    }

}

class Player(val name: String) {

    var chosenMove: Move? = null

    suspend fun pickMove(): Move {
        delay(Random.nextLong(100, 500).milliseconds)
        val move = Move.entries.random()
        chosenMove = move
        return move
    }

    override fun toString(): String {
        return name
    }
}

class RockPaperScissors
    (
    val player1: Player = Player("P1"),
    val player2: Player = Player("P2")
) {

    lateinit var winner: Player

    suspend fun pickMoves() {

        val player1Move = player1.pickMove()
        val player2Move = player2.pickMove()

        winner = if (player1Move.beats(player2Move)) player1 else player2
    }



}