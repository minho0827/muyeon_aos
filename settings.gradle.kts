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
// ★ 2026-11-07 탭 네이티브 전환 — PaceERA 와 같은 계층 모듈(:domain ← :network ← :data ← :app). docs/ARCHITECTURE.md
include(":domain", ":network", ":data")
