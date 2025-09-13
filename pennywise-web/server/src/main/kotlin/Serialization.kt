package com.example

import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*

fun Application.configureSerialization() {
    install(ContentNegotiation) { gson { } }
}
