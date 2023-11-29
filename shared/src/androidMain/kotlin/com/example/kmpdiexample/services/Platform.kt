package com.example.kmpdiexample.services

actual class Platform {
    actual val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}