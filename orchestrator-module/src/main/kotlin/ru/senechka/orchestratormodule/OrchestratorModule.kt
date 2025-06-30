package ru.senechka.orchestratormodule

import kotlinx.serialization.json.*
import ru.senechka.syntaxtreemodule.util.AnalyzerRunner

object OrchestratorModule {

    /**
     * Главная точка входа: принимает код и запрос пользователя (в естественном языке)
     */
    fun handleUserQuery(code: String, userPrompt: String): String {
        val systemPrompt = "Ты оркестратор архитектурного анализа. На входе — задание от пользователя. " +
                "Верни, какие волкеры нужно запустить, через запятую, без поясненийю Только список. Вот возможные волкеры, выбирай из них singleton, interfacesegregation, encapsulation, factory, composition, codemetrics, loops, solid, inheritance, polymorphism, template, dependency"
        val fullPrompt = "$systemPrompt\n\nЗадание: $userPrompt"
        val llamaResponse = LlamaClient.sendPrompt(fullPrompt)

        val workers = parseWalkerList(llamaResponse)

        val jsonAnalysis = AnalyzerRunner.analyze(code, workers)

        val validationPrompt = """
            Пользователь поставил задачу: "$userPrompt"
            Вот результат анализа (в формате JSON):
            $jsonAnalysis
        
            Проанализируй результат.
        
            Классифицируй его как один из следующих вариантов:
            
            ✅ Да — паттерн или правило найдено и реализовано корректно  
            ⚠ Частично — паттерн найден, есть хотя бы одно нарушение или проблема   
            ❌ Нет — паттерн отсутствует или результат не соответствует задаче
        
            Ты обязан выбрать только один вариант и указать его в начале ответа. Не используй ✅ Да, если есть ошибки.
        """.trimIndent()

        val finalValidation = LlamaClient.sendPrompt(validationPrompt)

        return """
            🔹 Запрос: $userPrompt
            🔧 Воркеры: ${workers.joinToString(", ")}
            📊 Анализ: $jsonAnalysis
            🧠 Ответ LLAMA3: $finalValidation
        """.trimIndent()
    }

    /**
     * Упрощённый парсинг ответа LLAMA в список воркеров
     */
    private fun parseWalkerList(llamaResponse: String): List<String> {
        return llamaResponse
            .lowercase()
            .replace(Regex("[^a-z0-9_,\\s]"), "")
            .split(",", " ", "\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
}
