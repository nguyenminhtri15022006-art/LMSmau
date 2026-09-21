import React from 'react';
import '../styles/AssignmentMessageDialog.css';

const ICON_BY_TONE = {
  error: 'error',
  warning: 'warning',
  info: 'info',
  success: 'check_circle',
};

export default function AssignmentMessageDialog({
  open = true,
  tone = 'error',
  title = 'Thông báo',
  message,
  detail,
  confirmLabel = 'Đã hiểu',
  onClose,
}) {
  if (!open) return null;

  const icon = ICON_BY_TONE[tone] || ICON_BY_TONE.info;

  return (
    <div className="assignment-message-backdrop" role="presentation" onClick={onClose}>
      <div
        className={`assignment-message-dialog assignment-message-dialog--${tone}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby="assignment-message-title"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="assignment-message-icon">
          <span className="material-symbols-outlined">{icon}</span>
        </div>
        <div className="assignment-message-body">
          <h2 id="assignment-message-title">{title}</h2>
          {message && <p>{message}</p>}
          {detail && <div className="assignment-message-detail">{detail}</div>}
        </div>
        <div className="assignment-message-actions">
          <button type="button" className="assignment-message-button" onClick={onClose}>
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
