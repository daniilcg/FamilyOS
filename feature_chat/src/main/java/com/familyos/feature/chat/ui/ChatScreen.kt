package com.familyos.feature.chat.ui

import android.Manifest
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.familyos.core.domain.model.ChatMessage
import com.familyos.core.domain.model.MemberPresence
import com.familyos.core.domain.model.MessageType
import com.familyos.core.ui.components.FamilyLoading
import com.familyos.feature.chat.viewmodel.ChatUiState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import java.io.File

private val EMOJIS = listOf("😀", "😂", "❤️", "👍", "🎉", "🙏", "🔥", "🏠", "🛒", "✅")

/**
 * Family chat screen with text, photo, voice, emoji, read receipts, and online status.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onDraftChange: (String) -> Unit,
    onSendText: () -> Unit,
    onSendEmoji: (String) -> Unit,
    onSendPhoto: (String) -> Unit,
    onSendVoice: (String, Long) -> Unit,
    onRecordingChanged: (Boolean, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.thread?.title ?: "Family Chat")
                        OnlineStatusRow(state.presence)
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            FamilyLoading()
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isMine = message.senderId == state.userId,
                        readCount = message.readBy.size,
                    )
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(EMOJIS) { emoji ->
                    Text(
                        text = emoji,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSendEmoji(emoji) }
                            .padding(8.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }

            ComposerBar(
                draft = state.draft,
                isRecording = state.isRecording,
                recordingMs = state.recordingMs,
                onDraftChange = onDraftChange,
                onSendText = onSendText,
                onSendPhoto = onSendPhoto,
                onSendVoice = onSendVoice,
                onRecordingChanged = onRecordingChanged,
            )

            state.errorMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun OnlineStatusRow(presence: List<MemberPresence>) {
    val online = presence.filter { it.isOnline }
    Text(
        text = when {
            online.isEmpty() -> "No one online"
            online.size == 1 -> "${online.first().displayName} online"
            else -> "${online.size} online"
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    )
}

@Composable
private fun MessageBubble(message: ChatMessage, isMine: Boolean, readCount: Int) {
    val bg = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .padding(12.dp),
        ) {
            when (message.type) {
                MessageType.IMAGE -> {
                    val imageSource = resolveImageSource(message)
                    if (imageSource != null) {
                        AsyncImage(
                            model = imageSource,
                            contentDescription = "Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text("📷 ${message.body}", color = fg)
                    }
                }
                MessageType.VOICE -> Text(
                    "🎤 Voice (${(message.durationMs ?: 0) / 1000}s)",
                    color = fg,
                )
                else -> Text(message.body, color = fg)
            }
        }
        if (isMine) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DoneAll,
                    contentDescription = "Read receipts",
                    modifier = Modifier.size(14.dp),
                    tint = if (readCount > 1) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
                Text(
                    " $readCount",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

/** Prefer attachmentUrl; fall back to body when it looks like a path/URI. */
private fun resolveImageSource(message: ChatMessage): Any? {
    val candidates = listOfNotNull(message.attachmentUrl, message.body)
    for (raw in candidates) {
        val value = raw.trim()
        if (value.isEmpty() || value.equals("Photo", ignoreCase = true)) continue
        when {
            value.startsWith("content://") ||
                value.startsWith("file://") ||
                value.startsWith("http://") ||
                value.startsWith("https://") -> return value
            value.startsWith("/") || value.contains(File.separatorChar) -> {
                val file = File(value)
                if (file.exists()) return file
                return value
            }
        }
    }
    return null
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ComposerBar(
    draft: String,
    isRecording: Boolean,
    recordingMs: Long,
    onDraftChange: (String) -> Unit,
    onSendText: () -> Unit,
    onSendPhoto: (String) -> Unit,
    onSendVoice: (String, Long) -> Unit,
    onRecordingChanged: (Boolean, Long) -> Unit,
) {
    val context = LocalContext.current
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val imagePermission = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        },
    )
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    var elapsed by remember { mutableLongStateOf(0L) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val dest = File(context.cacheDir, "chat_photo_${System.currentTimeMillis()}.jpg")
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return@rememberLauncherForActivityResult
            onSendPhoto(dest.absolutePath)
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsed = 0L
            while (true) {
                delay(200)
                elapsed += 200
                onRecordingChanged(true, elapsed)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { recorder?.release() }
            recorder = null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {
                if (!imagePermission.status.isGranted) {
                    imagePermission.launchPermissionRequest()
                    return@IconButton
                }
                photoPicker.launch("image/*")
            },
        ) {
            Icon(Icons.Default.Image, contentDescription = "Send photo")
        }

        if (isRecording) {
            Text(
                "Recording ${(recordingMs / 1000)}s",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.error,
            )
            IconButton(
                onClick = {
                    runCatching {
                        recorder?.stop()
                        recorder?.release()
                    }
                    recorder = null
                    val file = outputFile
                    onRecordingChanged(false, elapsed)
                    if (file != null && file.exists()) {
                        onSendVoice(file.absolutePath, elapsed)
                    }
                },
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop recording")
            }
        } else {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message") },
                maxLines = 4,
            )
            IconButton(
                onClick = {
                    if (!micPermission.status.isGranted) {
                        micPermission.launchPermissionRequest()
                        return@IconButton
                    }
                    val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
                    outputFile = file
                    val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        MediaRecorder(context)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaRecorder()
                    }
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    mediaRecorder.setOutputFile(file.absolutePath)
                    mediaRecorder.prepare()
                    mediaRecorder.start()
                    recorder = mediaRecorder
                    onRecordingChanged(true, 0L)
                },
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Record voice")
            }
            IconButton(onClick = onSendText, enabled = draft.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}
