import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import dashboardApi from '../../../../../services/dashboard.api';

const LANGUAGE_LABELS = {
  JAVA: 'Java',
  PYTHON: 'Python',
  CPP: 'C++',
  CPLUSPLUS: 'C++',
  JAVASCRIPT: 'JavaScript',
};

const getRateColor = (rate) => {
  if (rate >= 75) return 'var(--admin-green)';
  if (rate >= 50) return 'var(--admin-yellow)';
  return 'var(--admin-red)';
};

const formatActivity = (item) => {
  const acceptanceRate = Number(item.acceptanceRate || 0);
  return {
    lang: LANGUAGE_LABELS[item.language] || item.language || 'Khác',
    total: Number(item.total || 0),
    accepted: Number(item.accepted || 0),
    wa: Number(item.wrongAnswer || 0),
    re: Number(item.runtimeError || 0),
    rate: `${acceptanceRate}%`,
    rateColor: getRateColor(acceptanceRate),
    avgTimeMs: Number(item.avgTimeMs || 0),
  };
};

const CodeJudgeMgmt = () => {
  const [activities, setActivities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    let isMounted = true;
    const fetchData = async () => {
      setLoading(true);
      setError(false);
      try {
        const data = await dashboardApi.getJudgeActivity();
        if (isMounted) {
          setActivities(Array.isArray(data) ? data.map(formatActivity) : []);
        }
      } catch (err) {
        console.error('Failed to fetch judge activity:', err);
        if (isMounted) {
          setError(true);
          setActivities([]);
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    fetchData();
    return () => { isMounted = false; };
  }, []);

  // Calculate statistics from backend activities list
  const totalSubmissions = activities.reduce((sum, act) => sum + act.total, 0);
  const totalRuntimeErrors = activities.reduce((sum, act) => sum + act.re, 0);
  const errorRate = totalSubmissions > 0 ? ((totalRuntimeErrors / totalSubmissions) * 100).toFixed(1) : '0.0';

  // Compute weighted average execution time in seconds
  const totalExecTime = activities.reduce((sum, act) => sum + (act.avgTimeMs * act.total), 0);
  const avgTime = totalSubmissions > 0 ? (totalExecTime / totalSubmissions / 1000).toFixed(2) + 's' : '0.0s';

  const renderBody = () => {
    if (loading) {
      return (
        <tr>
          <td colSpan={6} style={{ color: 'var(--admin-muted)', fontSize: '0.85rem', textAlign: 'center', padding: '20px' }}>
            Đang tải hoạt động Code Judge...
          </td>
        </tr>
      );
    }

    if (error) {
      return (
        <tr>
          <td colSpan={6} style={{ color: 'var(--admin-red)', fontSize: '0.85rem', textAlign: 'center', padding: '20px' }}>
            Không thể tải hoạt động Code Judge.
          </td>
        </tr>
      );
    }

    if (activities.length === 0) {
      return (
        <tr>
          <td colSpan={6} style={{ color: 'var(--admin-muted)', fontSize: '0.85rem', textAlign: 'center', padding: '20px' }}>
            Chưa có bài nộp trong 24h gần nhất.
          </td>
        </tr>
      );
    }

    return activities.map((act) => (
      <tr key={act.lang}>
        <td><span className="admin-status-pill admin-pill-blue">{act.lang}</span></td>
        <td>{act.total}</td>
        <td style={{ color: 'var(--admin-green)', fontWeight: 600 }}>{act.accepted}</td>
        <td style={{ color: 'var(--admin-yellow)' }}>{act.wa}</td>
        <td style={{ color: 'var(--admin-red)' }}>{act.re}</td>
        <td>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div style={{ flex: 1, height: '6px', background: 'var(--admin-card-hover)', borderRadius: '3px', overflow: 'hidden' }}>
              <div style={{ width: act.rate, height: '100%', background: act.rateColor }}></div>
            </div>
            <span style={{ fontSize: '0.75rem', minWidth: '30px' }}>{act.rate}</span>
          </div>
        </td>
      </tr>
    ));
  };

  return (
    <motion.div 
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.3 }}
    >
      <div className="admin-stats-grid">
        <div className="admin-stat-card">
          <div className="admin-stat-label">Tổng bài nộp</div>
          <div className="admin-stat-val" style={{ color: 'var(--admin-accent)' }}>
            {loading ? '...' : totalSubmissions}
          </div>
          <div className="admin-stat-sub">24h qua</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Lỗi Runtime</div>
          <div className="admin-stat-val" style={{ color: 'var(--admin-red)' }}>
            {loading ? '...' : totalRuntimeErrors}
          </div>
          <div className="admin-stat-sub neg">{loading ? '...' : `${errorRate}% tỉ lệ lỗi`}</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Queue Judge0</div>
          <div className="admin-stat-val" style={{ color: 'var(--admin-yellow)' }}>0</div>
          <div className="admin-stat-sub">Đang chờ xử lý</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Avg Time</div>
          <div className="admin-stat-val" style={{ color: 'var(--admin-green)' }}>
            {loading ? '...' : avgTime}
          </div>
          <div className="admin-stat-sub">Thời gian trung bình</div>
        </div>
      </div>

      <div className="admin-card">
        <div className="admin-card-hd">
          <i className="ti ti-activity"></i>
          Hoạt động Code Judge (24h gần nhất)
        </div>
        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Ngôn ngữ</th>
                <th>Tổng nộp</th>
                <th>Accepted</th>
                <th>Wrong Answer</th>
                <th>Runtime Error</th>
                <th style={{ width: '200px' }}>Tỉ lệ AC</th>
              </tr>
            </thead>
            <tbody>{renderBody()}</tbody>
          </table>
        </div>
      </div>
    </motion.div>
  );
};

export default CodeJudgeMgmt;
