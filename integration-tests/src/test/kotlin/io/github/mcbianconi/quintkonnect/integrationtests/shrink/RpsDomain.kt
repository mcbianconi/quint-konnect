package io.github.mcbianconi.quintkonnect.integrationtests.shrink

enum class Move { INIT, ROCK, PAPER, SCISSORS }
enum class Player { PLAYER1, PLAYER2 }

sealed class GameStatus {
    object Started : GameStatus()
    object Pending : GameStatus()
    object Draw : GameStatus()
    data class Winner(val player: Player) : GameStatus()
}
