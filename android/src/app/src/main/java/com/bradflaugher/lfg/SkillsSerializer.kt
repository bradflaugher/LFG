package com.bradflaugher.lfg

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.bradflaugher.lfg.proto.Skills
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

  override suspend fun writeTo(
    t: Skills,
    output: OutputStream,
  ) = t.writeTo(output)
}
