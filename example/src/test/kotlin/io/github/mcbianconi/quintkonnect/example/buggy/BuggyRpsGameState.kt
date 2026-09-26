package io.github.mcbianconi.quintkonnect.example.buggy

import io.github.mcbianconi.quintkonnect.TypedState
import io.github.mcbianconi.quintkonnect.example.rockpaperscissors.GameStatus
import io.github.mcbianconi.quintkonnect.example.rockpaperscissors.GameStatusSer
import io.github.mcbianconi.quintkonnect.example.rockpaperscissors.Move
import io.github.mcbianconi.quintkonnect.example.rockpaperscissors.MoveSer
import io.github.mcbianconi.quintkonnect.example.rockpaperscissors.Player
import io.github.mcbianconi.quintkonnect.example.rockpaperscissors.PlayerSer
import io.github.mcbianconi.quintkonnect.example.rockpaperscissors.RpsState
import kotlinx.serialization.serializer

class BuggyRpsGameState : TypedState<BuggyRockPaperScissorsDriver, RpsState>(serializer()) {
    override fun extractFromDriver(driver: BuggyRockPaperScissorsDriver): RpsState =
        RpsState(
            p1State = driver.game.p1Move.toSer(),
            p2State = driver.game.p2Move.toSer(),
            status = driver.game.status.toSer(),
        )

    private fun Move.toSer(): MoveSer = when (this) {
        Move.INIT -> MoveSer.Init
        Move.ROCK -> MoveSer.Rock
        Move.PAPER -> MoveSer.Paper
        Move.SCISSORS -> MoveSer.Scissors
    }

    private fun GameStatus.toSer(): GameStatusSer = when (this) {
        is GameStatus.Started -> GameStatusSer.Started
        is GameStatus.Pending -> GameStatusSer.Pending
        is GameStatus.Draw -> GameStatusSer.Draw
        is GameStatus.Winner -> GameStatusSer.Winner(player.toSer())
    }

    private fun Player.toSer(): PlayerSer = when (this) {
        Player.PLAYER1 -> PlayerSer.PLAYER1
        Player.PLAYER2 -> PlayerSer.PLAYER2
    }
}
