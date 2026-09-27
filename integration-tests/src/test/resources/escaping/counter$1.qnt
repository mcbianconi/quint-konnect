module counter {

    var count: int

    action init = { count' = 0 }

    action increment = { count' = count + 1 }

    action step = any {
        increment,
    }
}
