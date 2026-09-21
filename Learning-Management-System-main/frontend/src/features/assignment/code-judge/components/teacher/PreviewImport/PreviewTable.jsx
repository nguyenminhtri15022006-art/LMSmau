import React from 'react';
import '../../../styles/teacher/PreviewImport/previewTable.css';

export default function PreviewTable({ testCases }) {
  return (
    <div className="preview-table-container">
      <table className="preview-table">
        <thead>
          <tr>
            <th>#</th>
            <th>Trạng thái</th>
            <th>Dữ liệu vào</th>
            <th>Dữ liệu ra mong muốn</th>
            <th>Loại</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {testCases.map((tc, idx) => (
            <tr key={tc.id}>
              <td>{idx + 1}</td>
              <td>
                <span className={`status-badge ${tc.status}`}>
                  {tc.status === 'valid' ? (
                    <>
                      <span className="material-symbols-outlined">check_circle</span>
                      Hợp lệ
                    </>
                  ) : (
                    <>
                      <span className="material-symbols-outlined">error</span>
                      {tc.error}
                    </>
                  )}
                </span>
              </td>
              <td>
                <input
                  type="text"
                  className={`option-input ${tc.status === 'error' ? 'input-error' : ''}`}
                  defaultValue={tc.input}
                  placeholder={tc.status === 'error' ? 'Nhập dữ liệu vào...' : ''}
                />
              </td>
              <td>
                <input type="text" className="option-input" defaultValue={tc.output} />
              </td>
              <td>
                <select className="type-select" defaultValue={tc.type}>
                  <option>Công khai</option>
                  <option>Ẩn</option>
                </select>
              </td>
              <td>
                <button className="btn-delete-option">
                  <span className="material-symbols-outlined">delete</span>
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <button className="btn-add-option preview-add-button">
        <span className="material-symbols-outlined">add</span>
        Thêm thủ công
      </button>
    </div>
  );
}
