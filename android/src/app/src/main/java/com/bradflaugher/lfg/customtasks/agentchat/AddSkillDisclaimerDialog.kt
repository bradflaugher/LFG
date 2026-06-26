package com.bradflaugher.lfg.customtasks.agentchat

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bradflaugher.lfg.R

@Composable
fun AddSkillDisclaimerDialog(
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.disclaimer_dialog_title)) },
    text = { Text(stringResource(R.string.disclaimer_dialog_content)) },
    confirmButton = {
      Button(onClick = onConfirm) { Text(stringResource(R.string.disclaimer_dialog_agree)) }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
    },
  )
}
