package io.github.mcbianconi.quintkonnect.example.buggy

import io.github.mcbianconi.quintkonnect.Driver
import io.github.mcbianconi.quintkonnect.State
import io.github.mcbianconi.quintkonnect.Step
import io.github.mcbianconi.quintkonnect.example.rockpaperscissors.RockPaperScissorsSpec
import io.github.mcbianconi.quintkonnect.example.rockpaperscissors.toMove
import io.github.mcbianconi.quintkonnect.nondet.decode

class BuggyRockPaperScissorsDriver : Driver {
    val game = BuggyRockPaperScissors()

    override fun step(step: Step) {
        when (step.actionTaken) {
            "init" -> game.init()

            "decide_moves" -> {
                val move1 = step.nondetPicks.decode<RockPaperScissorsSpec.Move>("move1")
                val move2 = step.nondetPicks.decode<RockPaperScissorsSpec.Move>("move2")
                game.decideMoves(move1.toMove(), move2.toMove())
            }

            "find_winner" -> game.findWinner()

            "restart" -> game.init()

            else -> error("Unimplemented action: ${step.actionTaken}")
        }
    }

    override fun quintState(): State<BuggyRockPaperScissorsDriver> = BuggyRpsGameState()
}
