package com.example.kmpdiexample.android.services

import com.example.kmpdiexample.services.Analytic
import com.example.kmpdiexample.services.Logger

class AnalyticImpl(private val logger: Logger): Analytic {
    override fun logEvent(event: String) {
        logger.log("Event \"$event\" sent to analytic by Android implementation")
    }
}