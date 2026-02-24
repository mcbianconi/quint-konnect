package io.github.mcbianconi.quintkonnect.example.rockpaperscissors

import io.github.mcbianconi.quintkonnect.Driver
import io.github.mcbianconi.quintkonnect.State
import io.github.mcbianconi.quintkonnect.Step
import io.github.mcbianconi.quintkonnect.annotations.QuintAction
import io.github.mcbianconi.quintkonnect.annotations.QuintRun

@QuintRun(
    spec = "src/test/resources/rock-paper-scissors.qnt",
    maxSamples = 5,
)
class RockPaperScissorsDriver : Driver {
    val game = RockPaperScissors()

    override fun step(step: Step) = generatedStep(step)

    override fun quintState(): State<*> = RpsGameState()

    @QuintAction("init")
    fun init() = game.init()

    @QuintAction("decide_moves")
    fun decideMoves(move1: MoveSer, move2: MoveSer) =
        game.decideMoves(move1.toMove(), move2.toMove())

    @QuintAction("find_winner")
    fun findWinner() = game.findWinner()

    @QuintAction("restart")
    fun restart() = game.init()
}

private fun MoveSer.toMove(): Move = when (this) {
    MoveSer.Init -> Move.INIT
    MoveSer.Rock -> Move.ROCK
    MoveSer.Paper -> Move.PAPER
    MoveSer.Scissors -> Move.SCISSORS
}
