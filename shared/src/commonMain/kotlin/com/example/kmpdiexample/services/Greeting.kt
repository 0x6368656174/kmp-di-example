package com.example.kmpdiexample.services

class Greeting(private val platform: Platform, private val analytic: Analytic) {
    fun greet(): String {
        analytic.logEvent("greet-requested")

        return "Hello, ${platform.name}!"
    }
}
