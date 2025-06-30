package ru.senechka.orchestratormodule

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.*
import kotlinx.serialization.json.*

object LlamaClient {

    @Serializable
    data class LlamaRequest(
        val model: String = "llama3:8b", // 💥 Убедись, что модель по умолчанию задана
        val prompt: String,
        val stream: Boolean = false
    )

    fun sendPrompt(prompt: String): String {
        val apiUrl = "http://localhost:11434/api/generate"

        val request = LlamaRequest(prompt = prompt)
        val json = Json { encodeDefaults = true }  // гарантируем сериализацию всех полей
        val requestJson = json.encodeToString(request)

        println("📤 Отправляем JSON: $requestJson")

        val connection = URL(apiUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 5000
        connection.readTimeout = 50000

        connection.outputStream.use { it.write(requestJson.toByteArray()) }

        val responseText = try {
            connection.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            connection.errorStream?.bufferedReader()?.readText()
                ?: throw RuntimeException("Ошибка запроса к LLAMA3: ${e.message}")
        } finally {
            connection.disconnect()
        }

        println("🟡 DEBUG: raw responseText = $responseText")

        return try {
            val jsonElement = Json.parseToJsonElement(responseText).jsonObject
            jsonElement["response"]?.jsonPrimitive?.content ?: "[LLAMA3] Нет поля 'response'"
        } catch (e: Exception) {
            "[LLAMA3] Ошибка разбора ответа: $responseText"
        }
    }
}
