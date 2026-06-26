/*
 * LFG — Uncensored on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the MIT License.
 * See LICENSE in the project root for terms.
 *
 * Includes code adapted from Google AI Edge Gallery (Apache 2.0,
 * Copyright 2025 Google LLC) — https://github.com/google-ai-edge/gallery.
 */
package com.bradflaugher.lfg.data

import androidx.datastore.core.DataStore
import com.bradflaugher.lfg.proto.AccessTokenData
import com.bradflaugher.lfg.proto.ImportedModel
import com.bradflaugher.lfg.proto.Settings
import com.bradflaugher.lfg.proto.Skill
import com.bradflaugher.lfg.proto.Skills
import com.bradflaugher.lfg.proto.Theme
import com.bradflaugher.lfg.proto.UserData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// TODO(b/423700720): Change to async (suspend) functions
interface DataStoreRepository {
  fun saveTextInputHistory(history: List<String>)

  fun readTextInputHistory(): List<String>

  fun saveTheme(theme: Theme)

  fun readTheme(): Theme

  fun saveSecret(
    key: String,
    value: String,
  )

  fun readSecret(key: String): String?

  fun deleteSecret(key: String)

  fun saveAccessTokenData(
    accessToken: String,
    refreshToken: String,
    expiresAt: Long,
  )

  fun clearAccessTokenData()

  fun readAccessTokenData(): AccessTokenData?

  fun saveImportedModels(importedModels: List<ImportedModel>)

  fun readImportedModels(): List<ImportedModel>

  fun isTosAccepted(): Boolean

  fun acceptTos()

  fun isGemmaTermsOfUseAccepted(): Boolean

  fun acceptGemmaTermsOfUse()

  fun addSkill(skill: Skill)

  fun setSkills(skills: List<Skill>)

  fun setSkillSelected(
    skill: Skill,
    selected: Boolean,
  )

  fun setAllSkillsSelected(selected: Boolean)

  fun getAllSkills(): List<Skill>

  fun deleteSkill(name: String)

  suspend fun deleteSkills(names: Set<String>)
}

