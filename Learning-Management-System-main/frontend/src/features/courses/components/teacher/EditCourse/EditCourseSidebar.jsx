import React from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faInfoCircle, faList } from '@fortawesome/free-solid-svg-icons';
import '../../../styles/teacher/EditCourse/editCourseSidebar.css';

const EditCourseSidebar = ({ activeTab, setActiveTab }) => {
    return (
        <div className="edit-course-sidebar">
            <h3 className="sidebar-title">CHỈNH SỬA KHÓA HỌC</h3>
            <nav className="sidebar-nav">
                <button
                    className={`sidebar-item ${activeTab === 'info' ? 'active' : ''}`}
                    onClick={() => setActiveTab('info')}
                >
                    <FontAwesomeIcon icon={faInfoCircle} className="sidebar-icon" />
                    <span>Thông tin khóa học</span>
                </button>
                <button
                    className={`sidebar-item ${activeTab === 'curriculum' ? 'active' : ''}`}
                    onClick={() => setActiveTab('curriculum')}
                >
                    <FontAwesomeIcon icon={faList} className="sidebar-icon" />
                    <span>Nội dung khóa học</span>
                </button>
            </nav>
        </div>
    );
};

export default EditCourseSidebar;
