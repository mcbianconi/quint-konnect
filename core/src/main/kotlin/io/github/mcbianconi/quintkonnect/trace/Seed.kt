package io.github.mcbianconi.quintkonnect.trace

fun genSeed(): String =
    System.getenv("QUINT_SEED")
        ?: "0x%x".format((Math.random() * Int.MAX_VALUE).toLong())
