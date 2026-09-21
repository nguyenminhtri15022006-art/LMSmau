import React from 'react';
import {motion} from 'framer-motion';
import AdminHomeStats from '../../components/admin/Home/AdminHomeStats';
import MicroservicesStatus from '../../components/admin/Home/MicroservicesStatus';
import SystemNotifications from '../../components/admin/Home/SystemNotifications';
import '../../styles/admin/AdminCommon/AdminCommon.css';

const HomePageAdmin = () => {
    return (
        <motion.div
            initial={{opacity: 0, y: 10}}
            animate={{opacity: 1, y: 0}}
            transition={{duration: 0.3}}
        >
            {/* Stats Grid */}
            <AdminHomeStats/>

            <div className="admin-grid-2">
                {/* Microservices Status */}
                <MicroservicesStatus/>

                {/* System Notifications */}
                <SystemNotifications/>
            </div>

        </motion.div>
    );
};


export default HomePageAdmin;
