package com.bradflaugher.lfg.ui.modelmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bradflaugher.lfg.data.DataStoreRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudProviderDialog(
  dataStoreRepository: DataStoreRepository,
  onDismiss: () -> Unit,
  onSettingsSaved: () -> Unit = {},
) {
  var customDisplayName by remember {
    mutableStateOf(dataStoreRepository.readSecret("CLOUD_DISPLAY_NAME") ?: "")
  }
  var endpoint by remember {
    val saved = dataStoreRepository.readSecret("CLOUD_API_ENDPOINT")
    mutableStateOf(if (saved.isNullOrEmpty()) "https://hyper.charm.land/v1/" else saved)
  }
  var apiKey by remember {
    mutableStateOf(dataStoreRepository.readSecret("CLOUD_API_KEY") ?: "")
  }
  var modelId by remember {
    val saved = dataStoreRepository.readSecret("CLOUD_MODEL_ID")
    mutableStateOf(if (saved.isNullOrEmpty()) "gemma-4-26b-a4b-it" else saved)
  }
  var isApiKeyVisible by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
    ) {
      Column(
        modifier = Modifier
          .padding(24.dp)
          .verticalScroll(rememberScrollState())
          .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Icon and Title
        Icon(
          imageVector = Icons.Rounded.Cloud,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(40.dp)
        )
        Text(
          text = "Configure Cloud Provider",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        // Privacy Warning Info Box
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "Security & Privacy Notice",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Your API Key and Endpoint are stored securely in local Encrypted Shared Preferences. Credentials are never sent to external servers other than the configured endpoint itself.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Model Name (Display Name)
        OutlinedTextField(
          value = customDisplayName,
          onValueChange = { customDisplayName = it },
          label = { Text("Model Name (Display Name)") },
          placeholder = { Text("Cloud Model") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        // Endpoint URL
        OutlinedTextField(
          value = endpoint,
          onValueChange = { endpoint = it },
          label = { Text("API Endpoint URL") },
          placeholder = { Text("https://hyper.charm.land/v1/") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next)
        )

        // API Key
        OutlinedTextField(
          value = apiKey,
          onValueChange = { apiKey = it },
          label = { Text("API Key") },
          placeholder = { Text("sk-hyper-...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
              Icon(
                imageVector = if (isApiKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = if (isApiKeyVisible) "Hide password" else "Show password"
              )
            }
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
        )

        // Model ID
        OutlinedTextField(
          value = modelId,
          onValueChange = { modelId = it },
          label = { Text("Model ID") },
          placeholder = { Text("gemma-4-26b-a4b-it") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          keyboardActions = KeyboardActions(
            onDone = {
              dataStoreRepository.saveSecret("CLOUD_DISPLAY_NAME", customDisplayName.trim())
              dataStoreRepository.saveSecret("CLOUD_API_ENDPOINT", endpoint.trim())
              dataStoreRepository.saveSecret("CLOUD_API_KEY", apiKey.trim())
              dataStoreRepository.saveSecret("CLOUD_MODEL_ID", modelId.trim())
              onSettingsSaved()
              onDismiss()
            }
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              dataStoreRepository.saveSecret("CLOUD_DISPLAY_NAME", customDisplayName.trim())
              dataStoreRepository.saveSecret("CLOUD_API_ENDPOINT", endpoint.trim())
              dataStoreRepository.saveSecret("CLOUD_API_KEY", apiKey.trim())
              dataStoreRepository.saveSecret("CLOUD_MODEL_ID", modelId.trim())
              onSettingsSaved()
              onDismiss()
            }
          ) {
            Text("Save Settings")
          }
        }
      }
    }
  }
}
