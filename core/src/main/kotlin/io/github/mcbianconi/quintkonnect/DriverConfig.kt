package io.github.mcbianconi.quintkonnect

data class DriverConfig(
    val statePath: List<String> = emptyList(),
    val nondetPath: List<String> = emptyList(),
)
