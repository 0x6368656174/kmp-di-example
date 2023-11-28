package com.example.kmpdiexample.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/** Start Koin DI with given native module and app declaration. */
fun startDI(nativeModule: Module, appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()

        modules(nativeModule, sharedModule)
    }
}