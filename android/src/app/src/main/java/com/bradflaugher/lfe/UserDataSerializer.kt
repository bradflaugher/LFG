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
import com.bradflaugher.lfe.proto.UserData
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object UserDataSerializer : Serializer<UserData> {
  override val defaultValue: UserData = UserData.getDefaultInstance()

  override suspend fun readFrom(input: InputStream): UserData {
    try {
      return UserData.parseFrom(input)
    } catch (exception: InvalidProtocolBufferException) {
      throw CorruptionException("Cannot read proto.", exception)
    }
  }

  override suspend fun writeTo(t: UserData, output: OutputStream) = t.writeTo(output)
}
