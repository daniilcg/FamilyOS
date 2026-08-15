pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FamilyOS"

include(":app")
include(":core")
include(":core_ui")
include(":core_domain")
include(":core_data")
include(":feature_auth")
include(":feature_home")
include(":feature_shopping")
include(":feature_tasks")
include(":feature_calendar")
include(":feature_budget")
include(":feature_documents")
include(":feature_notes")
include(":feature_notifications")
include(":feature_ai")
include(":feature_profile")
include(":feature_family")
include(":feature_settings")
include(":feature_chat")
include(":feature_billing")
