package com.bradflaugher.lfg.ui.common.modelitem

// import androidx.compose.ui.tooling.preview.Preview
// import com.bradflaugher.lfg.ui.theme.LfgTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.bradflaugher.lfg.R
import com.bradflaugher.lfg.data.MODEL_INFO_ICON_SIZE
import com.bradflaugher.lfg.data.Model
import com.bradflaugher.lfg.data.ModelDownloadStatus
import com.bradflaugher.lfg.data.ModelDownloadStatusType
import com.bradflaugher.lfg.data.Task
import com.bradflaugher.lfg.ui.common.getTaskBgGradientColors
import com.bradflaugher.lfg.ui.theme.customColors

/** Composable function to display an icon representing the download status of a model. */
@Composable
fun StatusIcon(
  task: Task?,
  model: Model,
  downloadStatus: ModelDownloadStatus?,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
    modifier = modifier,
  ) {
    if (model.name == "Cloud-Model-OpenAI-Compatible") {
      Icon(
        Icons.Rounded.Cloud,
        tint = Color(0xFF2196F3),
        contentDescription = "Cloud Model",
        modifier = Modifier.size(MODEL_INFO_ICON_SIZE),
      )
    } else if (model.localFileRelativeDirPathOverride.isNotEmpty()) {
      Icon(
        Icons.Filled.DownloadForOffline,
        tint = Color(0xFF4CAF50),
        contentDescription = stringResource(R.string.cd_downloaded_icon),
        modifier = Modifier.size(MODEL_INFO_ICON_SIZE),
      )
    } else {
      val color =
        if (task != null) {
          getTaskBgGradientColors(task = task)[1]
        } else {
          MaterialTheme.colorScheme.primary
        }
      when (downloadStatus?.status) {
        ModelDownloadStatusType.NOT_DOWNLOADED ->
          Icon(
            Icons.AutoMirrored.Outlined.HelpOutline,
            tint = MaterialTheme.customColors.modelInfoIconColor,
            contentDescription = stringResource(R.string.cd_not_downloaded_icon),
            modifier = Modifier.size(MODEL_INFO_ICON_SIZE),
          )

        ModelDownloadStatusType.SUCCEEDED -> {
          Icon(
            Icons.Filled.DownloadForOffline,
            tint = Color(0xFF4CAF50),
            contentDescription = stringResource(R.string.cd_downloaded_icon),
            modifier = Modifier.size(MODEL_INFO_ICON_SIZE),
          )
        }

        ModelDownloadStatusType.FAILED ->
          Icon(
            Icons.Rounded.Error,
            tint = Color(0xFFAA0000),
            contentDescription = stringResource(R.string.cd_download_failed_icon),
            modifier = Modifier.size(MODEL_INFO_ICON_SIZE),
          )

        ModelDownloadStatusType.IN_PROGRESS ->
          Icon(
            Icons.Rounded.Downloading,
            contentDescription = stringResource(R.string.cd_downloading_icon),
            modifier = Modifier.size(MODEL_INFO_ICON_SIZE),
          )

        else -> {}
      }
    }
  }
}

// @Preview(showBackground = true)
// @Composable
// fun StatusIconPreview() {
//   LfgTheme {
//     Column {
//       for (downloadStatus in
//         listOf(
//           ModelDownloadStatus(status = ModelDownloadStatusType.NOT_DOWNLOADED),
//           ModelDownloadStatus(status = ModelDownloadStatusType.IN_PROGRESS),
//           ModelDownloadStatus(status = ModelDownloadStatusType.SUCCEEDED),
//           ModelDownloadStatus(status = ModelDownloadStatusType.FAILED),
//           ModelDownloadStatus(status = ModelDownloadStatusType.UNZIPPING),
//           ModelDownloadStatus(status = ModelDownloadStatusType.PARTIALLY_DOWNLOADED),
//         )) {
//         StatusIcon(downloadStatus = downloadStatus)
//       }
//     }
//   }
// }
