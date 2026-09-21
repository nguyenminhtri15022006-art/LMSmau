import React, {useCallback, useEffect, useRef, useState} from 'react';
import {Link, Navigate, useLocation, useNavigate, useParams} from 'react-router-dom';
import EditCourseSidebar from '../../components/teacher/EditCourse/EditCourseSidebar';
import EditCourseInfo from '../../components/teacher/EditCourse/EditCourseInfo';
import Step2Curriculum from '../../components/teacher/CreateCourse/Step2Curriculum';
import { buildAppErrorState, getAppErrorRoute } from '../../../../utils/appError';
import {courseApi} from '../../../../services/course.api';
import {useToast} from '../../../../components/ui/Toast';
import '../../styles/teacher/EditCourse/editCourse.css';

const flatToTree = (nodes) => {
    if (!nodes?.length) return [];
    const sorted = [...nodes].sort((a, b) => (a.order ?? 0) - (b.order ?? 0));
    const map = {};
    sorted.forEach((node) => {
        map[node.id] = {
            id: node.id,
            type: node.type === 'lesson' ? 'lesson' : (node.parentId ? 'section' : 'chapter'),
            title: node.title,
            lessonId: node.lessonId,
            contentType: node.lessonType ? node.lessonType.toLowerCase() : 'video',
            items: [],
        };
    });
    const roots = [];
    sorted.forEach((node) => {
        const current = map[node.id];
        if (node.parentId && map[node.parentId]) {
            map[node.parentId].items.push(current);
        } else if (!node.parentId) {
            roots.push(current);
        }
    });
    return roots;
};

const EMPTY_COURSE = {
    title: '',
    level: '',
    categoryIds: [],
    duration: '',
    description: '',
    thumbnailUrl: '',
    thumbnailFile: null,
    learningPoints: [],
    requirements: [],
    status: 'PRIVATE',
    chapters: [],
    transferEmail: '',
};

