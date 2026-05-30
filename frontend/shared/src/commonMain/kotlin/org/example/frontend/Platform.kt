package org.example.frontend

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform