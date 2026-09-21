import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import { useLocation } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';
import { ENDPOINTS } from '../../constants/endpoints';
import { formatTimeVN, formatVN, parseBackendUtcDate } from '../../utils/dateTime';
import { useProblemContext } from '../../context/ProblemContext';
import '../ui/styles/ChatBot.css';

// ─── Route config ─────────────────────────────────────────────────────────────

const ROUTE_CONFIGS = {
    // ── PROBLEM context — Socratic tutor ────────────
    '/exercises/challenge': {
        title: 'Trợ lý bài tập',
        subtitle: 'Gợi ý theo phương pháp Socratic',
        icon: 'psychology',
        fabIcon: 'psychology',
        fabColor: '#059669',
        placeholder: 'Bạn đang gặp khó khăn ở đâu?',
        contextType: 'PROBLEM',
        initialMessage: 'Xin chào! Hãy mô tả khó khăn bạn đang gặp. Tôi sẽ đặt câu hỏi gợi mở để bạn tự tìm ra hướng giải quyết.',
    },
    // ── COURSE context ──────────────────────────────
    '/list-course': {
        title: 'Gợi ý khóa học',
        subtitle: 'AI tư vấn khóa học phù hợp',
        icon: 'school',
        fabIcon: 'school',
        fabColor: '#7c3aed',
        placeholder: 'Bạn muốn học gì? (VD: lập trình web, thiết kế...)',
        contextType: 'COURSE',
        initialMessage: 'Xin chào! Tôi có thể giúp bạn tìm khóa học phù hợp. Bạn đang quan tâm đến lĩnh vực nào?',
    },
    '/course-hub': {
        title: 'AI Trợ lý',
        subtitle: 'EduLearn - Trí tuệ nhân tạo',
        icon: 'auto_awesome',
        fabIcon: 'auto_awesome',
        fabColor: '#2563eb',
        placeholder: 'Hỏi tôi bất cứ điều gì...',
        contextType: 'GENERAL',
        initialMessage: 'Xin chào! Tôi là trợ lý AI của EduLearn. Tôi có thể giúp gì cho bạn?',
    },
};

const DEFAULT_CONFIG = {
    title: 'AI Trợ lý',
    subtitle: 'EduLearn - Trí tuệ nhân tạo',
    icon: 'auto_awesome',
    fabIcon: 'auto_awesome',
    fabColor: '#2563eb',
    placeholder: 'Hỏi tôi bất cứ điều gì...',
    contextType: 'GENERAL',
    initialMessage: 'Xin chào! Tôi là trợ lý AI của EduLearn. Tôi có thể giúp gì cho bạn?',
};

function getConfig(pathname) {
    if (ROUTE_CONFIGS[pathname]) return ROUTE_CONFIGS[pathname];
    const key = Object.keys(ROUTE_CONFIGS).find(r => pathname.startsWith(r));
    return key ? ROUTE_CONFIGS[key] : DEFAULT_CONFIG;
}

function useContextRefId(pathname, search) {
    const ctx = useProblemContext();
    if (pathname.startsWith('/exercises/challenge')) {
        return ctx?.problemId != null ? String(ctx.problemId) : null;
    }
    if (pathname.startsWith('/course-hub') || pathname.startsWith('/list-course/detail-course')) {
        return new URLSearchParams(search).get('id') || null;
    }
    return null;
}

function getToken() {
    return localStorage.getItem('access_token') || sessionStorage.getItem('access_token') || '';
}

function authHeader() {
    return { 'Authorization': `Bearer ${getToken()}` };
}

function formatTime(dateStr) {
    return formatTimeVN(dateStr || new Date());
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = parseBackendUtcDate(dateStr);
    if (!d) return '';
    const diff = Math.floor((Date.now() - d.getTime()) / 86400000);
    if (diff === 0) return 'Hôm nay';
    if (diff === 1) return 'Hôm qua';
    if (diff < 7) return `${diff} ngày trước`;
    return formatVN(d, { day: '2-digit', month: '2-digit' });
}

// ─── Markdown renderer ────────────────────────────────────────────────────────

