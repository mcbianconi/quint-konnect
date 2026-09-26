package io.github.mcbianconi.quintkonnect.example.partialstate

import io.github.mcbianconi.quintkonnect.Driver
import io.github.mcbianconi.quintkonnect.State
import io.github.mcbianconi.quintkonnect.Step
import io.github.mcbianconi.quintkonnect.example.rockpaperscissors.Move
import io.github.mcbianconi.quintkonnect.example.rockpaperscissors.MoveSer
import io.github.mcbianconi.quintkonnect.example.rockpaperscissors.RockPaperScissors
import io.github.mcbianconi.quintkonnect.nondet.decode

/** Same driver as `RockPaperScissorsDriver`, wired to [PartialRpsGameState] instead of `RpsGameState`. */
class PartialStateRpsDriver : Driver {
    val game = RockPaperScissors()

    override fun step(step: Step) {
        when (step.actionTaken) {
            "init" -> game.init()
            "decide_moves" -> {
                val move1 = step.nondetPicks.decode<MoveSer>("move1")
                val move2 = step.nondetPicks.decode<MoveSer>("move2")
                game.decideMoves(move1.toMove(), move2.toMove())
            }
            "find_winner" -> game.findWinner()
            "restart" -> game.init()
            else -> error("Unimplemented action: ${step.actionTaken}")
        }
    }

    override fun quintState(): State<PartialStateRpsDriver> = PartialRpsGameState()
}

private fun MoveSer.toMove(): Move = when (this) {
    MoveSer.Init -> Move.INIT
    MoveSer.Rock -> Move.ROCK
    MoveSer.Paper -> Move.PAPER
    MoveSer.Scissors -> Move.SCISSORS
}
