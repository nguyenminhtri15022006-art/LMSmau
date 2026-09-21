import React from 'react';
import ReactMarkdown from 'react-markdown';
import { formatLimit } from '../../../../../../utils/codeChallengeFormatters';

const TABS = [
  { key: 'problem', label: 'Bài toán' },
  { key: 'constraints', label: 'Ràng buộc' },
  { key: 'examples', label: 'Ví dụ' },
  { key: 'submissions', label: 'Lịch sử nộp bài' },
];

function getStatusClass(status) {
  return String(status || 'unknown').toLowerCase().replace(/[^a-z0-9]+/g, '-');
}

function trimBlankLines(lines) {
  const nextLines = [...lines];
  while (nextLines.length && !nextLines[0].trim()) nextLines.shift();
  while (nextLines.length && !nextLines[nextLines.length - 1].trim()) nextLines.pop();
  return nextLines;
}

function isSeparatorLine(line) {
  return /^[ \t]*[-_*]{3,}[ \t]*$/.test(line);
}

function isFenceLine(line) {
  return /^[ \t]*```[a-zA-Z0-9_-]*[ \t]*$/.test(line);
}

function isBareFenceLine(line) {
  return /^[ \t]*```[ \t]*$/.test(line);
}

function isMarkdownWrapperFence(line) {
  return /^[ \t]*```(?:markdown|md)?[ \t]*$/i.test(line);
}

function hasMarkdownHeading(lines) {
  return lines.some((line) => /^[ \t]*#{1,6}\s+/.test(line));
}

function removeOuterMarkdownFence(lines) {
  const nextLines = trimBlankLines(lines);
  if (!nextLines.length || !isMarkdownWrapperFence(nextLines[0])) {
    return nextLines;
  }

  const contentLines = trimBlankLines(nextLines.slice(1));
  if (!hasMarkdownHeading(contentLines)) {
    return nextLines;
  }

  if (contentLines.length && isBareFenceLine(contentLines[contentLines.length - 1])) {
    const fenceCount = contentLines.filter(isFenceLine).length;
    if (fenceCount % 2 === 1) {
      contentLines.pop();
    }
  }

  return trimBlankLines(contentLines);
}

function closeUnbalancedCodeFence(lines) {
  const fenceCount = lines.filter(isFenceLine).length;
  if (fenceCount % 2 === 1) {
    return [...lines, '```'];
  }
  return lines;
}

function normalizeProblemMarkdown(value) {
  const rawMarkdown = String(value || '').replace(/\r\n?/g, '\n');
  let lines = trimBlankLines(rawMarkdown.split('\n'));

  while (lines.length && isSeparatorLine(lines[0])) {
    lines.shift();
  }
  lines = trimBlankLines(lines);
  lines = removeOuterMarkdownFence(lines);

  const indents = lines
    .filter((line) => line.trim() && !isSeparatorLine(line))
    .map((line) => line.match(/^[ \t]*/)?.[0].length ?? 0);
  const minIndent = indents.length ? Math.min(...indents) : 0;

  if (minIndent > 0) {
    lines = lines.map((line) => (line.trim() ? line.slice(minIndent) : ''));
  }

  lines = removeOuterMarkdownFence(trimBlankLines(lines));
  return closeUnbalancedCodeFence(trimBlankLines(lines)).join('\n');
}

