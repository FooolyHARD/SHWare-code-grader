interface Props {
    result: string | null
    loading: boolean
}

export default function ResultPanel({ result, loading }: Props) {
    if (loading) return <p className="mt-4 text-gray-500">Анализируем...</p>
    if (!result) return null

    return (
        <div className="mt-4 bg-white border rounded p-4 shadow">
            <h3 className="font-semibold mb-2">📊 Результаты анализа:</h3>
            <pre className="text-sm whitespace-pre-wrap">{result}</pre>
        </div>
    )
}
