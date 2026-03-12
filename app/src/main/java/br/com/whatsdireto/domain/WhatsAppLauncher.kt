package br.com.whatsdireto.domain

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

object WhatsAppLauncher {

    fun open(context: Context, normalizedPhone: String, message: String = "") {
        val encodedMessage = if (message.isNotBlank()) {
            "?text=${URLEncoder.encode(message, "UTF-8")}"
        } else {
            ""
        }

        val directUri = Uri.parse("https://wa.me/$normalizedPhone$encodedMessage")

        val whatsappIntent = Intent(Intent.ACTION_VIEW, directUri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val whatsappBusinessIntent = Intent(Intent.ACTION_VIEW, directUri).apply {
            setPackage("com.whatsapp.w4b")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val fallbackIntent = Intent(Intent.ACTION_VIEW, directUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(whatsappIntent)
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(whatsappBusinessIntent)
            } catch (_: ActivityNotFoundException) {
                context.startActivity(fallbackIntent)
            }
        }
    }
}