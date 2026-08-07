# HealthCareDiet AOS

### Technical Requirements

* Kotlin: 2.1.20

* Android Studio: 2025.1.1 Patch 1

* Target AOS: 35

* UI Framework: Jetpack Compose

### Architectural pattern

The project uses the MVVM-Combine (Model - View - ViewModel) pattern:

* Model: Defines data, requests/responses to the API.

* View: User interface (Jetpack Compose View).

* ViewModel: Handles logic, state, data binding between Model and View.

### Libraries used

#### Serialization:

* Gson (**com.google.code.gson:gson:2.10.1**): Converts JSON to Kotlin/Java objects.

* Retrofit Gson Converter (**com.squareup.retrofit2:converter-gson:2.9.0**): A Retrofit converter
  that automatically converts JSON responses to objects using Gson.

#### Camera & QR Scanning:

* CameraX (**androidx.camera:camera-core:1.4.2**, **camera-camera2:1.4.2**, **camera-lifecycle:1.4.2**,
  **camera-view:1.4.2**): A Jetpack support library that simplifies camera development, providing a consistent and easy-to-use API surface across different Android versions and devices. It is used here for handling the camera feed and preview.

* Google ML Kit Barcode Scanning (**com.google.mlkit:barcode-scanning:17.3.0**):Provides fast and accurate on-device barcode and QR code recognition. It
processes the camera image frames to detect and extract data from various barcode formats.

* Kotlinx Serialization JSON (**org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0**): Included in your list, though its usage may overlap with Gson

#### MVVM & Coroutines:

* ViewModel KTX (**androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2**): Provides viewModelScope for
  coroutines and other Kotlin extensions for ViewModels.

* LiveData KTX (**androidx.lifecycle:lifecycle-livedata-ktx:2.6.2**): Offers coroutine-friendly
  extensions for LiveData, like liveData { }.

* Coroutines Play Services (**org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0**): Adds
  coroutine support for Google Play Services APIs.

#### Jetpack Compose UI:

* UI Core (**androidx.compose.ui:ui**): The core library for building UIs with Jetpack Compose.

* UI Graphics (**androidx.compose.ui:ui-graphics**): Provides APIs for drawing and handling graphics
  in Compose.

* UI Tooling (**androidx.compose.ui:ui-tooling-preview**): Allows for previewing Composables in
  Android Studio.

* Material Icons Extended (**androidx.compose.material:material-icons-extended**): A complete set of
  Material Design icons for Compose.

* Compose LiveData (**androidx.compose.runtime:runtime-livedata:1.6.8**): A bridge to observe
  LiveData directly within Composables using observeAsState().

#### AndroidX & Utilities:

* Activity KTX (**androidx.activity:activity-ktx:1.8.0**): Provides Kotlin extensions for Activity,
  including registerForActivityResult.

* Security Crypto (**androidx.security:security-crypto:1.1.0-alpha06**): A library for securely
  storing data in SharedPreferences or files.

* Material Components (**com.google.android.material:material:1.10.0**): The traditional Android
  Material Components library for Views.

#### Navigation & Services:

* Navigation Compose (**androidx.navigation:navigation-compose:2.7.7**): The navigation library for
  Jetpack Compose apps.

* Play Services Location (**com.google.android.gms:play-services-location:21.3.0**): Provides APIs
  for tracking and retrieving device location.

### Project structure

```
brycensolution_aos
├── .gradle/                                    # Gradle build system files and configurations.
├── .idea/                                      # IntelliJ IDEA project files.
├── .kotlin/                                    # Kotlin-specific project configurations.
├── app/                                        # Contains the main application source code and resources.
│   ├── build/                                  # Output directory for build artifacts.
│   └── src/                                    # Source root for the app.
│       └── main/                               # Main source set for the application.
│           ├── assets/                         # Static assets, like local files or web content.
│           │   └── images/                     # Image resources
│           │   
│           ├── java/                           # Contains the Java/Kotlin source code.
│           │   └── com.bkr_healthcarediet/
│           │       ├── activity/               # Main application activities (e.g., SplashActivity, NotificationActivity).
│           │       ├── common_components/      # Common, reusable UI components for the app.
│           │       ├── data/                   # Data layer: handles data sources and repositories.
│           │       │   ├── api_endpoint/       # API service definitions and related classes.
│           │       │   ├── models/             # Data models for requests, responses, and local data.
│           │       │   └── repository/         # Implementation of data repositories (e.g., AuthRepository, TokenRepository).
│           │       ├── domain/                 # Domain layer: contains core business logic.
│           │       │   ├── models/             # Core business entities and domain models.
│           │       │   ├── repositories/       # Interfaces (contracts) for the data layer (e.g., AuthRepository).
│           │       │   └── use_cases/          # Business logic operations (e.g., LoginUseCase).
│           │       ├── routers/                # Manages app navigation and routing.
│           │       ├── theme/                  # Defines the app's color scheme and typography.
│           │       ├── ui/                     # UI layer: contains screens, view models, and UI logic.
│           │       └── utils/                  # General utility classes and helper functions.
│           └── res/                            # Application resources: layouts, drawables, etc.
│               └── *.lproj/                    # Multilingual localization files.
├── build/                                      # Global build output directory.
├── build.gradle.kts                            # Top-level build script for the project (Kotlin DSL).
├── gradle/                                     # Gradle wrapper files.
├── gradlew                                     # Gradle wrapper script for Unix-like systems.
├── gradlew.bat                                 # Gradle wrapper script for Windows.
├── local.properties                            # Local environment settings for Android SDK.
├── proguard-rules.pro                          # ProGuard rules to shrink, optimize, and obfuscate code.
├── README.md                                   # Project description and setup instructions.
├── settings.gradle.kts                         # Defines settings for the project, including modules.
└── .gitignore                                  # Specifies intentionally untracked files to ignore by Git.
```

### Description of main folders

* activity/: Main application activities (e.g., SplashActivity, NotificationActivity).
* common_components/: Common, reusable UI components for the app.
* data/: Data layer: handles data sources and repositories.
* api_endpoint/: API service definitions and related classes.
* models/: Data models for requests, responses, and local data.
* repository/: Implementation of data repositories (e.g., AuthRepository, TokenRepository).
* domain/: Domain layer: contains core business logic.
* models/: Core business entities and domain models.
* repositories/: Interfaces (contracts) for the data layer (e.g., AuthRepository).
* use_cases/: Business logic operations (e.g., LoginUseCase).
* routers/: Manages app navigation and routing.
* theme/: Defines the app's color scheme and typography.
* ui/: UI layer: contains screens, view models, and UI logic.
* utils/: General utility classes and helper functions.

### Build instructions

    1. Open brycensolution_aos file with Android Studio 2025.1.1 Patch 1 or later.
    2. Android Studio will automatically sync Gradle and download the necessary libraries. This process might take a few minutes.
    3. With a device selected (physical or virtual), simply click the green Run button shaped like a triangle on the toolbar (or press Ctrl+F10).