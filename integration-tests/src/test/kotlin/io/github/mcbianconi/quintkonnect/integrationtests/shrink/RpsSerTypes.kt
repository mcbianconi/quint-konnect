@file:OptIn(ExperimentalSerializationApi::class)

package io.github.mcbianconi.quintkonnect.integrationtests.shrink

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("tag")
sealed class PlayerSer {
    @Serializable
    @SerialName("PLAYER1")
    data object PLAYER1 : PlayerSer()

    @Serializable
    @SerialName("PLAYER2")
    data object PLAYER2 : PlayerSer()
}

@Serializable
@JsonClassDiscriminator("tag")
sealed class MoveSer {
    @Serializable
    @SerialName("Init")
    data object Init : MoveSer()

    @Serializable
    @SerialName("Rock")
    data object Rock : MoveSer()

    @Serializable
    @SerialName("Paper")
    data object Paper : MoveSer()

    @Serializable
    @SerialName("Scissors")
    data object Scissors : MoveSer()
}

@Serializable
@JsonClassDiscriminator("tag")
sealed class GameStatusSer {
    @Serializable
    @SerialName("Started")
    data object Started : GameStatusSer()

    @Serializable
    @SerialName("Pending")
    data object Pending : GameStatusSer()

    @Serializable
    @SerialName("Draw")
    data object Draw : GameStatusSer()

    @Serializable
    @SerialName("Winner")
    data class Winner(val value: PlayerSer) : GameStatusSer()
}

@Serializable
data class RpsState(
    val p1State: MoveSer,
    val p2State: MoveSer,
    val status: GameStatusSer,
)
