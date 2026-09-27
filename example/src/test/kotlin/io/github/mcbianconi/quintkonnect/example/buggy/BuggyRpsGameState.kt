package io.github.mcbianconi.quintkonnect.example.buggy

import io.github.mcbianconi.quintkonnect.TypedState
import io.github.mcbianconi.quintkonnect.example.rockpaperscissors.RockPaperScissorsSpec
import io.github.mcbianconi.quintkonnect.example.rockpaperscissors.toSpec
import kotlinx.serialization.serializer

class BuggyRpsGameState : TypedState<BuggyRockPaperScissorsDriver, RockPaperScissorsSpec.State>(serializer()) {
    override fun extractFromDriver(driver: BuggyRockPaperScissorsDriver): RockPaperScissorsSpec.State =
        RockPaperScissorsSpec.State(
            p1State = driver.game.p1Move.toSpec(),
            p2State = driver.game.p2Move.toSpec(),
            status = driver.game.status.toSpec(),
        )
}