/** Repository for managing data using Proto DataStore. */
class DefaultDataStoreRepository(
  private val dataStore: DataStore<Settings>,
  private val userDataDataStore: DataStore<UserData>,
  private val skillsDataStore: DataStore<Skills>,
) : DataStoreRepository {
  override fun saveTextInputHistory(history: List<String>) {
    runBlocking {
      dataStore.updateData { settings ->
        settings.toBuilder().clearTextInputHistory().addAllTextInputHistory(history).build()
      }
    }
  }

  override fun readTextInputHistory(): List<String> {
    return runBlocking {
      val settings = dataStore.data.first()
      settings.textInputHistoryList
    }
  }

  override fun saveTheme(theme: Theme) {
    runBlocking {
      dataStore.updateData { settings -> settings.toBuilder().setTheme(theme).build() }
    }
  }

  override fun readTheme(): Theme {
    return runBlocking {
      val settings = dataStore.data.first()
      val curTheme = settings.theme
      // Use "auto" as the default theme.
      if (curTheme == Theme.THEME_UNSPECIFIED) Theme.THEME_AUTO else curTheme
    }
  }

  override fun saveSecret(
    key: String,
    value: String,
  ) {
    runBlocking {
      userDataDataStore.updateData { userData ->
        userData.toBuilder().putSecrets(key, value).build()
      }
    }
  }

  override fun readSecret(key: String): String? {
    return runBlocking { userDataDataStore.data.first().secretsMap[key] }
  }

  override fun deleteSecret(key: String) {
    runBlocking {
      userDataDataStore.updateData { userData -> userData.toBuilder().removeSecrets(key).build() }
    }
  }

  override fun saveAccessTokenData(
    accessToken: String,
    refreshToken: String,
    expiresAt: Long,
  ) {
    runBlocking {
      // Clear the entry in old data store.
      dataStore.updateData { settings ->
        settings.toBuilder().setAccessTokenData(AccessTokenData.getDefaultInstance()).build()
      }

      userDataDataStore.updateData { userData ->
        userData
          .toBuilder()
          .setAccessTokenData(
            AccessTokenData.newBuilder()
              .setAccessToken(accessToken)
              .setRefreshToken(refreshToken)
              .setExpiresAtMs(expiresAt)
              .build(),
          )
          .build()
      }
    }
  }

  override fun clearAccessTokenData() {
    runBlocking {
      dataStore.updateData { settings -> settings.toBuilder().clearAccessTokenData().build() }
      userDataDataStore.updateData { userData ->
        userData.toBuilder().clearAccessTokenData().build()
      }
    }
  }

  override fun readAccessTokenData(): AccessTokenData? {
    return runBlocking {
      val userData = userDataDataStore.data.first()
      userData.accessTokenData
    }
  }

  override fun saveImportedModels(importedModels: List<ImportedModel>) {
    runBlocking {
      dataStore.updateData { settings ->
        settings.toBuilder().clearImportedModel().addAllImportedModel(importedModels).build()
      }
    }
  }

  override fun readImportedModels(): List<ImportedModel> {
    return runBlocking {
      val settings = dataStore.data.first()
      settings.importedModelList
    }
  }

  override fun isTosAccepted(): Boolean {
    return runBlocking {
      val settings = dataStore.data.first()
      settings.isTosAccepted
    }
  }

  override fun acceptTos() {
    runBlocking {
      dataStore.updateData { settings -> settings.toBuilder().setIsTosAccepted(true).build() }
    }
  }

  override fun isGemmaTermsOfUseAccepted(): Boolean {
    return runBlocking {
      val settings = dataStore.data.first()
      settings.isGemmaTermsAccepted
    }
  }

  override fun acceptGemmaTermsOfUse() {
    runBlocking {
      dataStore.updateData { settings ->
        settings.toBuilder().setIsGemmaTermsAccepted(true).build()
      }
    }
  }

  override fun addSkill(skill: Skill) {
    runBlocking {
      skillsDataStore.updateData { skills ->
        val newSkills =
          buildList {
            add(skill)
            addAll(skills.skillList)
          }
        skills.toBuilder().clearSkill().addAllSkill(newSkills).build()
      }
    }
  }

  override fun setSkills(skills: List<Skill>) {
    runBlocking {
      skillsDataStore.updateData { curSkills ->
        curSkills.toBuilder().clearSkill().addAllSkill(skills).build()
      }
    }
  }

  override fun setSkillSelected(
    skill: Skill,
    selected: Boolean,
  ) {
    runBlocking {
      skillsDataStore.updateData { skills ->
        val newSkills =
          skills.skillList.map { curSkill ->
            if (curSkill.name == skill.name) {
              curSkill.toBuilder().setSelected(selected).setUserModifiedSelection(true).build()
            } else {
              curSkill
            }
          }
        Skills.newBuilder().addAllSkill(newSkills).build()
      }
    }
  }

  override fun setAllSkillsSelected(selected: Boolean) {
    runBlocking {
      skillsDataStore.updateData { skills ->
        val newSkills =
          skills.skillList.map { curSkill ->
            curSkill.toBuilder().setSelected(selected).setUserModifiedSelection(true).build()
          }
        Skills.newBuilder().addAllSkill(newSkills).build()
      }
    }
  }

  override fun getAllSkills(): List<Skill> {
    return runBlocking { skillsDataStore.data.first().skillList }
  }

  override fun deleteSkill(name: String) {
    runBlocking {
      skillsDataStore.updateData { skills ->
        val newSkills = skills.skillList.filter { it.name != name }
        Skills.newBuilder().addAllSkill(newSkills).build()
      }
    }
  }

  override suspend fun deleteSkills(names: Set<String>) {
    skillsDataStore.updateData { skills ->
      val newSkills = skills.skillList.filter { it.name !in names }
      skills.toBuilder().clearSkill().addAllSkill(newSkills).build()
    }
  }
}
