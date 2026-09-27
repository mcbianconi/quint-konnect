package io.github.mcbianconi.quintkonnect.example.rockpaperscissors

import io.github.mcbianconi.quintkonnect.Driver
import io.github.mcbianconi.quintkonnect.State
import io.github.mcbianconi.quintkonnect.annotations.QuintAction
import io.github.mcbianconi.quintkonnect.annotations.QuintRun

@QuintRun(
    spec = "src/test/resources/rock-paper-scissors.qnt",
    maxSamples = 5,
)
class RockPaperScissorsDriver : Driver {
    val game = RockPaperScissors()

    override fun quintState(): State<RockPaperScissorsDriver> = RpsGameState()

    @QuintAction("init")
    fun init() = game.init()

    @QuintAction("decide_moves")
    fun decideMoves(move1: RockPaperScissorsSpec.Move, move2: RockPaperScissorsSpec.Move) =
        game.decideMoves(move1.toMove(), move2.toMove())

    @QuintAction("find_winner")
    fun findWinner() = game.findWinner()

    @QuintAction("restart")
    fun restart() = game.init()
}
