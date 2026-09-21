import React, { useState, useEffect } from 'react';
import { Navigate, useParams, useNavigate } from 'react-router-dom';
import { assignmentApi } from '../../../../../services/assignment.api';
import { courseApi } from '../../../../../services/course.api';
import { mapQuizToExerciseHubItem } from '../../../../../utils/assignmentMappers';
import { buildAppErrorState } from '../../../../../utils/appError';
import AnimatedPage from '../../../../../components/ui/AnimatedPage';
import ExerciseBreadcrumb from '../../../shared/components/ExerciseBreadcrumb';
import QuizItem from '../../../Exercise/components/student/ExerciseHub/QuizItem';

import '../../../shared/styles/ExerciseShared.css';

export default function QuizListingPage() {
  const { courseId } = useParams();
  const navigate = useNavigate();

  const [quizzes, setQuizzes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [courseTitle, setCourseTitle] = useState('');

  useEffect(() => {
    if (!courseId) return;
    let cancelled = false;
    const fetchData = async () => {
      try {
        setLoading(true);
        const courses = await courseApi.getEnrolled();
        if (cancelled) return;
        const course = (Array.isArray(courses) ? courses : []).find(c => c.id === courseId);
        if (course && !cancelled) {
          setCourseTitle(course.title || '');
        }

        const data = await assignmentApi.getQuizzesForStudent(courseId, { page: 0, size: 50 });
        if (cancelled) return;
        const content = data?.content || (Array.isArray(data) ? data : []);
        setQuizzes(content.map(mapQuizToExerciseHubItem));
      } catch (err) {
        if (!cancelled) {
          setError(buildAppErrorState(err, {
            title: 'Không thể tải danh sách quiz',
            fallbackPath: '/exercises',
          }));
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    fetchData();
    return () => { cancelled = true; };
  }, [courseId]);

  if (loading) {
    return (
      <AnimatedPage>
        <div className="exercise-page" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh' }}>
          <p style={{ color: '#64748b' }}>Đang tải danh sách quiz...</p>
        </div>
      </AnimatedPage>
    );
  }

  if (error) {
    return <Navigate to="/error" replace state={error} />;
  }

  return (
    <AnimatedPage>
      <div className="exercise-page">
        <ExerciseBreadcrumb items={[{ label: courseTitle || 'Khoá học' }]} />

        <header className="listing-header">
          <h1 className="listing-title">{courseTitle || 'Quiz Exercises'}</h1>
          <p className="listing-subtitle">Kiểm tra kiến thức với các bài quiz trắc nghiệm đa dạng</p>
        </header>

        <div className="quiz-grid">
          {quizzes.length > 0 ? (
            quizzes.map(quiz => (
              <QuizItem
                key={quiz.id}
                quiz={quiz}
                courseTitle={courseTitle}
                courseSlug={courseId}
              />
            ))
          ) : (
            <div style={{ gridColumn: '1 / -1', textAlign: 'center', padding: '64px', background: 'white', borderRadius: '16px', color: '#64748b' }}>
              <p>Chưa có bài quiz nào cho khoá học này.</p>
              <button className="start-btn" style={{ marginTop: '16px' }} onClick={() => navigate('/exercises')}>
                Quay lại
              </button>
            </div>
          )}
        </div>
      </div>
    </AnimatedPage>
  );
}
