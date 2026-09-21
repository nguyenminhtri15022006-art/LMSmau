import React, { useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import InspireImagesTab from './WebContentMgmt/InspireImagesTab';
import FoundersContentTab from './WebContentMgmt/FoundersContentTab';
import CourseCategoriesTab from './WebContentMgmt/CourseCategoriesTab';
import BlogCategoriesTab from './WebContentMgmt/BlogCategoriesTab';
import '../../../styles/admin/WebContentMgmt/WebContentMgmt.css';

const TAB_ITEMS = [
  { id: 'inspire', label: 'Ảnh Truyền Cảm Hứng' },
  { id: 'founders', label: 'Người sáng lập' },
  { id: 'categories', label: 'Thể loại Khóa học' },
  { id: 'blog-categories', label: 'Thể loại Blog' },
];

const TAB_COMPONENTS = {
  inspire: InspireImagesTab,
  founders: FoundersContentTab,
  categories: CourseCategoriesTab,
  'blog-categories': BlogCategoriesTab,
};

const WebContentMgmt = () => {
  const [activeTab, setActiveTab] = useState('inspire');
  const [visitedTabs, setVisitedTabs] = useState(['inspire']);
  const [toast, setToast] = useState({ show: false, message: '', type: 'success' });
  const toastTimerRef = useRef(null);

  const showToast = (message, type = 'success') => {
    if (toastTimerRef.current) {
      clearTimeout(toastTimerRef.current);
    }

    setToast({ show: true, message, type });
    toastTimerRef.current = setTimeout(() => {
      setToast({ show: false, message: '', type: 'success' });
      toastTimerRef.current = null;
    }, 3000);
  };

  useEffect(() => () => {
    if (toastTimerRef.current) {
      clearTimeout(toastTimerRef.current);
    }
  }, []);

  const handleTabChange = (tabId) => {
    setActiveTab(tabId);
    setVisitedTabs((prev) => (prev.includes(tabId) ? prev : [...prev, tabId]));
  };

  return (
    <motion.div
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.3 }}
      className="web-content-mgmt"
    >
      <AnimatePresence>
        {toast.show && (
          <motion.div
            initial={{ opacity: 0, y: -50, scale: 0.9 }}
            animate={{ opacity: 1, y: 16, scale: 1 }}
            exit={{ opacity: 0, y: -50, scale: 0.9 }}
            className={`web-content-toast web-content-toast--${toast.type}`}
          >
            <i className={`ti ti-${toast.type === 'success' ? 'circle-check' : 'circle-x'}`}></i>
            {toast.message}
          </motion.div>
        )}
      </AnimatePresence>

      <div className="admin-tabs web-content-tabs">
        {TAB_ITEMS.map((tab) => (
          <button
            key={tab.id}
            type="button"
            className={`admin-tab web-content-tab-button ${activeTab === tab.id ? 'active' : ''}`}
            onClick={() => handleTabChange(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {TAB_ITEMS.filter((tab) => visitedTabs.includes(tab.id)).map((tab) => {
        const TabComponent = TAB_COMPONENTS[tab.id];

        return (
          <div key={tab.id} hidden={activeTab !== tab.id}>
            <TabComponent showToast={showToast} />
          </div>
        );
      })}
    </motion.div>
  );
};

export default WebContentMgmt;
