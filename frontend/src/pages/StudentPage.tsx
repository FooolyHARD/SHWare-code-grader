import React, { useState } from 'react';

import { Button, Textarea } from '@/components/ui/';

export default function StudentPage() {
    const [code, setCode] = useState('');
    const [result, setResult] = useState('');

    const handleSubmit = async () => {
        const response = await fetch('/api/submit', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ code }),
        });
        const data = await response.json();
        setResult(data.result);
    };

    return (
        <div className="p-6">
            <h1 className="text-2xl font-bold mb-4">Страница студента</h1>
            <Textarea value={code} onChange={e => setCode(e.target.value)} placeholder="Вставьте ваш код" />
            <Button onClick={handleSubmit} className="mt-4">Отправить на проверку</Button>
            {result && <div className="mt-4 bg-gray-100 p-4 rounded">Результат: {result}</div>}
        </div>
    );
}