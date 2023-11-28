package com.example.kmpdiexample.di

import com.example.kmpdiexample.services.Analytic
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.dsl.module

typealias NativeInjectionFactory<T> = Scope.() -> T

fun makeNativeModule(
    analytic: NativeInjectionFactory<Analytic>
): Module {
    return module {
        single { analytic() }
    }
}

