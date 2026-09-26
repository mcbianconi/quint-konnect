@file:OptIn(ExperimentalSerializationApi::class)

package io.github.mcbianconi.quintkonnect.example.partialstate

import io.github.mcbianconi.quintkonnect.TypedState
import io.github.mcbianconi.quintkonnect.annotations.QuintIgnore
import io.github.mcbianconi.quintkonnect.example.rockpaperscissors.GameStatusSer
import io.github.mcbianconi.quintkonnect.example.rockpaperscissors.Move
import io.github.mcbianconi.quintkonnect.example.rockpaperscissors.MoveSer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

/**
 * Projects [PartialStateRpsDriver]'s state onto the two players' moves only: [status] is
 * `@QuintIgnore`d and always reported as [GameStatusSer.Started] regardless of the actual game
 * status, to demonstrate that an ignored field never fails [TypedState.check] even though its value
 * plainly disagrees with the spec once a round is played.
 */
@Serializable
data class PartialRpsState(
    val p1State: MoveSer,
    val p2State: MoveSer,
    @QuintIgnore val status: GameStatusSer,
)

class PartialRpsGameState : TypedState<PartialStateRpsDriver, PartialRpsState>(serializer()) {
    override fun extractFromDriver(driver: PartialStateRpsDriver): PartialRpsState =
        PartialRpsState(
            p1State = driver.game.p1Move.toSer(),
            p2State = driver.game.p2Move.toSer(),
            status = GameStatusSer.Started,
        )

    private fun Move.toSer(): MoveSer = when (this) {
        Move.INIT -> MoveSer.Init
        Move.ROCK -> MoveSer.Rock
        Move.PAPER -> MoveSer.Paper
        Move.SCISSORS -> MoveSer.Scissors
    }
}
