package io.github.mcbianconi.quintkonnect.example.rockpaperscissors

import io.github.mcbianconi.quintkonnect.Driver
import io.github.mcbianconi.quintkonnect.Step
import io.github.mcbianconi.quintkonnect.annotations.QuintAction
import io.github.mcbianconi.quintkonnect.annotations.QuintRun

@QuintRun(
    spec = "src/test/resources/rock-paper-scissors.qnt",
    maxSamples = 5,
)
class RockPaperScissorsDriver: Driver {

    val game = RockPaperScissors()

    override fun step(step: Step) {
        TODO("Not yet implemented")
    }

    @QuintAction("init")
    fun init() { /* reset state */}

    @QuintAction("decide_moves")
    fun decideMoves() {}

    @QuintAction("find_winner")
    fun findWinner() {
        game.decideWinner()
    }


}