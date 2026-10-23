    pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
        // 네이버 지도 SDK(레슨 개설 위치 미니 지도, 10-23)
        maven { url = uri("https://repository.map.naver.com/archive/maven") }
        flatDir {
            dirs("app/libs")
        }
    }
}

rootProject.name = "muyeon_aos"
include(":app")