function renderInline(text) {
    // bold + italic → tách thành spans
    const parts = [];
    let remaining = text;
    let key = 0;

    while (remaining.length > 0) {
        const boldMatch = remaining.match(/\*\*(.+?)\*\*/);
        const italicMatch = remaining.match(/(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)/);

        let nextMatch = null;
        if (boldMatch && italicMatch) {
            nextMatch = boldMatch.index <= italicMatch.index ? { match: boldMatch, type: 'bold' } : { match: italicMatch, type: 'italic' };
        } else if (boldMatch) nextMatch = { match: boldMatch, type: 'bold' };
        else if (italicMatch) nextMatch = { match: italicMatch, type: 'italic' };

        if (!nextMatch) {
            parts.push(<span key={key++}>{remaining}</span>);
            break;
        }

        const { match, type } = nextMatch;
        if (match.index > 0) parts.push(<span key={key++}>{remaining.slice(0, match.index)}</span>);

        if (type === 'bold') parts.push(<strong key={key++}>{match[1]}</strong>);
        else parts.push(<em key={key++}>{match[1]}</em>);

        remaining = remaining.slice(match.index + match[0].length);
    }

    return parts;
}

function MarkdownContent({ content, streaming }) {
    if (!content && !streaming) return null;

    const lines = content.split('\n');
    const elements = [];
    let listType = null;
    let listItems = [];
    let key = 0;

    const flushList = () => {
        if (listItems.length === 0) return;
        const Tag = listType === 'ol' ? 'ol' : 'ul';
        elements.push(
            <Tag key={key++} className="chatbot-md-list">
                {listItems.map((li, i) => <li key={i}>{renderInline(li)}</li>)}
            </Tag>
        );
        listItems = [];
        listType = null;
    };

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];

        // Heading
        const h3 = line.match(/^###\s+(.+)/);
        const h2 = line.match(/^##\s+(.+)/);
        const h1 = line.match(/^#\s+(.+)/);
        if (h3 || h2 || h1) {
            flushList();
            const [match, Tag, cls] = h3 ? [h3, 'h3', 'chatbot-md-h3'] : h2 ? [h2, 'h2', 'chatbot-md-h2'] : [h1, 'h1', 'chatbot-md-h1'];
            elements.push(<Tag key={key++} className={cls}>{renderInline(match[1])}</Tag>);
            continue;
        }

        // Horizontal rule
        if (/^---+$/.test(line.trim())) {
            flushList();
            elements.push(<hr key={key++} className="chatbot-md-hr" />);
            continue;
        }

        // Unordered list
        const ulMatch = line.match(/^[-*]\s+(.+)/);
        if (ulMatch) {
            if (listType === 'ol') flushList();
            listType = 'ul';
            listItems.push(ulMatch[1]);
            continue;
        }

        // Ordered list
        const olMatch = line.match(/^\d+\.\s+(.+)/);
        if (olMatch) {
            if (listType === 'ul') flushList();
            listType = 'ol';
            listItems.push(olMatch[1]);
            continue;
        }

        // Flush list before non-list content
        flushList();

        // Empty line → spacer
        if (line.trim() === '') {
            elements.push(<div key={key++} className="chatbot-md-spacer" />);
            continue;
        }

        // Regular paragraph
        elements.push(<p key={key++} className="chatbot-md-p">{renderInline(line)}</p>);
    }

    flushList();

    return (
        <div className="chatbot-md">
            {elements}
            {streaming && <span className="chatbot-cursor">▍</span>}
        </div>
    );
}

// ─── History panel ────────────────────────────────────────────────────────────

function HistoryPanel({ contextType, onSelectSession, onClose, activeSessionId }) {
    const [sessions, setSessions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        setLoading(true);
        setError(null);
        fetch(`${ENDPOINTS.CHATBOT.SESSIONS}?size=50`, { headers: authHeader() })
            .then(async r => {
                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                return r.json();
            })
            .then(data => {
                const all = data?.data?.content ?? data?.data ?? [];
                const list = Array.isArray(all) ? all : [];
                setSessions(list.filter(s => s.contextType === contextType));
            })
            .catch(err => {
                console.error('[History] fetch sessions error:', err);
                setError('Không thể tải lịch sử');
                setSessions([]);
            })
            .finally(() => setLoading(false));
    }, []);

    return (
        <div className="chatbot-history-panel">
            <div className="chatbot-history-header">
                <span>Lịch sử trò chuyện</span>
                <button onClick={onClose} className="chatbot-close">
                    <span className="material-symbols-outlined">close</span>
                </button>
            </div>
            <div className="chatbot-history-list">
                {loading ? (
                    <div className="chatbot-history-empty">Đang tải...</div>
                ) : error ? (
                    <div className="chatbot-history-empty" style={{ color: '#ef4444' }}>{error}</div>
                ) : sessions.length === 0 ? (
                    <div className="chatbot-history-empty">Chưa có lịch sử</div>
                ) : (
                    sessions.map(s => (
                        <button
                            key={s.id}
                            className={`chatbot-history-item ${s.id === activeSessionId ? 'active' : ''}`}
                            onClick={() => onSelectSession(s)}
                        >
                            <div className="chatbot-history-title">{s.title || 'Cuộc trò chuyện'}</div>
                            <div className="chatbot-history-meta">
                                <span>
                                    {s.contextType === 'COURSE' ? '📚 Gợi ý' : s.contextType === 'PROBLEM' ? '🧩 Bài tập' : '💬 Trợ lý'}
                                    {' • '}{s.totalMessages} tin{' • '}{formatDate(s.lastMessageAt)}
                                </span>
                            </div>
                        </button>
                    ))
                )}
            </div>
        </div>
    );
}

