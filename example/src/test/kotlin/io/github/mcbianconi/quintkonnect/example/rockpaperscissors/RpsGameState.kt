@file:OptIn(ExperimentalSerializationApi::class)

package io.github.mcbianconi.quintkonnect.example.rockpaperscissors

import io.github.mcbianconi.quintkonnect.TypedState
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.serializer

@Serializable
@JsonClassDiscriminator("tag")
sealed class PlayerSer {
    @Serializable @SerialName("PLAYER1") data object PLAYER1 : PlayerSer()
    @Serializable @SerialName("PLAYER2") data object PLAYER2 : PlayerSer()
}

@Serializable
@JsonClassDiscriminator("tag")
sealed class MoveSer {
    @Serializable @SerialName("Init") data object Init : MoveSer()
    @Serializable @SerialName("Rock") data object Rock : MoveSer()
    @Serializable @SerialName("Paper") data object Paper : MoveSer()
    @Serializable @SerialName("Scissors") data object Scissors : MoveSer()
}

@Serializable
@JsonClassDiscriminator("tag")
sealed class GameStatusSer {
    @Serializable @SerialName("Started") data object Started : GameStatusSer()
    @Serializable @SerialName("Pending") data object Pending : GameStatusSer()
    @Serializable @SerialName("Draw") data object Draw : GameStatusSer()
    @Serializable @SerialName("Winner") data class Winner(val value: PlayerSer) : GameStatusSer()
}

@Serializable
data class RpsState(
    val p1State: MoveSer,
    val p2State: MoveSer,
    val status: GameStatusSer,
)

class RpsGameState : TypedState<RockPaperScissorsDriver, RpsState>(serializer()) {
    override fun extractFromDriver(driver: RockPaperScissorsDriver): RpsState =
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
