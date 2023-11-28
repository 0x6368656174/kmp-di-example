package com.example.kmpdiexample.services

import platform.UIKit.UIDevice

actual class Platform {
    actual val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}