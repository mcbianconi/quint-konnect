module rock_paper_scissors {

    type Player = PLAYER1 | PLAYER2
    type Move = Init | Rock | Paper | Scissors
    type GameStatus = Started | Pending | Draw | Winner(Player)

    var p1State: Move
    var p2State: Move
    var status: GameStatus

    def beats(m1: Move, m2: Move): bool = or {
        (m1 == Rock and m2 == Scissors),
        (m1 == Paper and m2 == Rock),
        (m1 == Scissors and m2 == Paper),
    }

    /// Initialize the game with both players in 'Init' state and game status as 'Started'
    action init = all {
        p1State' = Init,
        p2State' = Init,
        status' = Started
    }

    /// Randomly decide moves for both players; enabled only when game is in 'Started' state
    action decide_moves = {
        nondet move1 = Set(Rock, Paper, Scissors).oneOf()
        nondet move2 = Set(Rock, Paper, Scissors).oneOf()
        all {
            status == Started,
            p1State' = move1,
            p2State' = move2,
            status' = Pending,
        }
    }

    /// Determine the winner based on current moves; enabled only when game is 'Pending'
    action find_winner = all {
        status == Pending,
        p1State' = p1State,
        p2State' = p2State,
        if (beats(p1State, p2State)) status' = Winner(PLAYER1)
        else if (beats(p2State, p1State)) status' = Winner(PLAYER2)
        else status' = Draw,
    }

    /// Reset to a new round; enabled once a result (Draw or Winner) has been reached
    action restart = all {
        not(status == Started or status == Pending),
        p1State' = Init,
        p2State' = Init,
        status' = Started,
    }

    action step = any {
        decide_moves,
        find_winner,
        restart,
    }

    /// Invariant to check if the game status matches the expected winner
    val winInv = and {
        status == Winner(PLAYER1) implies beats(p1State, p2State),
        status == Winner(PLAYER2) implies beats(p2State, p1State),
    }
}
