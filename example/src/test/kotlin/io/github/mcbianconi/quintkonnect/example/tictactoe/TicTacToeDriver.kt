package io.github.mcbianconi.quintkonnect.example.tictactoe

import io.github.mcbianconi.quintkonnect.Driver
import io.github.mcbianconi.quintkonnect.State
import io.github.mcbianconi.quintkonnect.Step
import io.github.mcbianconi.quintkonnect.annotations.QuintAction
import io.github.mcbianconi.quintkonnect.annotations.QuintRun

@QuintRun(
    spec = "src/test/resources/tictactoe.qnt",
    maxSamples = 5,
)
class TicTacToeDriver : Driver {
    val game = TicTacToe()

    override fun step(step: Step) = generatedStep(step)

    override fun quintState(): State<*> = TicTacToeState()

    @QuintAction("init")
    fun init() {
        game.board.forEach { row -> row.fill(null) }
        game.nextTurn = Player.X
    }

    @QuintAction("MoveX")
    fun moveX(corner: List<Long>?, coordinate: List<Long>?) {
        val coord = (corner ?: coordinate)?.let { Pair(it[0], it[1]) } ?: Pair(2L, 2L)
        game.movePlayerTo(Player.X, coord)
    }

    @QuintAction("MoveO")
    fun moveO(coordinate: List<Long>) {
        game.movePlayerTo(Player.O, Pair(coordinate[0], coordinate[1]))
    }

    @QuintAction("stuttered")
    fun stuttered() {
        // game is over, no-op
    }
}
