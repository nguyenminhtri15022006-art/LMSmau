// components/Logo.jsx
// ==================================
// Logo của EduLearn (icon + tên)
// ==================================

import React from 'react';

function Logo() {
    return (
        <div className="logo-wrap">
            {/* Dùng Google Material Symbols (load trong index.html) */}
            <img src="/LMSicon.png" alt="LMSicon Logo" style={{width: "60px", height: "50px"}}/>
            <span className="logo-name">DevNest</span>
        </div>
    );
}

export default Logo;
