package br.com.whatsdireto.domain

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

object WhatsAppLauncher {

    fun open(context: Context, normalizedPhone: String, message: String = "") {
        try {
            // Remove qualquer caractere não numérico do telefone
            val cleanPhone = normalizedPhone.replace("[^0-9]".toRegex(), "")

            // Formato correto para o WhatsApp com mensagem
            val intent = if (message.isNotBlank()) {
                // Para mensagem, usa o formato api.whatsapp.com/send
                val encodedMessage = URLEncoder.encode(message, "UTF-8")
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage")
                Intent(Intent.ACTION_VIEW, uri)
            } else {
                // Sem mensagem, pode usar wa.me
                val uri = Uri.parse("https://wa.me/$cleanPhone")
                Intent(Intent.ACTION_VIEW, uri)
            }

            intent.apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                // Tenta com WhatsApp Business
                val cleanPhone = normalizedPhone.replace("[^0-9]".toRegex(), "")
                val businessIntent = if (message.isNotBlank()) {
                    val encodedMessage = URLEncoder.encode(message, "UTF-8")
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"))
                } else {
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanPhone"))
                }.apply {
                    setPackage("com.whatsapp.w4b")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(businessIntent)
            } catch (e2: ActivityNotFoundException) {
                // Fallback para navegador
                val cleanPhone = normalizedPhone.replace("[^0-9]".toRegex(), "")
                val browserIntent = if (message.isNotBlank()) {
                    val encodedMessage = URLEncoder.encode(message, "UTF-8")
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"))
                } else {
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanPhone"))
                }.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            }
        }
    }
}