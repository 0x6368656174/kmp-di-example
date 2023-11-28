package com.example.kmpdiexample

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform