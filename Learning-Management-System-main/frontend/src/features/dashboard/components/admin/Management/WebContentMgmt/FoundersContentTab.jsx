import React, { useState } from 'react';
import { DEFAULT_FOUNDERS_CONTENT } from '../../../../../aboutus/constants/foundersContent';
import '../../../../styles/admin/WebContentMgmt/FoundersContentTab.css';

const FOUNDER_AVATAR_MAX_BYTES = 1024 * 1024;

const getFileExtension = (fileName = '') => {
  const parts = fileName.split('.');
  return parts.length > 1 ? parts.pop().toLowerCase() : '';
};

const isPngFile = (file) => {
  if (!file) return false;

  const extension = getFileExtension(file.name);
  const hasPngExtension = extension === 'png';
  const hasPngMimeType = !file.type || file.type === 'image/png';

  return hasPngExtension && hasPngMimeType;
};

const readFileAsDataUrl = (file) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(file);
  });

const isValidAvatarSource = (value) => {
  const trimmedValue = value.trim();

  if (!trimmedValue) return false;
  if (trimmedValue.startsWith('data:image/png;base64,')) return true;

  try {
    const url = new URL(trimmedValue);
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch (error) {
    return false;
  }
};

const FoundersContentTab = ({ showToast }) => {
  const [content, setContent] = useState(DEFAULT_FOUNDERS_CONTENT);

  const updateSectionField = (field, value) => {
    setContent((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const updateFounder = (founderIndex, field, value) => {
    setContent((prev) => ({
      ...prev,
      founders: prev.founders.map((founder, index) =>
        index === founderIndex ? { ...founder, [field]: value } : founder,
      ),
    }));
  };

  const handleAvatarFileChange = async (founderIndex, event) => {
    const file = event.target.files?.[0];
    event.target.value = '';

    if (!file) return;

    if (!isPngFile(file)) {
      showToast('Ảnh đại diện chỉ hỗ trợ file PNG.', 'danger');
      return;
    }

    if (file.size > FOUNDER_AVATAR_MAX_BYTES) {
      showToast('Ảnh PNG không được vượt quá 1MB.', 'danger');
      return;
    }

    try {
      const dataUrl = await readFileAsDataUrl(file);
      updateFounder(founderIndex, 'avatar', dataUrl);
      showToast('Đã chọn ảnh PNG, bấm lưu để áp dụng');
    } catch (error) {
      console.error(error);
      showToast('Không thể đọc file ảnh PNG này.', 'danger');
    }
  };

  const saveFoundersContent = () => {
    const hasInvalidAvatar = content.founders.some((founder) => !isValidAvatarSource(founder.avatar));

    if (hasInvalidAvatar) {
      showToast('Ảnh đại diện phải là URL http/https hoặc file PNG hợp lệ.', 'danger');
      return;
    }

    showToast('Nội dung người sáng lập đang được set cứng trong code.');
  };

  const resetFoundersContent = () => {
    if (!window.confirm('Khôi phục nội dung đội ngũ sáng lập về mặc định?')) return;

    setContent(DEFAULT_FOUNDERS_CONTENT);
    showToast('Đã khôi phục nội dung đội ngũ sáng lập');
  };

  return (
    <div className="web-content-founders-layout">
      <div className="admin-card web-content-founders-section-card">
        <div className="admin-card-hd">
          <i className="ti ti-users-group web-content-founders-heading-icon"></i>
          Nội dung section người sáng lập
        </div>

        <div className="admin-card-body web-content-founders-section-body">
          <div className="admin-grid-2 web-content-founders-section-grid">
            <div className="admin-field web-content-field-reset">
              <label>Nhãn phụ</label>
              <input
                value={content.eyebrow}
                onChange={(event) => updateSectionField('eyebrow', event.target.value)}
              />
            </div>

            <div className="admin-field web-content-field-reset">
              <label>Tiêu đề section</label>
              <input
                value={content.title}
                onChange={(event) => updateSectionField('title', event.target.value)}
              />
            </div>
          </div>

          <div className="admin-field web-content-field-reset">
            <label>Mô tả section</label>
            <textarea
              className="web-content-founders-description"
              value={content.description}
              onChange={(event) => updateSectionField('description', event.target.value)}
            />
          </div>
        </div>
      </div>

      <div className="admin-grid-3 web-content-founders-grid">
        {content.founders.map((founder, index) => (
          <div key={founder.id} className="admin-card web-content-founder-editor">
            <div className="admin-card-hd">
              <i className="ti ti-user-circle web-content-founders-heading-icon"></i>
              Người sáng lập #{index + 1}
            </div>

            <div className="admin-card-body web-content-founder-editor-body">
              <div className="web-content-founder-preview">
                <img src={founder.avatar} alt={`Xem trước ảnh của ${founder.name}`} />
                <div>
                  <strong>{founder.name || `Người sáng lập #${index + 1}`}</strong>
                  <span>{founder.role}</span>
                </div>
              </div>

              <div className="admin-field web-content-field-reset">
                <label>Họ tên</label>
                <input value={founder.name} onChange={(event) => updateFounder(index, 'name', event.target.value)} />
              </div>

              <div className="admin-field web-content-field-reset">
                <label>Vai trò</label>
                <input value={founder.role} onChange={(event) => updateFounder(index, 'role', event.target.value)} />
              </div>

              <div className="admin-field web-content-field-reset">
                <label>URL ảnh đại diện hoặc data PNG</label>
                <input
                  value={founder.avatar}
                  placeholder="Dán URL ảnh http/https hoặc chọn file PNG bên dưới"
                  onChange={(event) => updateFounder(index, 'avatar', event.target.value)}
                />
              </div>

              <div className="admin-field web-content-field-reset">
                <label>Tải ảnh PNG từ máy</label>
                <input
                  type="file"
                  accept="image/png,.png"
                  onChange={(event) => handleAvatarFileChange(index, event)}
                />
                <p className="web-content-founder-upload-note">Chỉ nhận PNG, tối đa 1MB.</p>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="web-content-founders-actions">
        <button className="admin-btn admin-btn-primary" type="button" onClick={saveFoundersContent}>
          <i className="ti ti-device-floppy"></i>
          Lưu nội dung người sáng lập
        </button>
        <button className="admin-btn" type="button" onClick={resetFoundersContent}>
          <i className="ti ti-refresh"></i>
          Khôi phục mặc định
        </button>
      </div>
    </div>
  );
};

export default FoundersContentTab;
