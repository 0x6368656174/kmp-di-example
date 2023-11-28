package com.example.kmpdiexample.android.di

import com.example.kmpdiexample.android.services.AnalyticImpl
import com.example.kmpdiexample.di.makeNativeModule

val nativeModule = makeNativeModule(
    analytic = { AnalyticImpl( get() ) }
)