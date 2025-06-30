import React, { useEffect, useState } from 'react';

interface Submission {
    student: string;
    code: string;
    result: string;
}

export default function TeacherPage() {
    const [submissions, setSubmissions] = useState<Submission[]>([]);

    useEffect(() => {
        fetch('/api/submissions')
            .then(res => res.json())
            .then(data => setSubmissions(data));
    }, []);

    return (
        <div className="p-6">
            <h1 className="text-2xl font-bold mb-4">Страница преподавателя</h1>
            {submissions.length === 0 && <p>Нет отправленных заданий</p>}
            <ul className="space-y-4">
                {submissions.map((s, index) => (
                    <li key={index} className="bg-white p-4 shadow rounded">
                        <p><strong>Студент:</strong> {s.student}</p>
                        <pre className="bg-gray-100 p-2 mt-2">{s.code}</pre>
                        <p className="mt-2"><strong>Результат:</strong> {s.result}</p>
                    </li>
                ))}
            </ul>
        </div>
    );
}