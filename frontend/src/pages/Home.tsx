import { Link } from 'react-router-dom'

export default function Home() {
    return (
        <div className="p-6 text-center">
            <h1 className="text-3xl font-bold mb-6">🎓 Code Grader</h1>
            <div className="space-x-4">
                <Link to="/student">
                    <button className="px-6 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
                        Войти как студент
                    </button>
                </Link>
                <Link to="/teacher">
                    <button className="px-6 py-2 bg-green-600 text-white rounded hover:bg-green-700">
                        Войти как преподаватель
                    </button>
                </Link>
            </div>
        </div>
    )
}