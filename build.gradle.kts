// Top-level build file — configuration common to all sub-projects.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.room) apply false
  alias(libs.plugins.google.services) apply false
}