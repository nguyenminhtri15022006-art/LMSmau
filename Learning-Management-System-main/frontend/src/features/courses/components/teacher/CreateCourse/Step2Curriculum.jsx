import React, { useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faGripVertical,
    faTrashAlt,
    faPlus,
    faPlayCircle,
    faFileAlt,
    faChevronUp,
    faChevronDown,
    faFolderPlus,
    faFileMedical,
    faArrowUp,
    faArrowDown,
} from '@fortawesome/free-solid-svg-icons';
import { ChapterSettingsModal, AddSectionModal, AddContentPartModal } from './CurriculumModals';
import { courseApi } from '../../../../../services/course.api';
import { useToast } from '../../../../../components/ui/Toast';
import '../../../styles/teacher/CreateCourse/step2Curriculum.css';

// ── Helpers ──────────────────────────────────────────────────────────────────
const findNode = (items, id) => {
    for (const item of items) {
        if (item.id === id) return item;
        if (item.items) {
            const found = findNode(item.items, id);
            if (found) return found;
        }
    }
    return null;
};

const collectFileUrls = (node) => {
    const urls = [];
    if (node.fileUrl) urls.push(node.fileUrl);
    if (node.items) node.items.forEach(child => urls.push(...collectFileUrls(child)));
    return urls;
};

// Thu thập tất cả id đang có trong cây (để tìm node mới sau khi backend tạo)
const collectAllNodeIds = (items) => {
    const ids = new Set();
    const recurse = (nodes) => {
        nodes.forEach(n => {
            ids.add(n.id);
            if (n.items) recurse(n.items);
        });
    };
    recurse(items);
    return ids;
};

// Flatten cây thành danh sách { nodeId, parentId, order } để gửi lên reorder API
const flattenToOrderList = (nodes, parentId = null) => {
    const result = [];
    nodes.forEach((node, idx) => {
        result.push({ nodeId: node.id, parentId, order: idx });
        if (node.items && node.items.length > 0) {
            result.push(...flattenToOrderList(node.items, node.id));
        }
    });
    return result;
};

// Xây content object theo format backend yêu cầu
const buildLessonContent = (contentType, fileUrl, fileType) => {
    if (contentType === 'video') {
        return { video_url: fileUrl || '', video_source: 'minio' };
    }
    return { file_url: fileUrl || '', file_type: fileType || 'application/octet-stream' };
};

// ── CurriculumNode ────────────────────────────────────────────────────────────
const CurriculumNode = ({ node, level = 0, index, totalItems, onUpdate, onDelete, isCollapsed }) => {
    const [isEditing, setIsEditing] = useState(false);
    const [editTitle, setEditTitle] = useState(node.title);
    const isFolder = node.type === 'chapter' || node.type === 'section';
    const indentStyle = { '--level': level };

    const handleTitleSave = () => {
        if (editTitle.trim() && editTitle !== node.title) {
            onUpdate(node.id, 'EDIT_CHAPTER', { title: editTitle });
        }
        setIsEditing(false);
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') handleTitleSave();
        if (e.key === 'Escape') {
            setEditTitle(node.title);
            setIsEditing(false);
        }
    };

    return (
        <div className={`curriculum-node ${node.type}-node`} style={indentStyle}>
            <div className={`node-header ${node.type}-header`}>
                <div className="drag-and-order">
                    <FontAwesomeIcon icon={faGripVertical} className="drag-handle" />
                    <div className="order-btns">
                        <button disabled={index === 0} onClick={() => onUpdate(node.id, 'MOVE_UP')} className="order-btn">
                            <FontAwesomeIcon icon={faArrowUp} />
                        </button>
                        <button disabled={index === totalItems - 1} onClick={() => onUpdate(node.id, 'MOVE_DOWN')} className="order-btn">
                            <FontAwesomeIcon icon={faArrowDown} />
                        </button>
                    </div>
                </div>

                {node.type === 'section' && <div className="section-bullet" />}

                {node.type === 'lesson' && (
                    <div className={`part-icon-box ${node.contentType}`}>
                        <FontAwesomeIcon icon={node.contentType === 'video' ? faPlayCircle : faFileAlt} />
                    </div>
                )}

                <div className="node-info">
                    {isEditing ? (
                        <input
                            autoFocus
                            className="inline-edit-input"
                            value={editTitle}
                            onChange={(e) => setEditTitle(e.target.value)}
                            onBlur={handleTitleSave}
                            onKeyDown={handleKeyDown}
                        />
                    ) : (
                        <h4 className="node-title" onDoubleClick={() => setIsEditing(true)}>{node.title}</h4>
                    )}
                    {node.type === 'lesson' && (
                        <span className="part-meta">{node.contentType === 'video' ? 'Video' : 'Tài liệu'}</span>
                    )}
                </div>

                <div className="item-actions">
                    {isFolder && (
                        <>
                            <button
                                className="action-text-btn add"
                                onClick={() => onUpdate(node.id, 'OPEN_MODAL', { type: 'section' })}
                            >
                                <FontAwesomeIcon icon={faFolderPlus} />
                                <span>Thêm mục</span>
                            </button>
                            <button
                                className="action-text-btn add"
                                onClick={() => onUpdate(node.id, 'OPEN_MODAL', { type: 'part' })}
                            >
                                <FontAwesomeIcon icon={faFileMedical} />
                                <span>Thêm bài</span>
                            </button>
                        </>
                    )}

                    <button className="action-icon-btn delete" onClick={() => onDelete(node.id)}>
                        <FontAwesomeIcon icon={faTrashAlt} />
                    </button>
                </div>
            </div>

            {isFolder && !isCollapsed && node.items && node.items.length > 0 && (
                <div className="node-children">
                    {node.items.map((child, idx) => (
                        <CurriculumNode
                            key={child.id}
                            node={child}
                            level={level + 1}
                            index={idx}
                            totalItems={node.items.length}
                            onUpdate={onUpdate}
                            onDelete={onDelete}
                            isCollapsed={isCollapsed}
                        />
                    ))}
                </div>
            )}
        </div>
    );
};

