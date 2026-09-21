import React, { useState } from 'react';
import '../styles/RoleSelectionModal.css';
import RoleSelector from './RoleSelector';

function RoleSelectionModal({ onConfirm, onCancel, provider }) {
    const [selectedRole, setSelectedRole] = useState(null);

    const handleConfirm = () => {
        if (selectedRole) {
            onConfirm(selectedRole, provider);
        }
    };

    return (
        <div className="role-modal-overlay">
            <div className="role-modal-container">
                <div className="role-modal-content">
                    <div className="role-modal-header">
                        <h2>Chọn vai trò</h2>
                        <p>Trước khi tiếp tục với Google, vui lòng chọn cách bạn muốn sử dụng EduLearn.</p>
                    </div>

                    <div className="role-selector-wrapper">
                        <RoleSelector
                            selectedRole={selectedRole}
                            onSelect={(role) => setSelectedRole(role.toUpperCase())}
                        />
                    </div>

                    <div className="modal-footer">
                        <button
                            className="btn-confirm"
                            disabled={!selectedRole}
                            onClick={handleConfirm}
                        >
                            Tiếp tục
                        </button>
                        <button className="btn-cancel" onClick={onCancel}>
                            Hủy
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default RoleSelectionModal;
