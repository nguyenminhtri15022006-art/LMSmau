import { useState, useCallback } from 'react';
import apiClient from '../services/api.client';
import { ENDPOINTS } from '../constants/endpoints';

// =============================================
// useJudge0 – Code submission to Judge0
// =============================================

const STATUS = {
  IDLE: 'idle',
  SUBMITTING: 'submitting',
  IN_QUEUE: 'in_queue',
  PROCESSING: 'processing',
  AC: 'AC',
  ACCEPTED: 'AC',
  WA: 'WA',
  WRONG_ANSWER: 'WA',
  TLE: 'TLE',
  TIME_LIMIT: 'TLE',
  CE: 'CE',
  COMPILE_ERROR: 'CE',
  RE: 'RE',
  RUNTIME_ERROR: 'RE',
  ERROR: 'error',
};

/**
 * Usage:
 *   const { submit, status, result, isRunning, reset } = useJudge0();
 *   await submit({ sourceCode, languageId, stdin });
 */
export function useJudge0() {
  const [status, setStatus] = useState(STATUS.IDLE);
  const [result, setResult] = useState(null);
  const [logs, setLogs] = useState([]);

  const isRunning = [STATUS.SUBMITTING, STATUS.IN_QUEUE, STATUS.PROCESSING].includes(status);

  const submit = useCallback(async ({ sourceCode, languageId, stdin = '' }) => {
    setStatus(STATUS.SUBMITTING);
    setResult(null);
    setLogs([]);

    try {
      // Submit code to Judge0 via backend proxy
      const { data: submission } = await apiClient.post(ENDPOINTS.JUDGE0.SUBMIT, {
        source_code: btoa(sourceCode), // base64 encode
        language_id: languageId,
        stdin: btoa(stdin),
      });

      const token = submission.token;
      setStatus(STATUS.IN_QUEUE);
      setLogs((prev) => [...prev, `[${new Date().toLocaleTimeString()}] Submitted. Token: ${token}`]);

      // Poll for result
      const poll = async () => {
        const { data } = await apiClient.get(ENDPOINTS.JUDGE0.RESULT(token));
        const statusId = data.status?.id;

        if (statusId <= 2) {
          // In queue or processing
          setStatus(statusId === 1 ? STATUS.IN_QUEUE : STATUS.PROCESSING);
          setTimeout(poll, 1500);
        } else {
          setResult(data);
          if (statusId === 3) setStatus(STATUS.ACCEPTED);
          else if (statusId === 4) setStatus(STATUS.WRONG_ANSWER);
          else if (statusId === 5) setStatus(STATUS.TIME_LIMIT);
          else if (statusId === 6) setStatus(STATUS.COMPILE_ERROR);
          else setStatus(STATUS.RUNTIME_ERROR);

          setLogs((prev) => [
            ...prev,
            `[${new Date().toLocaleTimeString()}] Result: ${data.status?.description}`,
          ]);
        }
      };

      await poll();
    } catch (err) {
      setStatus(STATUS.ERROR);
      setLogs((prev) => [...prev, `[ERROR] ${err.message}`]);
    }
  }, []);

  const reset = useCallback(() => {
    setStatus(STATUS.IDLE);
    setResult(null);
    setLogs([]);
  }, []);

  return { submit, status, result, logs, isRunning, reset, STATUS };
}

export default useJudge0;
