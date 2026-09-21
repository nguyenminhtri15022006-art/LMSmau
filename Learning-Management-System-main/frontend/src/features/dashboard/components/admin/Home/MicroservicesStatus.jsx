import React, { useState, useEffect } from 'react';
import dashboardApi from '../../../../../services/dashboard.api';
import { ADMIN_HEALTH_REFRESH_INTERVAL_MS } from '../../../../../configurations/env';

const MicroservicesStatus = () => {
  const [services, setServices] = useState([
    { name: 'Identity Service', port: ':8081', status: 'Loading...', dotClass: '', pillClass: '' },
    { name: 'Course Service', port: ':8082', status: 'Loading...', dotClass: '', pillClass: '' },
    { name: 'Assignment Service', port: ':8083', status: 'Loading...', dotClass: '', pillClass: '' },
    { name: 'Blog Service', port: ':8084', status: 'Loading...', dotClass: '', pillClass: '' },
    { name: 'Chatbot Service', port: ':8085', status: 'Loading...', dotClass: '', pillClass: '' },
    { name: 'API Gateway', port: ':8080', status: 'Loading...', dotClass: '', pillClass: '' }
  ]);

  const getStatusClasses = (status) => {
    switch (status) {
      case 'Online': return { dot: 'admin-dot-green', pill: 'admin-pill-green' };
      case 'Offline': return { dot: 'admin-dot-red', pill: 'admin-pill-red' };
      default: return { dot: '', pill: '' };
    }
  };

  useEffect(() => {
    let isMounted = true;
    const fetchStatus = async () => {
      try {
        const results = await dashboardApi.getMicroservicesStatus();
        if (!isMounted || !Array.isArray(results)) return;

        setServices(results.map(res => {
          const classes = getStatusClasses(res.status);
          return {
            name: res.name || 'Unknown Service',
            port: res.port || '',
            status: res.status || 'Offline',
            dotClass: classes.dot,
            pillClass: classes.pill
          };
        }));
      } catch (error) {
        console.error('Failed to fetch microservices status:', error);
      }
    };

    fetchStatus();
    const interval = setInterval(fetchStatus, ADMIN_HEALTH_REFRESH_INTERVAL_MS);
    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, []);

  return (
    <div className="admin-card">
      <div className="admin-card-hd">
        <i className="ti ti-server"></i> 
        Trạng thái Microservices
      </div>
      <div className="admin-card-body">
        {services.map((svc, index) => (
          <div key={index} className="admin-service-row">
            <span className={`admin-dot ${svc.dotClass}`}></span>
            <span className="svc-name" style={{ flex: 1, fontWeight: 500 }}>{svc.name}</span>
            <span style={{ fontSize: '0.75rem', color: 'var(--admin-muted)', marginRight: '10px' }}>{svc.port}</span>
            <span className={`admin-status-pill ${svc.pillClass}`}>{svc.status}</span>
          </div>
        ))}
      </div>
    </div>
  );
};

export default MicroservicesStatus;
