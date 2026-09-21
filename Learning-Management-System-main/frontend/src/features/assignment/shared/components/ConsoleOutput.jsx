import React from 'react';

function getStatusLabel(status) {
  const labels = {
    AC: 'Passed',
    WA: 'Wrong Answer',
    TLE: 'Time Limit Exceeded',
    MLE: 'Memory Limit Exceeded',
    RE: 'Runtime Error',
    CE: 'Compilation Error',
  };
  return labels[status] || status || 'Unknown';
}

function isPassed(status) {
  return status === 'AC';
}

export default function ConsoleOutput({
  isRunning,
  showResults,
  runResult,
  runError,
  height,
  onClose,
  onResizeStart,
}) {
  const testCaseResults = runResult?.testCaseResults || [];

  return (
    <div className="console-panel" style={{ height: `${height}px` }}>
      <div className="console-resize-handle" onMouseDown={onResizeStart} />
      <div className="console-header">
        <h4>Console Output</h4>
        <button type="button" className="console-close-btn" onClick={onClose} aria-label="Close console">
          <span className="material-symbols-outlined">close</span>
        </button>
      </div>
      <div className="console-output">
        {!isRunning && !showResults && (
          <p style={{ color: '#475569' }}>Nhấn 'Chạy mã' để xem kết quả bên dưới</p>
        )}

        {isRunning && (
          <p style={{ color: '#2563eb' }}>Đang biên dịch và thực thi các bài kiểm tra...</p>
        )}

        {showResults && (
          <>
            {runError && (
              <div className="test-case-row">
                <span className="material-symbols-outlined test-failed">error</span>
                <span className="test-failed">{runError}</span>
              </div>
            )}

            {!runError && runResult?.compileError && (
              <pre className="console-error-output">{runResult.compileError}</pre>
            )}

            {!runError && testCaseResults.map((testCase, index) => {
              const passed = isPassed(testCase.status);
              return (
                <div className="test-case-row" key={`${testCase.testCaseNumber || index}-${testCase.status}`}>
                  <span className={`material-symbols-outlined ${passed ? 'test-passed' : 'test-failed'}`}>
                    {passed ? 'check_circle' : 'cancel'}
                  </span>
                  <div className="test-case-detail">
                    <span className={passed ? 'test-passed' : 'test-failed'}>
                      Test Case {testCase.testCaseNumber || index + 1}: {getStatusLabel(testCase.status)}
                    </span>
                    <span>
                      Time: {testCase.timeMs ?? 0} ms | Memory: {testCase.memoryKb ?? 0} KB
                    </span>
                    {!passed && testCase.outputSnippet && <pre>{testCase.outputSnippet}</pre>}
                  </div>
                </div>
              );
            })}

            {!runError && runResult && (
              <div className="console-summary">
                <p className={runResult.allPassed ? 'test-passed' : 'test-failed'}>
                  {runResult.allPassed
                    ? 'Tất cả test case ví dụ đã được thông qua.'
                    : `Kết quả chạy thử: ${runResult.status || 'FAILED'}`}
                </p>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
