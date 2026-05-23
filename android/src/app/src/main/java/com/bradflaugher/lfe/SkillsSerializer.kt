/*
 * LFE — A low-feature, on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the GNU General Public License v3.0 or later.
 * See LICENSE in the project root for terms.
 *
 * Includes code adapted from Google AI Edge Gallery (Apache 2.0,
 * Copyright 2025 Google LLC) — https://github.com/google-ai-edge/gallery.
 */
package com.bradflaugher.lfe

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.bradflaugher.lfe.proto.Skills
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

/** Serializes and deserializes [Skills] proto messages for use with ProtoDataStore. */
object SkillsSerializer : Serializer<Skills> {
  override val defaultValue: Skills = Skills.getDefaultInstance()

  override suspend fun readFrom(input: InputStream): Skills {
    try {
      return Skills.parseFrom(input)
    } catch (exception: InvalidProtocolBufferException) {
      throw CorruptionException("Cannot read proto.", exception)
    }
  }

  override suspend fun writeTo(t: Skills, output: OutputStream) = t.writeTo(output)
}
