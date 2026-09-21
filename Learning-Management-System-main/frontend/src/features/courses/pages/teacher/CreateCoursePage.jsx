import React, { useState, useEffect } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSpinner } from '@fortawesome/free-solid-svg-icons';
import { buildAppErrorState, getAppErrorRoute } from '../../../../utils/appError';
import { courseApi } from '../../../../services/course.api';
import { useToast } from '../../../../components/ui/Toast';
import EditCourseInfo from '../../components/teacher/EditCourse/EditCourseInfo';
import '../../styles/teacher/CreateCourse/createCourse.css';

const INITIAL_DATA = {
    title: '',
    level: '',
    duration: '',
    categoryIds: [],
    thumbnailFile: null,
    thumbnailUrl: null,
    description: '',
    learningPoints: [''],
    requirements: [],
    status: 'PRIVATE',
};

const CreateCoursePage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const toast = useToast();
    const [courseData, setCourseData] = useState(INITIAL_DATA);
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(false);
    const [bootstrapping, setBootstrapping] = useState(true);
    const [errors, setErrors] = useState({});
    const [error, setError] = useState(null);

    useEffect(() => {
        const loadCategories = async () => {
            try {
                const data = await courseApi.getCategories();
                setCategories(data);
                setError(null);
            } catch (err) {
                setError(buildAppErrorState(err, {
                    title: 'Không thể tải dữ liệu tạo khóa học',
                    fallbackPath: '/manage/courses',
                }));
            } finally {
                setBootstrapping(false);
            }
        };

        loadCategories();
    }, []);

    const updateCourseData = (newData) => {
        setCourseData((prev) => ({ ...prev, ...newData }));
        const keys = Object.keys(newData);
        if (keys.some((key) => errors[key])) {
            setErrors((prev) => {
                const next = { ...prev };
                keys.forEach((key) => delete next[key]);
                return next;
            });
        }
    };

    const validate = () => {
        const nextErrors = {};
        const learningPoints = (courseData.learningPoints || []).filter((point) => point.trim());
        if (!courseData.title?.trim()) nextErrors.title = 'Tiêu đề không được để trống.';
        if (!courseData.duration || Number(courseData.duration) < 1) nextErrors.duration = 'Thời hạn phải ít nhất 1 tháng.';
        if (learningPoints.length === 0) nextErrors.learningPoints = 'Vui lòng nhập ít nhất một mục tiêu học viên đạt được.';
        return nextErrors;
    };

    const handleSubmit = async () => {
        const nextErrors = validate();
        if (Object.keys(nextErrors).length > 0) {
            setErrors(nextErrors);
            return;
        }

        setLoading(true);
        try {
            const formData = new FormData();
            formData.append('title', courseData.title.trim());
            formData.append('status', courseData.status);
            if (courseData.description?.trim()) formData.append('description', courseData.description.trim());
            if (courseData.duration) formData.append('duration', courseData.duration);
            if (courseData.level) formData.append('level', courseData.level);
            if (courseData.thumbnailFile) formData.append('thumbnail', courseData.thumbnailFile);
            (courseData.categoryIds || []).forEach((id) => formData.append('categoryIds', id));
            (courseData.learningPoints || []).filter((point) => point.trim()).forEach((point) => formData.append('learningPoints', point.trim()));
            (courseData.requirements || []).filter((requirement) => requirement.trim()).forEach((requirement) => formData.append('requirements', requirement.trim()));

            const res = await courseApi.create(formData);
            const newCourse = res?.data?.data ?? res?.data;
            toast.success('Khóa học đã được tạo thành công!');
            navigate(newCourse?.id ? `/manage/courses/center/${newCourse.id}` : '/manage/courses');
        } catch (err) {
            const msg = err?.response?.data?.message;
            if (msg === 'COURSE_TITLE_BLANK') setErrors({ title: 'Tiêu đề không được để trống.' });
            else if (msg === 'COURSE_DURATION_MIN') setErrors({ duration: 'Thời hạn phải ít nhất 1 tháng.' });
            else toast.error('Tạo khóa học thất bại. Vui lòng thử lại.');
        } finally {
            setLoading(false);
        }
    };

    if (error) {
        return <Navigate to={getAppErrorRoute(location.pathname)} replace state={error} />;
    }

    if (bootstrapping) {
        return <div className="create-course-container" style={{ padding: '48px', textAlign: 'center' }}>Đang tải...</div>;
    }

    return (
        <div className="create-course-container">
            <header className="create-course-header">
                <div className="breadcrumbs">
                    <Link to="/manage/courses">Khóa học</Link>
                    {' / '}
                    <span>{courseData.title || 'Tạo khóa học mới'}</span>
                </div>
                <h1>Tạo khóa học mới</h1>
                <p>Điền đầy đủ thông tin bên dưới. Bạn có thể chỉnh sửa thêm nội dung và cấu trúc bài học sau khi tạo.</p>
            </header>

            <main className="step-content-area">
                <div className="step-content-card">
                    <EditCourseInfo
                        data={courseData}
                        updateData={updateCourseData}
                        categories={categories}
                        errors={errors}
                        requireLearningPoints
                    />
                </div>
            </main>

            <div className="creation-footer">
                <button
                    className="btn-footer-cancel"
                    onClick={() => navigate('/manage/courses')}
                    disabled={loading}
                >
                    Hủy bỏ
                </button>
                <button className="btn-finish" onClick={handleSubmit} disabled={loading}>
                    {loading
                        ? <><FontAwesomeIcon icon={faSpinner} spin style={{ marginRight: 8 }} />Đang tạo...</>
                        : 'Tạo khóa học'}
                </button>
            </div>
        </div>
    );
};

export default CreateCoursePage;
