import { useState } from 'react'

export function useAnalyze() {
    const [loading, setLoading] = useState(false)
    const [result, setResult] = useState<string | null>(null)

    async function analyze(code: string) {
        setLoading(true)
        setResult(null)
        try {
            const res = await fetch('/api/analyze', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ code }),
            })

            const data = await res.json()
            setResult(JSON.stringify(data, null, 2))
        } catch (error) {
            setResult('❌ Ошибка при анализе. Проверьте соединение с сервером.')
        } finally {
            setLoading(false)
        }
    }

    return { result, loading, analyze }
}
