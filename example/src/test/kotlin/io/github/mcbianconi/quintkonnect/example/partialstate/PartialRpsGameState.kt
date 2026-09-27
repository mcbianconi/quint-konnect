package io.github.mcbianconi.quintkonnect.example.partialstate

import io.github.mcbianconi.quintkonnect.TypedState
import io.github.mcbianconi.quintkonnect.annotations.QuintIgnore
import io.github.mcbianconi.quintkonnect.example.rockpaperscissors.RockPaperScissorsSpec
import io.github.mcbianconi.quintkonnect.example.rockpaperscissors.toSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

/**
 * Projects [PartialStateRpsDriver]'s state onto the two players' moves only: [status] is
 * `@QuintIgnore`d and always reported as [RockPaperScissorsSpec.GameStatus.Started] regardless of
 * the actual game status, to demonstrate that an ignored field never fails [TypedState.check] even
 * though its value plainly disagrees with the spec once a round is played.
 *
 * Hand-written rather than `RockPaperScissorsSpec.State`: `@QuintIgnore` can't go on a generated
 * class (qk-ymex).
 */
@Serializable
data class PartialRpsState(
    val p1State: RockPaperScissorsSpec.Move,
    val p2State: RockPaperScissorsSpec.Move,
    @QuintIgnore val status: RockPaperScissorsSpec.GameStatus,
)

class PartialRpsGameState : TypedState<PartialStateRpsDriver, PartialRpsState>(serializer()) {
    override fun extractFromDriver(driver: PartialStateRpsDriver): PartialRpsState =
        PartialRpsState(
            p1State = driver.game.p1Move.toSpec(),
            p2State = driver.game.p2Move.toSpec(),
            status = RockPaperScissorsSpec.GameStatus.Started,
        )
}
