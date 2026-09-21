import React from 'react';
import '../../../styles/teacher/CreateCoding/testCasesCard.css';

export default function TestCasesCard({
                                          courseId,
                                          testCases,
                                          isGenerating,
                                          canBulkImport = true,
                                          onBulkImport,
                                          onAddTestCase,
                                          onAIGenerate,
                                          onUpdateTestCase,
                                          onDeleteTestCase,
                                      }) {
    return (
        <div className="editor-card test-cases-card">
            <div className="test-cases-header">
                <div className="create-card-heading">
                    <span className="material-symbols-outlined">fact_check</span>
                    <div>
                        <h2>Bộ test cases</h2>
                        <p>{testCases.length} bộ kiểm thử sẽ được dùng để chấm bài.</p>
                    </div>
                </div>
                <div className="test-cases-header-actions">
                    <button className="btn-add-option test-cases-action-button" onClick={onAddTestCase}>
                        <span className="material-symbols-outlined">add</span>
                        Thêm bộ test
                    </button>
                    <button
                        className="back-link test-cases-import-button"
                        onClick={() => onBulkImport(courseId)}
                        title={!canBulkImport ? 'Lưu bài tập trước khi nhập hàng loạt' : undefined}
                    >
                        <span className="material-symbols-outlined">upload</span>
                        Nhập hàng loạt
                    </button>
                    <button
                        className="btn-add-option ai-gen-btn test-cases-action-button"
                        onClick={onAIGenerate}
                        disabled={isGenerating}
                    >
                        <span className={`material-symbols-outlined ${isGenerating ? 'spin-anim' : ''}`}>
                          {isGenerating ? 'sync' : 'auto_awesome'}
                        </span>
                        {isGenerating ? 'Đang tạo bộ test với AI...' : 'Tạo bộ test với AI'}
                    </button>
                </div>
            </div>

            <div className="test-cases-table-container">
                <table className="test-cases-table">
                    <thead>
                    <tr>
                        <th>Đầu vào</th>
                        <th>Đầu ra</th>
                        <th className="test-cases-number-heading">Thứ tự</th>
                        <th className="test-cases-number-heading">Trọng số</th>
                        <th>Hiển thị</th>
                        <th className="test-cases-actions-heading">Thao tác</th>
                    </tr>
                    </thead>
                    <tbody>
                    {testCases.map((testCase, index) => (
                        <tr key={testCase.id}>
                            <td>
                  <textarea
                      className="option-input test-case-input"
                      value={testCase.input}
                      rows="3"
                      placeholder="stdin..."
                      onChange={(event) => onUpdateTestCase(testCase.id, 'input', event.target.value)}
                  />
                            </td>
                            <td>
                  <textarea
                      className="option-input test-case-input"
                      value={testCase.output}
                      rows="3"
                      placeholder="expected output..."
                      onChange={(event) => onUpdateTestCase(testCase.id, 'output', event.target.value)}
                  />
                            </td>
                            <td>
                                <input
                                    className="option-input test-case-number-input"
                                    type="number"
                                    min="0"
                                    step="1"
                                    value={testCase.orderIndex ?? index}
                                    onChange={(event) => onUpdateTestCase(testCase.id, 'orderIndex', event.target.value)}
                                />
                            </td>
                            <td>
                                <input
                                    className="option-input test-case-number-input"
                                    type="number"
                                    min="0.01"
                                    step="0.01"
                                    value={testCase.scoreWeight ?? 1}
                                    onChange={(event) => onUpdateTestCase(testCase.id, 'scoreWeight', event.target.value)}
                                />
                            </td>
                            <td>
                                <select
                                    className="type-select"
                                    value={testCase.hidden ? 'hidden' : 'public'}
                                    onChange={(event) => onUpdateTestCase(testCase.id, 'hidden', event.target.value === 'hidden')}
                                >
                                    <option value="public">Công khai</option>
                                    <option value="hidden">Ẩn</option>
                                </select>
                            </td>
                            <td className="test-cases-actions-cell">
                                <button className="btn-delete-option" onClick={() => onDeleteTestCase(testCase.id)}>
                                    <span className="material-symbols-outlined">delete</span>
                                </button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
