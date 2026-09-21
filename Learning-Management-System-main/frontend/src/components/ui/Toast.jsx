import React, { useState, useCallback, useEffect, createContext, useContext, useRef } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faCheckCircle, faTimesCircle, faInfoCircle,
    faExclamationTriangle, faTimes,
} from '@fortawesome/free-solid-svg-icons';
import './styles/Toast.css';

const DURATION = 4000;

const ICONS = {
    success: faCheckCircle,
    error:   faTimesCircle,
    info:    faInfoCircle,
    confirm: faExclamationTriangle,
};

const TITLES = {
    success: 'Thành công',
    error:   'Lỗi',
    info:    'Thông báo',
    confirm: 'Xác nhận',
};

/* ── Regular toast ── */
const ToastItem = ({ id, type, message, title, onRemove }) => {
    const [hiding, setHiding] = useState(false);
    const timerRef = useRef(null);

    const dismiss = useCallback(() => {
        setHiding(true);
        setTimeout(() => onRemove(id), 260);
    }, [id, onRemove]);

    useEffect(() => {
        timerRef.current = setTimeout(dismiss, DURATION);
        return () => clearTimeout(timerRef.current);
    }, [dismiss]);

    return (
        <div className={`toast toast-${type} ${hiding ? 'toast-hiding' : ''}`}>
            <div className="toast-icon">
                <FontAwesomeIcon icon={ICONS[type]} />
            </div>
            <div className="toast-body">
                <p className="toast-title">{title || TITLES[type]}</p>
                {message && <p className="toast-message">{message}</p>}
            </div>
            <button className="toast-close" onClick={dismiss}>
                <FontAwesomeIcon icon={faTimes} />
            </button>
            <div className="toast-progress" style={{ animationDuration: `${DURATION}ms` }} />
        </div>
    );
};

/* ── Confirm toast ── */
const ConfirmToastItem = ({ id, title, message, confirmLabel, cancelLabel, onConfirm, onRemove }) => {
    const [hiding, setHiding] = useState(false);

    const dismiss = useCallback(() => {
        setHiding(true);
        setTimeout(() => onRemove(id), 260);
    }, [id, onRemove]);

    const handleConfirm = () => {
        onConfirm();
        dismiss();
    };

    return (
        <div className={`toast toast-confirm ${hiding ? 'toast-hiding' : ''}`}>
            <div className="toast-icon">
                <FontAwesomeIcon icon={ICONS.confirm} />
            </div>
            <div className="toast-body">
                <p className="toast-title">{title || TITLES.confirm}</p>
                {message && <p className="toast-message">{message}</p>}
                <div className="toast-confirm-actions">
                    <button className="toast-btn-confirm" onClick={handleConfirm}>
                        {confirmLabel || 'Xác nhận'}
                    </button>
                    <button className="toast-btn-cancel" onClick={dismiss}>
                        {cancelLabel || 'Hủy'}
                    </button>
                </div>
            </div>
            <button className="toast-close" onClick={dismiss}>
                <FontAwesomeIcon icon={faTimes} />
            </button>
        </div>
    );
};

/* ── Context ── */
const ToastContext = createContext(null);

export const ToastProvider = ({ children }) => {
    const [toasts, setToasts] = useState([]);

    const remove = useCallback((id) => {
        setToasts(prev => prev.filter(t => t.id !== id));
    }, []);

    const show = useCallback((type, message, title) => {
        const id = Date.now() + Math.random();
        setToasts(prev => [...prev, { id, type, message, title }]);
    }, []);

    const confirm = useCallback((message, onConfirm, { title, confirmLabel, cancelLabel } = {}) => {
        const id = Date.now() + Math.random();
        setToasts(prev => [...prev, {
            id, type: 'confirm', message, title, confirmLabel, cancelLabel, onConfirm,
        }]);
    }, []);

    const toast = {
        success: (message, title)                             => show('success', message, title),
        error:   (message, title)                             => show('error',   message, title),
        info:    (message, title)                             => show('info',    message, title),
        confirm: (message, onConfirm, opts)                   => confirm(message, onConfirm, opts ?? {}),
    };

    return (
        <ToastContext.Provider value={toast}>
            {children}
            <div className="toast-container">
                {toasts.map(t =>
                    t.type === 'confirm' ? (
                        <ConfirmToastItem key={t.id} {...t} onRemove={remove} />
                    ) : (
                        <ToastItem key={t.id} {...t} onRemove={remove} />
                    )
                )}
            </div>
        </ToastContext.Provider>
    );
};

export const useToast = () => {
    const ctx = useContext(ToastContext);
    if (!ctx) throw new Error('useToast must be used inside <ToastProvider>');
    return ctx;
};
