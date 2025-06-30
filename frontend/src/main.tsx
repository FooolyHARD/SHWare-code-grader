import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import Home from './pages/Home'
import StudentPage from './pages/StudentPage'
import TeacherPage from './pages/TeacherPage'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <Router>
            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/student" element={<StudentPage />} />
                <Route path="/teacher" element={<TeacherPage />} />
            </Routes>
        </Router>
    </React.StrictMode>
)