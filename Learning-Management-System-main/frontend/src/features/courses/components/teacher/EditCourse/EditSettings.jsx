import React from 'react';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faExclamationTriangle, faEye, faUserPlus} from '@fortawesome/free-solid-svg-icons';
import '../../../styles/teacher/EditCourse/editSettings.css';

const EditSettings = ({data, updateData, onDelete}) => {
    return (
        <div className="edit-settings-container">
            <div className="settings-section">
                <div className="section-header">
                    <FontAwesomeIcon icon={faEye} className="section-icon text-blue"/>
                    <h3>Hiển thị và trạng thái</h3>
                </div>
                <div className="radio-group">
                    <label className={`radio-option ${data.status === 'PUBLIC' ? 'selected' : ''}`}>
                        <div className="radio-control">
                            <input
                                type="radio"
                                name="status"
                                value="PUBLIC"
                                checked={data.status === 'PUBLIC'}
                                onChange={() => updateData({status: 'PUBLIC'})}
                            />
                            <span className="custom-radio"></span>
                        </div>
                        <div className="radio-text">
                            <h4>Công khai</h4>
                            <p>Bất kỳ ai cũng có thể tìm thấy và đăng ký khóa học này. Nó sẽ xuất hiện trong kết quả tìm
                                kiếm.</p>
                        </div>
                    </label>

                    <label className={`radio-option ${data.status === 'PRIVATE' ? 'selected' : ''}`}>
                        <div className="radio-control">
                            <input
                                type="radio"
                                name="status"
                                value="PRIVATE"
                                checked={data.status === 'PRIVATE'}
                                onChange={() => updateData({status: 'PRIVATE'})}
                            />
                            <span className="custom-radio"></span>
                        </div>
                        <div className="radio-text">
                            <h4>Bản nháp</h4>
                            <p>Chỉ hiển thị với bạn và những người được ủy quyền cộng tác. Việc đăng ký đã đóng.</p>
                        </div>
                    </label>
                </div>
            </div>

            <div className="settings-section">
                <div className="section-header">
                    <FontAwesomeIcon icon={faUserPlus} className="section-icon text-blue"/>
                    <h3>Chuyển quyền sở hữu</h3>
                </div>
                <div className="transfer-box">
                    <p>Chuyển khóa học này sang tài khoản của giáo viên khác. Thao tác này không thể hoàn tác sau khi
                        được chấp nhận.</p>
                    <div className="transfer-input-group">
                        <input
                            type="email"
                            placeholder="Địa chỉ email của chủ sở hữu mới"
                            value={data.transferEmail || ''}
                            onChange={(e) => updateData({transferEmail: e.target.value})}
                        />
                        <button type="button" className="btn-transfer">Khởi tạo chuyển giao</button>
                    </div>
                </div>
            </div>

            <div className="settings-section danger-zone-section">
                <div className="section-header danger-header">
                    <FontAwesomeIcon icon={faExclamationTriangle} className="section-icon text-red"/>
                    <h3 className="text-red">Khu vực nguy hiểm</h3>
                </div>
                <div className="danger-box">
                    <div className="danger-text">
                        <h4>Xóa khóa học này</h4>
                        <p>Một khi bạn xóa một khóa học, không có đường quay lại. Hãy chắc chắn.</p>
                    </div>
                    <button type="button" className="btn-delete-course" onClick={onDelete}>
                        Xóa khóa học
                    </button>
                </div>
            </div>
        </div>
    );
};

export default EditSettings;
