package com.example.kmpdiexample.di

import com.example.kmpdiexample.services.Greeting
import com.example.kmpdiexample.services.Logger
import com.example.kmpdiexample.services.Platform
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * The Koin module that contains all DI services, ViewModules, etc.,
 * that provided in Shared Module.
 */
internal val sharedModule: Module = module {
    singleOf(::Logger)
    singleOf(::Platform)
    singleOf(::Greeting)
}