// ─── Main component ───────────────────────────────────────────────────────────

export default function Chatbot() {
    const { user } = useAuth();
    const location = useLocation();
    const config = useMemo(() => getConfig(location.pathname), [location.pathname]);
    const contextRefId = useContextRefId(location.pathname, location.search);

    const [isOpen, setIsOpen] = useState(false);
    const [showHistory, setShowHistory] = useState(false);
    const [imgError, setImgError] = useState(false);
    const [messages, setMessages] = useState([]);
    const [newMessage, setNewMessage] = useState('');
    const [isStreaming, setIsStreaming] = useState(false);
    const [loadingHistory, setLoadingHistory] = useState(false);
    const [historyError, setHistoryError] = useState(null);

    const sessionIdRef = useRef(null);
    const messagesEndRef = useRef(null);
    const abortRef = useRef(null);
    const prevPathRef = useRef(location.pathname);

    // Reset khi chuyển trang
    useEffect(() => {
        if (location.pathname !== prevPathRef.current) {
            prevPathRef.current = location.pathname;
            sessionIdRef.current = null;
            setMessages([]);
            setNewMessage('');
            setIsStreaming(false);
            setShowHistory(false);
        }
    }, [location.pathname]);

    // Init tin nhắn chào
    useEffect(() => {
        sessionIdRef.current = null;
        setMessages([{
            id: 'init',
            role: 'MODEL',
            content: config.initialMessage,
            timestamp: formatTime(),
        }]);
    }, [config]);

    // Auto scroll
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    // Load session từ lịch sử
    const loadSession = useCallback(async (session) => {
        setLoadingHistory(true);
        setShowHistory(false);
        setHistoryError(null);
        try {
            const url = ENDPOINTS.CHATBOT.SESSION_MESSAGES(session.id);
            console.log('[ChatBot] loadSession url:', url, 'sessionId:', session.id);
            const res = await fetch(url, { headers: authHeader() });
            const data = await res.json();
            console.log('[ChatBot] loadSession response:', res.status, data);

            if (!res.ok) throw new Error(data?.message ?? `HTTP ${res.status}`);

            const list = Array.isArray(data?.data) ? data.data : [];
            console.log('[ChatBot] messages list:', list.length, list[0]);

            const msgs = list.map((m, i) => ({
                id: m.id ?? `hist-${i}`,
                role: m.role,
                content: m.content ?? '',
                timestamp: formatTime(m.createdAt),
            }));

            sessionIdRef.current = session.id;
            setMessages(msgs.length > 0 ? msgs : [{
                id: 'empty', role: 'MODEL',
                content: 'Cuộc trò chuyện này chưa có tin nhắn.',
                timestamp: formatTime(),
            }]);
        } catch (err) {
            console.error('[ChatBot] loadSession error:', err);
            setHistoryError(err.message || 'Không thể tải tin nhắn');
        } finally {
            setLoadingHistory(false);
        }
    }, []);

    const handleNewChat = useCallback(() => {
        if (sessionIdRef.current) {
            fetch(ENDPOINTS.CHATBOT.ARCHIVE_SESSION(sessionIdRef.current), {
                method: 'POST',
                headers: authHeader(),
            }).catch(() => {});
        }
        sessionIdRef.current = null;
        setShowHistory(false);
        setMessages([{
            id: 'init-' + Date.now(),
            role: 'MODEL',
            content: config.initialMessage,
            timestamp: formatTime(),
        }]);
    }, [config]);

    const handleSendMessage = async () => {
        if (!newMessage.trim() || isStreaming) return;

        const userContent = newMessage.trim();
        setNewMessage('');

        const aiMsgId = 'a-' + Date.now();

        setMessages(prev => [
            ...prev,
            { id: 'u-' + Date.now(), role: 'USER', content: userContent, timestamp: formatTime() },
            { id: aiMsgId, role: 'MODEL', content: '', timestamp: formatTime(), streaming: true },
        ]);
        setIsStreaming(true);

        const controller = new AbortController();
        abortRef.current = controller;

        try {
            const response = await fetch(ENDPOINTS.CHATBOT.CHAT, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'text/event-stream',
                    'Authorization': `Bearer ${getToken()}`,
                },
                body: JSON.stringify({
                    contextType: config.contextType,
                    contextRefId: contextRefId ?? null,
                    sessionId: sessionIdRef.current ?? null,
                    message: userContent,
                }),
                signal: controller.signal,
            });

            if (!response.ok) {
                const errText = await response.text().catch(() => '');
                throw new Error(`HTTP ${response.status}: ${errText}`);
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let aiContent = '';
            let buffer = '';

            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                buffer += decoder.decode(value, { stream: true });

                // Tách thành các SSE event hoàn chỉnh (kết thúc \n\n)
                const parts = buffer.split('\n\n');
                buffer = parts.pop() ?? '';

                for (const part of parts) {
                    // Thu thập TẤT CẢ data: lines trong 1 event, join bằng \n
                    // (Spring WebFlux tách mỗi \n trong text thành 1 data: line riêng)
                    const dataLines = part
                        .split('\n')
                        .filter(l => l.startsWith('data:'))
                        .map(l => l.slice(5));

                    if (dataLines.length === 0) continue;

                    const text = dataLines.join('\n');

                    // Event đầu tiên luôn là [SESSION:xxx]
                    if (text.startsWith('[SESSION:') && text.endsWith(']')) {
                        sessionIdRef.current = text.slice(9, -1);
                        continue;
                    }

                    aiContent += text;
                    setMessages(prev => prev.map(m =>
                        m.id === aiMsgId ? { ...m, content: aiContent } : m
                    ));
                }
            }
        } catch (err) {
            if (err.name !== 'AbortError') {
                console.error('[ChatBot] stream error:', err);
                setMessages(prev => prev.map(m =>
                    m.id === aiMsgId
                        ? { ...m, content: 'Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại.', streaming: false }
                        : m
                ));
            }
        } finally {
            setMessages(prev => prev.map(m =>
                m.id === aiMsgId ? { ...m, streaming: false } : m
            ));
            setIsStreaming(false);
        }
    };

    // ── Render ──────────────────────────────────────────────────────────────

    return (
        <div className="chatbot-container">
            {isOpen && (
                <div className="chatbot-window">
                    {/* Header */}
                    <header className="chatbot-header" style={{ background: config.fabColor }}>
                        <div className="chatbot-header-info">
                            <div className="chatbot-avatar">
                                <span className="material-symbols-outlined">{config.icon}</span>
                            </div>
                            <div>
                                <h1 className="chatbot-title">{config.title}</h1>
                                <div className="chatbot-status">
                                    <span className="status-dot" />
                                    <span className="status-text">{config.subtitle}</span>
                                </div>
                            </div>
                        </div>
                        <div style={{ display: 'flex', gap: 4 }}>
                            <button className="chatbot-close" title="Lịch sử" onClick={() => setShowHistory(p => !p)}>
                                <span className="material-symbols-outlined">history</span>
                            </button>
                            <button className="chatbot-close" title="Chat mới" onClick={handleNewChat}>
                                <span className="material-symbols-outlined">add_comment</span>
                            </button>
                            <button className="chatbot-close" onClick={() => setIsOpen(false)}>
                                <span className="material-symbols-outlined">close</span>
                            </button>
                        </div>
                    </header>

                    {/* History overlay */}
                    {showHistory && (
                        <HistoryPanel
                            contextType={config.contextType}
                            activeSessionId={sessionIdRef.current}
                            onSelectSession={loadSession}
                            onClose={() => setShowHistory(false)}
                        />
                    )}

                    {/* Error toast */}
                    {historyError && (
                        <div className="chatbot-error-toast" onClick={() => setHistoryError(null)}>
                            <span className="material-symbols-outlined">error</span>
                            {historyError}
                            <span className="material-symbols-outlined" style={{ marginLeft: 'auto' }}>close</span>
                        </div>
                    )}

                    {/* Messages */}
                    <main className="chatbot-messages">
                        {loadingHistory ? (
                            <div className="chatbot-history-loading">
                                <div className="typing-dots"><span /><span /><span /></div>
                            </div>
                        ) : (
                            <>
                                <div className="date-marker"><span>Ngày hôm nay</span></div>

                                {messages.map(msg => (
                                    <div
                                        key={msg.id}
                                        className={`message ${msg.role === 'USER' ? 'message-student' : 'message-ai'}`}
                                    >
                                        <div className="message-avatar">
                                            {msg.role === 'USER' ? (
                                                user?.avatarUrl && !imgError ? (
                                                    <img
                                                        src={user.avatarUrl}
                                                        alt="avatar"
                                                        referrerPolicy="no-referrer"
                                                        crossOrigin="anonymous"
                                                        onError={() => setImgError(true)}
                                                    />
                                                ) : (
                                                    <span className="material-symbols-outlined">person</span>
                                                )
                                            ) : (
                                                <span className="material-symbols-outlined">{config.icon}</span>
                                            )}
                                        </div>
                                        <div className="message-content">
                                            <div className="message-bubble">
                                                {msg.role === 'MODEL' ? (
                                                    <MarkdownContent
                                                        content={msg.content}
                                                        streaming={msg.streaming}
                                                    />
                                                ) : (
                                                    <p>{msg.content}</p>
                                                )}
                                            </div>
                                            <span className="message-time">{msg.timestamp}</span>
                                        </div>
                                    </div>
                                ))}

                                {isStreaming && messages.at(-1)?.content === '' && (
                                    <div className="typing-indicator">
                                        <div className="typing-avatar">
                                            <span className="material-symbols-outlined">{config.icon}</span>
                                        </div>
                                        <div className="typing-dots"><span /><span /><span /></div>
                                    </div>
                                )}
                            </>
                        )}
                        <div ref={messagesEndRef} />
                    </main>

                    {/* Footer */}
                    <footer className="chatbot-footer">
                        <div className="input-container">
                            <input
                                type="text"
                                placeholder={config.placeholder}
                                value={newMessage}
                                onChange={e => setNewMessage(e.target.value)}
                                onKeyPress={e => e.key === 'Enter' && handleSendMessage()}
                                className="message-input"
                                disabled={isStreaming || loadingHistory}
                            />
                            <button
                                className="send-button"
                                onClick={handleSendMessage}
                                disabled={isStreaming || !newMessage.trim() || loadingHistory}
                                style={{ background: config.fabColor }}
                            >
                                <span className="material-symbols-outlined">
                                    {isStreaming ? 'stop_circle' : 'send'}
                                </span>
                            </button>
                        </div>
                    </footer>
                </div>
            )}

            {/* FAB */}
            <button
                className="chatbot-fab"
                onClick={() => setIsOpen(p => !p)}
                style={{ background: config.fabColor }}
                title={config.title}
            >
                <span className="material-symbols-outlined">
                    {isOpen ? 'close' : config.fabIcon}
                </span>
            </button>
        </div>
    );
}
