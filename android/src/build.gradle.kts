/*
 * LFE — Copyright (C) 2026 Brad Flaugher. GPL-3.0-or-later.
 * Includes code adapted from Google AI Edge Gallery (Apache 2.0).
 */

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.hilt.application) apply false
  alias(libs.plugins.ksp) apply false
}

// We lean on Android Lint (./gradlew lint) for CI gating. Style-only linters
// (ktlint, detekt) can be re-added once the codebase stabilizes and we want
// to invest in style enforcement; today the friction isn't worth the value.