// ── Step2Curriculum ───────────────────────────────────────────────────────────
const Step2Curriculum = ({ courseId, data, updateData, onFileUploaded, onFileRemoved, onNodeAdded, onNodeDeleted }) => {
    const toast = useToast();
    const [isCollapsed, setIsCollapsed] = useState(false);
    const [addingChapter, setAddingChapter] = useState(false);
    const [newChapterTitle, setNewChapterTitle] = useState('');
    const [saving, setSaving] = useState(false);
    const [modals, setModals] = useState({
        chapter: { isOpen: false, data: null },
        section: { isOpen: false, data: null, parentId: null },
        part:    { isOpen: false, data: null, parentId: null },
    });

    const openModal = (type, params = {}) => {
        setModals(prev => ({ ...prev, [type]: { isOpen: true, ...params } }));
    };

    const closeModal = (type) => {
        setModals(prev => ({ ...prev, [type]: { isOpen: false, data: null, parentId: null } }));
    };

    // ── Tree mutation helpers ─────────────────────────────────────────────────
    const recursiveUpdate = (items, targetId, action, payload) => {
        if (action === 'ADD_CHAPTER') return [...items, payload];

        if (action === 'MOVE_UP' || action === 'MOVE_DOWN') {
            const idx = items.findIndex(item => item.id === targetId);
            if (idx !== -1) {
                const newItems = [...items];
                const swapIdx = action === 'MOVE_UP' ? idx - 1 : idx + 1;
                if (swapIdx >= 0 && swapIdx < items.length) {
                    [newItems[idx], newItems[swapIdx]] = [newItems[swapIdx], newItems[idx]];
                    return newItems;
                }
            }
        }

        return items.map(item => {
            if (item.id === targetId) {
                if (action === 'EDIT_CHAPTER') return { ...item, ...payload };
                if (action === 'ADD_CHILD') return { ...item, items: [...(item.items || []), payload] };
                if (action === 'DELETE') return null;
            }
            if (item.items) {
                const childIdx = item.items.findIndex(child => child.id === targetId);
                if (childIdx !== -1 && (action === 'MOVE_UP' || action === 'MOVE_DOWN')) {
                    const newChildren = [...item.items];
                    const swapIdx = action === 'MOVE_UP' ? childIdx - 1 : childIdx + 1;
                    if (swapIdx >= 0 && swapIdx < newChildren.length) {
                        [newChildren[childIdx], newChildren[swapIdx]] = [newChildren[swapIdx], newChildren[childIdx]];
                        return { ...item, items: newChildren };
                    }
                }
                const updatedChildren = recursiveUpdate(item.items, targetId, action, payload);
                return { ...item, items: updatedChildren.flat().filter(Boolean) };
            }
            return item;
        }).flat().filter(Boolean);
    };

    const handleUpdate = async (id, action, payload) => {
        if (action === 'OPEN_MODAL') {
            openModal(payload.type, { parentId: id, data: payload.data });
            return;
        }
        const newData = recursiveUpdate(data.chapters, id, action, payload);
        updateData({ chapters: newData });

        if ((action === 'MOVE_UP' || action === 'MOVE_DOWN') && courseId) {
            setSaving(true);
            try {
                const orderList = flattenToOrderList(newData);
                await courseApi.reorderStructureNodes(courseId, orderList);
            } catch (err) {
                toast.error(err?.response?.data?.message || 'Không thể lưu thứ tự. Vui lòng thử lại.');
                updateData({ chapters: data.chapters });
            } finally {
                setSaving(false);
            }
        }

        if (action === 'EDIT_CHAPTER' && courseId && payload?.title) {
            try {
                await courseApi.updateStructureNodeTitle(courseId, id, payload.title);
            } catch (err) {
                toast.error(err?.response?.data?.message || 'Không thể lưu tên. Vui lòng thử lại.');
                updateData({ chapters: data.chapters });
            }
        }
    };

    // ── Add chapter (top-level folder) ───────────────────────────────────────
    const handleAddChapter = async () => {
        const title = newChapterTitle.trim();
        if (!title) return;
        setSaving(true);
        try {
            if (courseId) {
                const existingIds = collectAllNodeIds(data.chapters);
                const response = await courseApi.addStructureNode(courseId, {
                    type: 'folder',
                    title,
                    parentId: null,
                });
                const newBackendNode = response.nodes.find(n => !existingIds.has(n.id));
                if (newBackendNode) onNodeAdded?.(newBackendNode.id);
                const newChapter = {
                    id: newBackendNode?.id ?? `c${Date.now()}`,
                    title,
                    type: 'chapter',
                    items: [],
                };
                updateData({ chapters: [...data.chapters, newChapter] });
            } else {
                const newChapter = { id: `c${Date.now()}`, title, type: 'chapter', items: [] };
                updateData({ chapters: [...data.chapters, newChapter] });
            }
            setNewChapterTitle('');
            setAddingChapter(false);
        } catch (err) {
            toast.error(err?.response?.data?.message || 'Không thể thêm chương mới. Vui lòng thử lại.');
        } finally {
            setSaving(false);
        }
    };

    const handleAddChapterKeyDown = (e) => {
        if (e.key === 'Enter') handleAddChapter();
        if (e.key === 'Escape') {
            setNewChapterTitle('');
            setAddingChapter(false);
        }
    };

    // ── Save modal (add/edit section or lesson) ───────────────────────────────
    const handleSave = async (type, payload) => {
        const modalState = modals[type];
        setSaving(true);
        try {
            if (modalState.data) {
                // ── EDIT existing node ──
                const isObj = typeof payload === 'object' && payload !== null;
                if (onFileRemoved && isObj && payload.replacedUrl) {
                    onFileRemoved(payload.replacedUrl);
                }
                const cleanPayload = isObj
                    ? (({ replacedUrl, fileType, ...rest }) => rest)(payload)
                    : { title: payload };

                // Nếu là lesson và có lessonId → cập nhật content trên backend
                if (courseId && type === 'part' && modalState.data.lessonId && isObj) {
                    const content = buildLessonContent(
                        cleanPayload.contentType || modalState.data.contentType,
                        cleanPayload.fileUrl,
                        payload.fileType,
                    );
                    if (cleanPayload.fileUrl) {
                        await courseApi.saveLessonContent(
                            courseId,
                            modalState.data.lessonId,
                            (cleanPayload.contentType || modalState.data.contentType).toUpperCase(),
                            content,
                        );
                    }
                }
                const newData = recursiveUpdate(data.chapters, modalState.data.id, 'EDIT_CHAPTER', cleanPayload);
                updateData({ chapters: newData });

            } else if (type === 'chapter') {
                // ── ADD chapter via modal (fallback, bình thường dùng inline) ──
                const title = typeof payload === 'string' ? payload : payload.title;
                await handleAddChapter();
                return;

            } else if (type === 'section') {
                // ── ADD section (folder) ──
                const title = typeof payload === 'string' ? payload : payload.title;
                let newNode = { id: `s${Date.now()}`, type: 'section', title, items: [] };

                if (courseId) {
                    const existingIds = collectAllNodeIds(data.chapters);
                    const response = await courseApi.addStructureNode(courseId, {
                        type: 'folder',
                        title,
                        parentId: modalState.parentId,
                    });
                    const backendNode = response.nodes.find(n => !existingIds.has(n.id));
                    if (backendNode) {
                        onNodeAdded?.(backendNode.id);
                        newNode = { id: backendNode.id, type: 'section', title, items: [] };
                    }
                }
                const newData = recursiveUpdate(data.chapters, modalState.parentId, 'ADD_CHILD', newNode);
                updateData({ chapters: newData });

            } else if (type === 'part') {
                // ── ADD lesson ──
                const { replacedUrl, fileType, ...cleanPayload } = typeof payload === 'object' ? payload : { title: payload };
                if (onFileUploaded && cleanPayload.fileUrl) onFileUploaded(cleanPayload.fileUrl);

                let newNode = {
                    id: `l${Date.now()}`,
                    type: 'lesson',
                    title: cleanPayload.title,
                    contentType: cleanPayload.contentType || 'video',
                    fileUrl: cleanPayload.fileUrl,
                };

                if (courseId) {
                    const existingIds = collectAllNodeIds(data.chapters);
                    const response = await courseApi.addStructureNode(courseId, {
                        type: 'lesson',
                        title: cleanPayload.title,
                        parentId: modalState.parentId,
                    });
                    const backendNode = response.nodes.find(n => !existingIds.has(n.id));
                    if (backendNode) {
                        onNodeAdded?.(backendNode.id);
                        newNode = { ...newNode, id: backendNode.id, lessonId: backendNode.lessonId };

                        // Lưu content nếu có file
                        if (cleanPayload.fileUrl && backendNode.lessonId) {
                            const content = buildLessonContent(
                                cleanPayload.contentType || 'video',
                                cleanPayload.fileUrl,
                                fileType,
                            );
                            await courseApi.saveLessonContent(
                                courseId,
                                backendNode.lessonId,
                                (cleanPayload.contentType || 'video').toUpperCase(),
                                content,
                            );
                        }
                    }
                }
                const newData = recursiveUpdate(data.chapters, modalState.parentId, 'ADD_CHILD', newNode);
                updateData({ chapters: newData });
            }

            closeModal(type);
        } catch (err) {
            console.error('handleSave error:', err);
            toast.error(err?.response?.data?.message || 'Không thể lưu. Vui lòng thử lại.');
        } finally {
            setSaving(false);
        }
    };

    // ── Delete node ───────────────────────────────────────────────────────────
    const handleDelete = async (id) => {
        const node = findNode(data.chapters, id);
        if (node) {
            if (onFileRemoved) collectFileUrls(node).forEach(onFileRemoved);
            // Báo lại tất cả ID trong nhánh này đã bị xóa → bỏ khỏi tracking
            if (onNodeDeleted) onNodeDeleted([...collectAllNodeIds([node])]);
        }
        try {
            if (courseId) {
                await courseApi.deleteStructureNode(courseId, id);
            }
            const newData = recursiveUpdate(data.chapters, id, 'DELETE', null);
            updateData({ chapters: newData });
        } catch (err) {
            toast.error(err?.response?.data?.message || 'Không thể xóa. Vui lòng thử lại.');
        }
    };

    // ── Render ────────────────────────────────────────────────────────────────
    return (
        <div className="curriculum-builder">
            <div className="top-actions-bar">
                <button className="btn-collapse" onClick={() => setIsCollapsed(!isCollapsed)} disabled={saving}>
                    <FontAwesomeIcon icon={isCollapsed ? faChevronDown : faChevronUp} />
                    {isCollapsed ? 'Mở rộng tất cả' : 'Thu gọn tất cả'}
                </button>
            </div>

            <div className="chapters-container tree-view">
                {data.chapters.map((chapter, idx) => (
                    <CurriculumNode
                        key={chapter.id}
                        node={chapter}
                        index={idx}
                        totalItems={data.chapters.length}
                        onUpdate={handleUpdate}
                        onDelete={handleDelete}
                        isCollapsed={isCollapsed}
                    />
                ))}

                {addingChapter ? (
                    <div className="add-chapter-inline">
                        <input
                            autoFocus
                            type="text"
                            className="add-chapter-input"
                            placeholder="Nhập tên chương..."
                            value={newChapterTitle}
                            onChange={(e) => setNewChapterTitle(e.target.value)}
                            onKeyDown={handleAddChapterKeyDown}
                            disabled={saving}
                        />
                        <div className="add-chapter-inline-actions">
                            <button className="btn-chapter-confirm" onClick={handleAddChapter} disabled={saving}>
                                {saving ? 'Đang lưu...' : 'Thêm chương'}
                            </button>
                            <button className="btn-chapter-cancel" onClick={() => { setAddingChapter(false); setNewChapterTitle(''); }} disabled={saving}>
                                Hủy
                            </button>
                        </div>
                    </div>
                ) : (
                    <button className="add-chapter-wrapper" onClick={() => setAddingChapter(true)} disabled={saving}>
                        <div className="plus-icon-circle">
                            <FontAwesomeIcon icon={faPlus} />
                        </div>
                        <span>Thêm chương mới</span>
                    </button>
                )}
            </div>

            <ChapterSettingsModal
                isOpen={modals.chapter.isOpen}
                onClose={() => closeModal('chapter')}
                chapterData={modals.chapter.data}
                onSave={(title) => handleSave('chapter', title)}
            />
            <AddSectionModal
                isOpen={modals.section.isOpen}
                onClose={() => closeModal('section')}
                sectionData={modals.section.data}
                onSave={(title) => handleSave('section', title)}
            />
            <AddContentPartModal
                isOpen={modals.part.isOpen}
                onClose={() => closeModal('part')}
                partData={modals.part.data}
                onSave={(partData) => handleSave('part', partData)}
                onFileUploaded={onFileUploaded}
                onFileRemoved={onFileRemoved}
            />
        </div>
    );
};

export default Step2Curriculum;
