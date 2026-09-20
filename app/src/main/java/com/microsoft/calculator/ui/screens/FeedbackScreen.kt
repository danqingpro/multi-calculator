package com.microsoft.calculator.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.microsoft.calculator.engine.QqMailSender
import com.microsoft.calculator.ui.i18n.LocalStrings
import com.microsoft.calculator.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 反馈接收邮箱 */
private const val FEEDBACK_EMAIL = "2290943281@qq.com"

/** QQ 邮箱单封邮件附件上限:50 MB */
private const val MAX_ATTACHMENT_BYTES = 50L * 1024 * 1024

private data class Attachment(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long
)

/** 发送方式 */
private enum class SendMode { SYSTEM_EMAIL, QQ_DIRECT }

@Composable
fun FeedbackScreen() {
    val s = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var contact by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var submitting by remember { mutableStateOf(false) }

    var sendMode by remember { mutableStateOf(SendMode.SYSTEM_EMAIL) }
    var qqEmail by remember { mutableStateOf("") }
    var qqAuthCode by remember { mutableStateOf("") }

    val contentResolver = context.contentResolver

    // 取图片
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            addAttachment(uri, contentResolver) { attachments = attachments + it }
                ?: Toast.makeText(context, s.feedbackFileTooLarge, Toast.LENGTH_SHORT).show()
        }
    }

    // 取任意文件
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            addAttachment(uri, contentResolver) { attachments = attachments + it }
                ?: Toast.makeText(context, s.feedbackFileTooLarge, Toast.LENGTH_SHORT).show()
        }
    }

    fun totalSize() = attachments.sumOf { it.sizeBytes }

    fun submit() {
        if (message.isBlank()) {
            Toast.makeText(context, s.feedbackEmpty, Toast.LENGTH_SHORT).show()
            return
        }
        if (totalSize() > MAX_ATTACHMENT_BYTES) {
            Toast.makeText(context, s.feedbackTotalTooLarge, Toast.LENGTH_SHORT).show()
            return
        }
        submitting = true

        if (sendMode == SendMode.SYSTEM_EMAIL) {
            // 通过系统邮件应用发送
            val body = buildString {
                appendLine(message)
                appendLine()
                if (contact.isNotBlank()) appendLine("${s.feedbackContact}: $contact")
            }
            val intent = if (attachments.isNotEmpty()) {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "message/rfc822"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
                    putExtra(Intent.EXTRA_SUBJECT, "[${s.appName}] ${s.feedback}")
                    putExtra(Intent.EXTRA_TEXT, body)
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(attachments.map { it.uri }))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "message/rfc822"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
                    putExtra(Intent.EXTRA_SUBJECT, "[${s.appName}] ${s.feedback}")
                    putExtra(Intent.EXTRA_TEXT, body)
                }
            }
            try {
                context.startActivity(Intent.createChooser(intent, s.feedbackSubmit))
            } catch (e: Exception) {
                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
            }
            submitting = false
        } else {
            // 直接通过 QQ 邮箱 SMTP 发送
            if (qqEmail.isBlank() || qqAuthCode.isBlank()) {
                Toast.makeText(context, s.feedbackQqAuthCodeHint, Toast.LENGTH_LONG).show()
                submitting = false
                return
            }
            scope.launch {
                val attList = attachments.map { Triple(it.name, it.uri, contentResolver) }
                val result = withContext(Dispatchers.IO) {
                    QqMailSender.sendFeedback(
                        qqEmail = qqEmail,
                        authCode = qqAuthCode,
                        contact = contact.ifBlank { qqEmail },
                        message = message,
                        subject = "[${s.appName}] ${s.feedback}",
                        attachments = attList
                    )
                }
                submitting = false
                if (result == null) {
                    Toast.makeText(context, s.feedbackSendSuccess, Toast.LENGTH_LONG).show()
                    message = ""
                    attachments = emptyList()
                } else {
                    Toast.makeText(context, "${s.feedbackSendFailed}$result", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(s.feedbackSentHint, color = TextSecondary, fontSize = 12.sp)

        // 联系方式
        Column {
            Text(s.feedbackContact, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(s.feedbackContactHint, color = TextMuted) },
                singleLine = true,
                colors = outlinedFieldColors()
            )
        }

        // 反馈内容
        Column {
            Text(s.feedbackMessage, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                placeholder = { Text(s.feedbackMessageHint, color = TextMuted) },
                colors = outlinedFieldColors()
            )
        }

        // 附件按钮
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AttachButton(
                icon = Icons.Default.AddPhotoAlternate,
                label = s.feedbackAttachImage,
                onClick = { imagePicker.launch("image/*") }
            )
            AttachButton(
                icon = Icons.Default.AttachFile,
                label = s.feedbackAttachFile,
                onClick = { filePicker.launch("*/*") }
            )
        }

        // 附件列表
        if (attachments.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val totalMb = totalSize() / (1024.0 * 1024.0)
                Text(
                    "${s.feedbackAttachments} (${attachments.size}) · ${"%.1f".format(totalMb)} / 50 MB",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                attachments.forEachIndexed { idx, att ->
                    AttachmentItem(att) { attachments = attachments.filterIndexed { i, _ -> i != idx } }
                }
            }
        }

        HorizontalDivider(color = ButtonBgLight, modifier = Modifier.padding(vertical = 4.dp))

        // 发送方式选择
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(s.feedbackSubmit, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)

            // 方式 1:系统邮件应用
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(if (sendMode == SendMode.SYSTEM_EMAIL) AccentBlue.copy(alpha = 0.15f) else ButtonBg)
                    .clickable { sendMode = SendMode.SYSTEM_EMAIL }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = sendMode == SendMode.SYSTEM_EMAIL,
                    onClick = { sendMode = SendMode.SYSTEM_EMAIL },
                    colors = RadioButtonDefaults.colors(selectedColor = AccentBlue)
                )
                Spacer(Modifier.width(8.dp))
                Text(s.feedbackSendViaSystem, color = TextPrimary, fontSize = 14.sp)
            }

            // 方式 2:QQ 邮箱直接发送
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(if (sendMode == SendMode.QQ_DIRECT) AccentBlue.copy(alpha = 0.15f) else ButtonBg)
                    .clickable { sendMode = SendMode.QQ_DIRECT }
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = sendMode == SendMode.QQ_DIRECT,
                        onClick = { sendMode = SendMode.QQ_DIRECT },
                        colors = RadioButtonDefaults.colors(selectedColor = AccentBlue)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(s.feedbackSendViaQq, color = TextPrimary, fontSize = 14.sp)
                }

                if (sendMode == SendMode.QQ_DIRECT) {
                    Spacer(Modifier.height(10.dp))

                    // QQ 邮箱
                    OutlinedTextField(
                        value = qqEmail,
                        onValueChange = { qqEmail = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(s.feedbackQqEmailHint, color = TextMuted) },
                        singleLine = true,
                        label = { Text(s.feedbackQqEmail, color = TextSecondary, fontSize = 12.sp) },
                        colors = outlinedFieldColors()
                    )
                    Spacer(Modifier.height(8.dp))

                    // 授权码
                    OutlinedTextField(
                        value = qqAuthCode,
                        onValueChange = { qqAuthCode = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(s.feedbackQqAuthCodeHint, color = TextMuted) },
                        singleLine = true,
                        label = { Text(s.feedbackQqAuthCode, color = TextSecondary, fontSize = 12.sp) },
                        colors = outlinedFieldColors()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(s.feedbackQqAuthCodeHelp, color = TextMuted, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 提交按钮
        Button(
            onClick = { submit() },
            enabled = !submitting,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (submitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(s.feedbackSending, color = Color.White, fontSize = 15.sp)
            } else {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(s.feedbackSubmit, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

/** 从 Uri 读取文件名与大小,并做单文件 50MB 限制校验。返回 null 表示文件过大 */
private fun addAttachment(
    uri: Uri,
    contentResolver: android.content.ContentResolver,
    onAdd: (Attachment) -> Unit
): Unit? {
    var name = uri.lastPathSegment ?: "file"
    var size = 0L
    try {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
    } catch (_: Exception) {
    }
    if (size > MAX_ATTACHMENT_BYTES) {
        return null
    }
    onAdd(Attachment(uri, name, size))
    return Unit
}

@Composable
private fun AttachButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        color = ButtonBg,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = TextPrimary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun AttachmentItem(att: Attachment, onRemove: () -> Unit) {
    val mb = att.sizeBytes / (1024.0 * 1024.0)
    Surface(
        color = ButtonBg,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AttachFile, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(att.name, color = TextPrimary, fontSize = 13.sp, maxLines = 1)
                Text("${"%.1f".format(mb)} MB", color = TextMuted, fontSize = 11.sp)
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = PanelBg,
    unfocusedContainerColor = PanelBg,
    focusedBorderColor = AccentBlue,
    unfocusedBorderColor = ButtonBgLight,
    cursorColor = AccentBlue,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
