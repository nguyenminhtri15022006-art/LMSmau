import React, { useState, useEffect } from 'react';
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
  };
};

const CodeJudgeActivity = () => {
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

  const renderBody = () => {
    if (loading) {
      return (
        <tr>
          <td colSpan={6} style={{ color: 'var(--admin-muted)', fontSize: '0.85rem' }}>
            Đang tải hoạt động Code Judge...
          </td>
        </tr>
      );
    }

    if (error) {
      return (
        <tr>
          <td colSpan={6} style={{ color: 'var(--admin-red)', fontSize: '0.85rem' }}>
            Không thể tải hoạt động Code Judge.
          </td>
        </tr>
      );
    }

    if (activities.length === 0) {
      return (
        <tr>
          <td colSpan={6} style={{ color: 'var(--admin-muted)', fontSize: '0.85rem' }}>
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
  );
};

export default CodeJudgeActivity;
