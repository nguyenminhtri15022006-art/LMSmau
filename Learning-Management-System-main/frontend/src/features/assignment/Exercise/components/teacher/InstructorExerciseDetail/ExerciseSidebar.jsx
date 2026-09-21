import {useNavigate, useParams} from 'react-router-dom';
import '../../../styles/teacher/InstructorExerciseDetail/exerciseSidebar.css';
import { formatVN } from '../../../../../../utils/dateTime';

function formatTime(value) {
    if (!value) return '';
    return formatVN(value, {
        day: '2-digit',
        month: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
    });
}

function initials(name) {
    return String(name || 'N')
        .trim()
        .split(/\s+/)
        .slice(0, 2)
        .map(part => part.charAt(0))
        .join('')
        .toUpperCase();
}

function clampPercent(value) {
    const number = Number(value);
    if (!Number.isFinite(number)) return 0;
    return Math.max(0, Math.min(100, Math.round(number)));
}

export default function ExerciseSidebar({
                                            data,
                                            type = 'quiz',
                                            loading = false,
                                            error = null,
                                            onAIGenerate,
                                            onExport,
                                            onBulkSettings,
                                        }) {
    const navigate = useNavigate();
    const {courseId} = useParams();
    const analytics = data?.analytics;
    const completionPercent = clampPercent(analytics?.completionPercent);
    const completionTrend = Number(analytics?.completionTrendPercent ?? 0);
    const activity = analytics?.activity ?? [];
    const recentSubmissions = analytics?.recentSubmissions ?? [];
    const maxActivity = Math.max(1, ...activity.map(item => Number(item.count) || 0));

    return (
        <aside className="exercise-sidebar">
            <div className="sidebar-card insights-card">
                <div className="card-header-flex">
                    <span className="card-tag">TỔNG QUAN KHÓA HỌC</span>
                    <span className="material-symbols-outlined icon-small">info</span>
                </div>
                <div className="gauge-container">
                    <div className="gauge-circle">
                        <svg viewBox="0 0 36 36" className="circular-chart">
                            <path className="circle-bg"
                                  d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"/>
                            <path className="circle" strokeDasharray={`${completionPercent}, 100`}
                                  d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"/>
                            <text x="18" y="20.35"
                                  className="percentage">{loading ? '...' : `${completionPercent}%`}</text>
                        </svg>
                    </div>
                    <div className="gauge-info">
                        <span className="gauge-label">Trung bình hoàn thành</span>
                        <span className={`gauge-trend ${completionTrend >= 0 ? 'positive' : ''}`}>
              <span
                  className="material-symbols-outlined">{completionTrend >= 0 ? 'trending_up' : 'trending_down'}</span>
                            {loading ? 'Đang tải' : `${completionTrend > 0 ? '+' : ''}${completionTrend}%`}
            </span>
                    </div>
                </div>
                {error && <p style={{margin: '12px 0 0', color: '#ef4444', fontSize: 12}}>{error}</p>}
            </div>

            <div className="sidebar-card activity-card">
                <h4 className="sidebar-section-title">Hoạt động nộp bài</h4>
                <div className="sparkline-mock">
                    {(loading ? Array.from({length: 7}, (_, index) => ({
                        label: '',
                        count: index + 1
                    })) : activity).map((item, index) => (
                        <div
                            key={`${item.date ?? index}-${index}`}
                            className="spark-bar"
                            title={`${item.label ?? ''}: ${item.count ?? 0}`}
                            style={{height: `${loading ? 35 + index * 6 : Math.max(6, ((Number(item.count) || 0) / maxActivity) * 100)}%`}}
                        />
                    ))}
                </div>
                <div className="spark-labels">
                    <span>{activity[0]?.label ?? ''}</span>
                    <span>{activity.at(-1)?.label ?? 'Hôm nay'}</span>
                </div>
            </div>

            <div className="sidebar-card submissions-card">
                <h4 className="sidebar-section-title">Nộp bài gần đây</h4>
                <div className="submission-list">
                    {loading && <span className="sub-time">Đang tải danh sách...</span>}
                    {!loading && recentSubmissions.length === 0 && (
                        <span className="sub-time">Chưa có lần nộp nào.</span>
                    )}
                    {!loading && recentSubmissions.map((sub, index) => (
                        <div key={`${sub.submittedAt ?? index}-${sub.studentName}`} className="submission-item">
                            <div className="sub-avatar">
                                {sub.avatarUrl ? <img src={sub.avatarUrl} alt="" style={{
                                    width: '100%',
                                    height: '100%',
                                    borderRadius: 8,
                                    objectFit: 'cover'
                                }}/> : initials(sub.studentName)}
                            </div>
                            <div className="sub-info">
                                <span className="sub-name">{sub.studentName}</span>
                                <span
                                    className="sub-time">{sub.exerciseTitle}<br></br>{formatTime(sub.submittedAt)}</span>
                            </div>
                            <span className="sub-score">{clampPercent(sub.scorePercent)}%</span>
                        </div>
                    ))}
                </div>
                <button
                    className="view-all-link"
                    onClick={() => navigate(
                        `/instructor/exercises/${courseId}/results?type=${type}`,
                        { state: data?.courseName ? { courseTitle: data.courseName } : undefined }
                    )}
                >
                    Xem tất cả {analytics?.totalSubmissions ?? 0} kết quả
                </button>
            </div>
        </aside>
    );
}