export default function ProblemPanel({
  activeTab,
  setActiveTab,
  challenge,
  submissionHistory,
  onSubmissionSelect,
  loadingSubmissionId,
}) {
  const examples = challenge.examples || [];
  const allowedLangs = challenge.allowedLangs || [];
  const historyRows = submissionHistory?.rows || [];
  const problemMarkdown = normalizeProblemMarkdown(
    challenge.problemStatement || challenge.description || 'Chưa có mô tả bài toán.',
  );

  return (
    <div className="problem-panel">
      <div className="panel-tabs">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            className={`panel-tab ${activeTab === tab.key ? 'active' : ''}`}
            onClick={() => setActiveTab(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="panel-content" key={activeTab}>
        {activeTab === 'problem' && (
          <div className="problem-statement">
            <div className="problem-markdown">
              <ReactMarkdown>
                {problemMarkdown}
              </ReactMarkdown>
            </div>
          </div>
        )}

        {activeTab === 'constraints' && (
          <div className="problem-statement">
            <h3>Ràng buộc</h3>
            <div className="constraint-grid">
              <div className="constraint-metric">
                <span>Time limit</span>
                <strong>{formatLimit(challenge.timeLimitMs, 'ms')}</strong>
              </div>
              <div className="constraint-metric">
                <span>Memory limit</span>
                <strong>{formatLimit(challenge.memoryLimitMb, 'MB')}</strong>
              </div>
              <div className="constraint-metric constraint-metric-wide">
                <span>Allowed languages</span>
                <strong>{allowedLangs.length ? allowedLangs.join(', ') : 'Chưa thiết lập'}</strong>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'examples' && (
          <div className="problem-statement">
            {examples.length ? examples.map((ex, i) => (
              <div key={`${ex.input}-${i}`} className="example-item">
                <h3>Ví dụ {i + 1}</h3>
                <div className="example-box">
                  <div className="example-block">
                    <strong>Input:</strong>
                    <pre>{String(ex.input ?? '')}</pre>
                  </div>
                  <div className="example-block">
                    <strong>Output:</strong>
                    <pre>{String(ex.output ?? '')}</pre>
                  </div>
                  {ex.explanation && <p className="example-explanation">{ex.explanation}</p>}
                </div>
              </div>
            )) : (
              <div className="panel-empty-state">
                <span className="material-symbols-outlined">dataset</span>
                <p>Chưa có ví dụ công khai cho bài tập này.</p>
              </div>
            )}
          </div>
        )}

        {activeTab === 'submissions' && (
          <div className="submission-history">
            {submissionHistory?.loading && (
              <div className="panel-empty-state">
                <span className="material-symbols-outlined">hourglass_top</span>
                <p>Đang tải lịch sử nộp bài...</p>
              </div>
            )}

            {!submissionHistory?.loading && submissionHistory?.error && (
              <div className="panel-empty-state panel-error-state">
                <span className="material-symbols-outlined">error</span>
                <p>{submissionHistory.error}</p>
              </div>
            )}

            {!submissionHistory?.loading && !submissionHistory?.error && !historyRows.length && (
              <div className="panel-empty-state">
                <span className="material-symbols-outlined">history</span>
                <p>Chưa có bài nộp nào cho bài tập này.</p>
              </div>
            )}

            {!submissionHistory?.loading && !submissionHistory?.error && historyRows.length > 0 && (
              <div className="submission-table-wrap">
                <table className="submission-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Language</th>
                      <th>Status</th>
                      <th>Score</th>
                      <th>Runtime</th>
                      <th>Submitted</th>
                    </tr>
                  </thead>
                  <tbody>
                    {historyRows.map((row) => (
                      <tr key={row.key}>
                        <td className="submission-id">
                          <button
                            type="button"
                            className="submission-id-button"
                            onClick={() => onSubmissionSelect?.(row.submissionId)}
                            disabled={!row.submissionId || loadingSubmissionId === row.submissionId}
                            title="Hiển thị mã nguồn bài nộp này"
                          >
                            {loadingSubmissionId === row.submissionId ? 'Đang tải...' : row.id}
                          </button>
                        </td>
                        <td>{row.language}</td>
                        <td>
                          <span className={`submission-status ${getStatusClass(row.statusRaw)}`}>
                            {row.status}
                          </span>
                        </td>
                        <td>{row.score}</td>
                        <td>{row.runtime}</td>
                        <td>{row.submittedAt}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
