package com.microsoft.calculator.engine

import android.net.Uri
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.internet.MimeUtility

/**
 * QQ 邮箱 SMTP 直接发送器。
 *
 * 使用 QQ 邮箱的 SMTP 服务器(smtp.qq.com:465 SSL),
 * 通过用户的 QQ 邮箱地址 + 邮箱授权码(非 QQ 密码)直接发送邮件。
 * 这样即使手机没有安装 QQ 邮箱 App,也能把反馈直接发送到开发者邮箱。
 *
 * 授权码获取方式:
 *   QQ 邮箱 → 设置 → 账户 → POP3/SMTP/IMAP/Exchange/CardDAV/CalDAV 服务 →
 *   开启 SMTP 服务 → 生成授权码(16 位)。
 */
object QqMailSender {

    private const val SMTP_HOST = "smtp.qq.com"
    private const val SMTP_PORT = "465"
    private const val FEEDBACK_RECIPIENT = "2290943281@qq.com"

    /**
     * 发送反馈邮件。
     *
     * @param qqEmail   发送者 QQ 邮箱(如 123456@qq.com)
     * @param authCode  QQ 邮箱授权码(16 位)
     * @param contact   发送者联系方式(电话或邮箱)
     * @param message   反馈正文
     * @param subject   邮件主题
     * @param attachments 附件列表(文件名 -> content Uri)
     * @param contentResolver 用于读取附件内容
     * @return 成功返回 null,失败返回错误信息
     */
    fun sendFeedback(
        qqEmail: String,
        authCode: String,
        contact: String,
        message: String,
        subject: String,
        attachments: List<Triple<String, Uri, android.content.ContentResolver>>
    ): String? {
        return try {
            val props = Properties().apply {
                put("mail.smtp.host", SMTP_HOST)
                put("mail.smtp.port", SMTP_PORT)
                put("mail.smtp.auth", "true")
                put("mail.smtp.ssl.enable", "true")
                put("mail.smtp.ssl.trust", SMTP_HOST)
                put("mail.smtp.connectiontimeout", "15000")
                put("mail.smtp.timeout", "30000")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(qqEmail, authCode)
                }
            })

            val mimeMsg = MimeMessage(session)
            mimeMsg.setFrom(InternetAddress(qqEmail))
            mimeMsg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(FEEDBACK_RECIPIENT))
            mimeMsg.subject = MimeUtility.encodeText(subject, "UTF-8", "B")

            val multipart = MimeMultipart()

            // 正文
            val textPart = MimeBodyPart()
            val body = buildString {
                appendLine(message)
                appendLine()
                if (contact.isNotBlank()) {
                    appendLine("联系方式: $contact")
                }
                appendLine()
                appendLine("—— 通过计算器 App 反馈")
            }
            textPart.setText(body, "UTF-8")
            multipart.addBodyPart(textPart)

            // 附件
            for ((name, uri, resolver) in attachments) {
                val part = MimeBodyPart()
                // 把 content Uri 复制到临时文件,再作为附件
                val tempFile = java.io.File.createTempFile("attach_", "_" + name.takeLast(40))
                resolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                val source = FileDataSource(tempFile)
                part.dataHandler = DataHandler(source)
                part.fileName = MimeUtility.encodeText(name, "UTF-8", "B")
                multipart.addBodyPart(part)
            }

            mimeMsg.setContent(multipart)
            Transport.send(mimeMsg)
            null
        } catch (e: Exception) {
            e.message ?: e.javaClass.simpleName
        }
    }
}