const EditCoursePage = () => {
    const {courseId} = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const toast = useToast();
    const originalData = useRef(null);
    const newlyUploadedUrls = useRef(new Set());
    const pendingDeleteUrls = useRef(new Set());
    const newlyAddedNodeIds = useRef(new Set());
    const [activeTab, setActiveTab] = useState('info');
    const [courseData, setCourseData] = useState(EMPTY_COURSE);
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchData = async () => {
            try {
                setLoading(true);
                const [courseRes, categoriesData, structureData] = await Promise.all([
                    courseApi.getById(courseId),
                    courseApi.getCategories(),
                    courseApi.getStructure(courseId).catch(() => null),
                ]);

                const course = courseRes?.data?.data ?? courseRes?.data;
                const fetched = {
                    title: course.title || '',
                    level: course.level || '',
                    categoryIds: (course.categories || []).map((category) => category.id),
                    duration: course.duration || '',
                    description: course.description || '',
                    thumbnailUrl: course.thumbnailUrl || '',
                    thumbnailFile: null,
                    learningPoints: course.learningPoints || [],
                    requirements: course.requirements || [],
                    status: course.status || 'PRIVATE',
                    chapters: flatToTree(structureData?.nodes),
                    transferEmail: '',
                };

                originalData.current = fetched;
                setCourseData(fetched);
                setCategories(categoriesData);
                setError(null);
            } catch (err) {
                setError(buildAppErrorState(err, {
                    title: 'Không thể tải thông tin khóa học',
                    fallbackPath: '/manage/courses',
                }));
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, [courseId]);

    const updateCourseData = useCallback((newData) => {
        setCourseData((prev) => ({...prev, ...newData}));
    }, []);

    const handleNodeAdded = useCallback((nodeId) => {
        newlyAddedNodeIds.current.add(nodeId);
    }, []);

    const handleNodeDeleted = useCallback((nodeIds) => {
        nodeIds.forEach((id) => newlyAddedNodeIds.current.delete(id));
    }, []);

    const handleFileUploaded = useCallback((url) => {
        newlyUploadedUrls.current.add(url);
    }, []);

    const handleFileRemoved = useCallback((url) => {
        if (!url) return;
        if (newlyUploadedUrls.current.has(url)) {
            newlyUploadedUrls.current.delete(url);
            courseApi.deleteStorageFile(url).catch(console.error);
        } else {
            pendingDeleteUrls.current.add(url);
        }
    }, []);

    const handleDiscard = () => {
        toast.confirm(
            'Tất cả thay đổi chưa lưu sẽ bị hoàn tác về giá trị ban đầu.',
            () => {
                newlyUploadedUrls.current.forEach((url) => {
                    courseApi.deleteStorageFile(url).catch(console.error);
                });
                newlyUploadedUrls.current.clear();
                pendingDeleteUrls.current.clear();

                const nodeIds = [...newlyAddedNodeIds.current];
                newlyAddedNodeIds.current.clear();
                nodeIds.forEach((nodeId) => {
                    courseApi.deleteStructureNode(courseId, nodeId).catch(console.error);
                });

                setCourseData({...originalData.current});
                toast.info('Đã hoàn tác về dữ liệu ban đầu.');
            },
            {title: 'Loại bỏ thay đổi?', confirmLabel: 'Loại bỏ', cancelLabel: 'Tiếp tục chỉnh sửa'}
        );
    };

    const handleSave = async () => {
        if (!courseData.title.trim()) {
            toast.error('Tiêu đề khóa học không được để trống.');
            return;
        }
        try {
            setSaving(true);
            const formData = new FormData();
            formData.append('title', courseData.title);
            formData.append('description', courseData.description || '');
            formData.append('status', courseData.status);
            if (courseData.duration) formData.append('duration', String(courseData.duration));
            if (courseData.level) formData.append('level', courseData.level);
            (courseData.categoryIds || []).forEach((id) => formData.append('categoryIds', id));
            (courseData.learningPoints || []).filter(Boolean).forEach((point) => formData.append('learningPoints', point));
            (courseData.requirements || []).filter(Boolean).forEach((requirement) => formData.append('requirements', requirement));
            if (courseData.thumbnailFile) formData.append('thumbnail', courseData.thumbnailFile);

            await courseApi.update(courseId, formData);
            pendingDeleteUrls.current.forEach((url) => {
                courseApi.deleteStorageFile(url).catch(console.error);
            });
            pendingDeleteUrls.current.clear();
            newlyUploadedUrls.current.clear();
            newlyAddedNodeIds.current.clear();
            originalData.current = {...courseData, thumbnailFile: null};
            toast.success('Khóa học đã được cập nhật thành công.');
        } catch (err) {
            const msg = err?.response?.data?.message || err.message;
            toast.error(msg, 'Lưu thất bại');
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async () => {
        if (!window.confirm('Bạn có chắc chắn muốn xóa khóa học này? Hành động này không thể hoàn tác.')) return;
        try {
            await courseApi.delete(courseId);
            navigate('/manage/courses');
        } catch (err) {
            const msg = err?.response?.data?.message || err.message;
            toast.error(msg, 'Xóa thất bại');
        }
    };

    if (error) {
        return <Navigate to={getAppErrorRoute(location.pathname)} replace state={error} />;
    }

    if (loading) return <div className="edit-course-loading">Đang tải...</div>;

    return (
        <div className="edit-course-layout">
            <div className="sidebar-col">
                <EditCourseSidebar activeTab={activeTab} setActiveTab={setActiveTab}/>
            </div>

            <div className="content-col">
                <div className="breadcrumbs">
                    <Link to="/manage/courses">Khóa học</Link>
                    <span className="material-symbols-outlined" style={{ fontSize: '14px', color: '#cbd5e1' }}>chevron_right</span>
                    <Link to={`/manage/courses/center/${courseId}`} className="breadcrumb-current">{courseData.title || 'Quản lý khóa học'}</Link>
                    <span className="material-symbols-outlined" style={{ fontSize: '14px', color: '#cbd5e1' }}>chevron_right</span>
                    <span>Chỉnh sửa</span>
                </div>
                <div className="edit-card">
                    <div className="edit-card-header">
                        <div className="header-text">
                            {activeTab === 'curriculum' ? (
                                <>
                                    <h2>Nội dung khóa học</h2>
                                    <p>Thay đổi được lưu tự động ngay khi thực hiện.</p>
                                </>
                            ) : (
                                <>
                                    <h2>Chỉnh sửa thông tin khóa học</h2>
                                    <p>Cập nhật sẽ được hiển thị ngay sau khi lưu.</p>
                                </>
                            )}
                        </div>
                        {activeTab !== 'curriculum' && (
                            <div className="edit-actions">
                                <button className="btn-discard" onClick={handleDiscard}>Loại bỏ thay đổi</button>
                                <button className="btn-save" onClick={handleSave} disabled={saving}>
                                    {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
                                </button>
                            </div>
                        )}
                    </div>

                    <div className="edit-card-body">
                        {activeTab === 'info' && (
                            <EditCourseInfo
                                data={courseData}
                                updateData={updateCourseData}
                                categories={categories}
                            />
                        )}
                        {activeTab === 'curriculum' && (
                            <div className="curriculum-wrapper-edit">
                                <Step2Curriculum
                                    courseId={courseId}
                                    data={courseData}
                                    updateData={updateCourseData}
                                    onFileUploaded={handleFileUploaded}
                                    onFileRemoved={handleFileRemoved}
                                    onNodeAdded={handleNodeAdded}
                                    onNodeDeleted={handleNodeDeleted}
                                />
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default EditCoursePage;
