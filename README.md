# Kotlin Multiplatform dependency injection with pure native services

I'm the tech lead of a mobile development team, and it's my approach to how to use dependency injection in the Kotlin Muiltiplatform project with [Koin](https://insert-koin.io/) library that allow you to write pure native iOS/Swift and Android/Kotlin services.

The main new idea of this manual is that we are trying to write all native features directly in native code without using Kotlin Multiplatform native interpolation. We declare native services in iOS and Android apps and pass them to the shared code through Koin dependency injection. It allows us to use general libraries documentation with native snippets in Kotlin and Swift, and we don't have any restrictions from Kotlin Multiplatform, like not supporting Swift.

## Preparing

In this manual, I will use the default Kotlin Multiplatform project that was created by Android Studio. You could find the documentation about how to create a project from scratch in the [official documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-create-first-app.html).

After creating the default project, we need to install Koin and add the necessary dependencies to shared and Android projects. For it, let's add the Koin dependencies version to `gradle/libs.versions.toml`.

```toml
# gradle/libs.versions.toml

[versions]
koin = "3.5.0"

[libraries]
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
```

Then we need to use Koin in shared project.

```kotlin
// shared/build.gradle.kts

kotlin {
  sourceSets {
    commonMain.dependencies {
      implementation(libs.koin.core)
    }
  }
}
```

Also, we need Koin in our Android project too.

```kotlin
// androidApp/build.gradle.kts

dependencies {
  implementation(libs.koin.android)
}
```

## Creating services

In this manual, I will try to cover all the types of services that you will usually use in your project:
- services that have only one multiplatform code, written in the shared project and do not use any native features.
- services that use some native features and could be easily written in a standard Kotlin Multiplatform way by splitting the `expect` declaration and a few `actual` implementations.
- services that use some native features, but they should be written in native code, separately in Swift for iOS and Kotlin for Android.

### Pure shared services

Let's create a few services that don't use native features directly (they still may use native features, but only through other services). It will be two services: `Logger` and `Greeting`.

The `Logger` service will not depend on any other services and will just print the given string to the console.

```kotlin
// shared/src/commonMain/kotlin/com/example/kmpdiexample/services/Logger.kt

class Logger {
  fun log(text: String) {
    println("[SHARED LOGGER]: $text")
  }
}
```

The `Greeting` service will depend on two other services: `Platform` and `Analytic`. You could see that to use another service, we just need to pass their constructor parameter. It's how Koin DI works.

```kotlin
// shared/src/commonMain/kotlin/com/example/kmpdiexample/services/Greeting.kt

class Greeting(private val platform: Platform, private val analytic: Analytic) {
  fun greet(): String {
    analytic.logEvent("greet-requested")

    return "Hello, ${platform.name}!"
  }
}
```

### Pure Kotlin Multiplatform native services

If our app needs some service that should use some native feature, the [Kotlin documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-connect-to-apis.html) recommends using `expect` and `actual` declarations. It could be okay in easy cases, but it has a lot of restrictions, like a lack of support for Swift libraries and problems with documentation. But sometimes it's a good way to do native services. For example, it can be useful if you use a real multiplatform library like [ktor](https://ktor.io/) or [multiplatform-settings](https://github.com/russhwolf/multiplatform-settings) and need to configure it to use different engines on Android and iOS. But if you are going to use non-multiplatform libraries, I recommend using the third approach with pure native services.

Let's implement a `Platform` service that will use Kotlin Multiplatform native interaction. It should be pretty familiar to Kotlin Multiplatform users.

At first, we need to describe the expected service.

```kotlin
// shared/src/commonMain/kotlin/com/example/kmpdiexample/services/Platform.kt

expect class Platform {
  val name: String
}
```

Then, we should write actual Android implementations of the service.

```kotlin
// shared/src/androidMain/kotlin/com/example/kmpdiexample/services/Platform.kt

actual class Platform {
  actual val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}
```

And iOS implementation of the service.

```kotlin
// shared/src/iosMain/kotlin/com/example/kmpdiexample/services/Platform.kt

actual class Platform {
  actual val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}
```

### Pure native services

If you need to use some library that isn't supported by Kotlin Mulitplatform, it's better to keep this library usage pure native. With Koin DI, you could declare a library interface in the shared project and move implementations to real native iOS and Android code. It allows you to use Swift for iOS, plus it allows you to easily apply the library documentation to your code and easily update libraries without spending hours trying to understand how to communicate with native code from Kotlin Multiplatform.

In this manual, we will not install any 3rd party libraries, but let's imagine that we need to implement an `Analytic` service that could use any native library like [Firebase Analytic](https://firebase.google.com/docs/analytics) or [Segment](https://segment.com/).

To do it, we need to declare our library.

```kotlin
// shared/src/commonMain/kotlin/com/example/kmpdiexample/services/Analytic.kt

interface Analytic {
  fun logEvent(event: String)
}
```

Then, we should implement the library in the Android app code.

```kotlin
// androidApp/src/main/java/com/example/kmpdiexample/android/services/AnalyticImpl.kt

class AnalyticImpl(private val logger: Logger): Analytic {
  override fun logEvent(event: String) {
    logger.log("Event \"$event\" sent to analytic by Android implementation")
  }
}
```

Also, we should provide iOS implementations in Swift code.

```swift
// iosApp/iosApp/Services/AnalyticImpl.swift

class AnalyticImpl: Analytic {
  private let logger: Logger

  init(logger: Logger) {
    self.logger = logger
  }

  func logEvent(event: String) {
    logger.log(text: "Event \"\(event)\" sent to analytic by iOS implementation")
  }
}
```

## Configuring dependency injection

After creating all services and providing their code, we should add them to Koin modules to allow Koin to inject them.

First of all, we should add all services that are fully available in the shared project to Koin `sharedModule` Koin module.

```kotlin
// shared/src/commonMain/kotlin/com/example/kmpdiexample/di/sharedModule.kt

val sharedModule: Module = module {
  singleOf(::Logger)
  singleOf(::Platform)
  singleOf(::Greeting)
}
```

Then we should create a function that will be run in the native app when the app is started. This function should configure our DI. This function will start Koin DI with our `sharedModule` and `nativeModule` (this module will be passed directly from native code).

```kotlin
// shared/src/commonMain/kotlin/com/example/kmpdiexample/di/startDI.kt

fun startDI(nativeModule: Module, appDeclaration: KoinAppDeclaration = {}) {
  startKoin {
    appDeclaration()

    modules(nativeModule, sharedModule)
  }
}
```

Because we have services like `Analytic` whose implementation is not available in the shared project, we should configure them in native codes. For it, we may create a function `makeNativeModule`, that will expect `Analytic` implementation and return Koin module with this service.

```kotlin
// shared/src/commonMain/kotlin/com/example/kmpdiexample/di/makeNativeModule.kt

typealias NativeInjectionFactory<T> = Scope.() -> T

fun makeNativeModule(
  analytic: NativeInjectionFactory<Analytic>
): Module {
  return module {
    single { analytic() }
  }
}
```

The last part is starting our DI from Android and iOS.

In Android, we need to create `nativeModule`.

```kotlin
// androidApp/src/main/java/com/example/kmpdiexample/android/di/nativeModule.kt

val nativeModule = makeNativeModule(
  analytic = { AnalyticImpl( get() ) }
)
```

Then we should use it in `startDI` function.

```kotlin
// androidApp/src/main/java/com/example/kmpdiexample/android/MainActivity.kt

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    startDI(nativeModule) { androidContext(this@MainActivity) }
  }
}
```

In iOS we should to do the same things. Firstly, create `nativeModule`.

```swift
// iosApp/iosApp/DI/nativeModule.swift

var nativeModule: Koin_coreModule = MakeNativeModuleKt.makeNativeModule(
  analytic: { scope in
    return AnalyticImpl(logger: scope.get())
  }
)
```

And run `startDI` function.

```swift
// iosApp/iosApp/iOSApp.swift

struct iOSApp: App {
  init() {
    StartDIKt.startDI(
      nativeModule: nativeModule,
      appDeclaration: { _ in }
    )
  }
}
```

That's all. Our dependency injection is ready to use.

## Using dependency injection

In the shared project, as I showed in `Greeting` service, other services may be used just by passing them to constructor parameters.

To use services in Android, we may use the standard Koin Android library `inject` function.

```kotlin
// androidApp/src/main/java/com/example/kmpdiexample/android/MainActivity.kt

class MainActivity : ComponentActivity() {
  private val greeting: Greeting by inject()

  override fun onCreate(savedInstanceState: Bundle?) {
    setContent {
      MyApplicationTheme {
        Surface {
          GreetingView(greeting.greet())
        }
      }
    }
  }
}
```

To use services on iOS, we should create a few helpers.

```kotlin
// shared/src/iosMain/kotlin/com/example/kmpdiexample/di/koinGet.kt

fun <T> koinGet(
  clazz: KClass<*>,
  qualifier: Qualifier? = null,
  parameters: ParametersDefinition? = null
): T {
  val koin = KoinPlatformTools.defaultContext().get()

  return koin.get(clazz, qualifier, parameters)
}
```

```kotlin
// shared/src/iosMain/kotlin/com/example/kmpdiexample/di/SwiftType.kt

@OptIn(BetaInteropApi::class)
data class SwiftType(
  val type: ObjCObject,
  val swiftClazz: KClass<*>,
)

@OptIn(BetaInteropApi::class)
fun SwiftType.getClazz(): KClass<*> =
  when (type) {
    is ObjCClass -> getOriginalKotlinClass(type)
    is ObjCProtocol -> getOriginalKotlinClass(type)
    else -> null
  }
    ?: swiftClazz
```

```swift
// iosApp/iosApp/DI/KoinHelpers.swift

class SwiftKClass<T>: NSObject, KotlinKClass {
  func isInstance(value: Any?) -> Bool { value is T }
  var qualifiedName: String? { String(reflecting: T.self) }
  var simpleName: String? { String(describing: T.self) }
}

func KClass<T>(for type: T.Type) -> KotlinKClass {
  SwiftType(type: type, swiftClazz: SwiftKClass<T>()).getClazz()
}

extension Koin_coreScope {
  func get<T>() -> T {
    // swiftlint:disable force_cast
    get(clazz: KClass(for: T.self), qualifier: nil, parameters: nil) as! T
    // swiftlint:enable force_cast
  }
}

func inject<T>(
  qualifier: Koin_coreQualifier? = nil,
  parameters: (() -> Koin_coreParametersHolder)? = nil
) -> T {
  // swiftlint:disable force_cast
  KoinGetKt.koinGet(clazz: KClass(for: T.self), qualifier: qualifier, parameters: parameters) as! T
  // swiftlint:enable force_cast
}
```

After that, we may use our services like in Android app.

```swift
// iosApp/iosApp/ContentView.swift

struct ContentView: View {
  let greeting: Greeting = inject()

  var body: some View {
    Text(greeting.greet())
  }
}
```
