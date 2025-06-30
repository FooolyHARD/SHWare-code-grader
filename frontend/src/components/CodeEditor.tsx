import { useState } from 'react'

interface Props {
    onSubmit: (code: string) => void
}

export default function CodeEditor({ onSubmit }: Props) {
    const [code, setCode] = useState('')

    return (
        <div className="space-y-4">
      <textarea
          value={code}
          onChange={(e) => setCode(e.target.value)}
          placeholder="Введите ваш код здесь..."
          className="w-full h-64 p-2 font-mono text-sm border rounded-md resize-none outline-none"
      />
            <button
                onClick={() => onSubmit(code)}
                className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
            >
                🔍 Анализировать
            </button>
        </div>
    )
}
