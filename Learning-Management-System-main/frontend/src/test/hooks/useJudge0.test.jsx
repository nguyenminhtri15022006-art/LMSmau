import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, render, waitFor } from '@testing-library/react';
import React from 'react';
import useJudge0 from '../../hooks/useJudge0';
import apiClient from '../../services/api.client';
import { ENDPOINTS } from '../../constants/endpoints';

vi.mock('../../services/api.client', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}));

function HookHarness({ onReady }) {
  const hook = useJudge0();
  React.useEffect(() => {
    onReady(hook);
  }, [hook, onReady]);
  return <div data-testid="status">{hook.status}</div>;
}

const renderHookHarness = () => {
  const ref = {};
  render(<HookHarness onReady={(hook) => Object.assign(ref, hook)} />);
  return ref;
};

describe('useJudge0', () => {
  beforeEach(() => {
    apiClient.post.mockReset();
    apiClient.get.mockReset();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('submits code and maps accepted result', async () => {
    const ref = renderHookHarness();
    apiClient.post.mockResolvedValueOnce({ data: { token: 'token-1' } });
    apiClient.get.mockResolvedValueOnce({ data: { status: { id: 3, description: 'Accepted' } } });

    await act(async () => {
      await ref.submit({ sourceCode: 'print(1)', languageId: 71, stdin: 'input' });
    });

    expect(apiClient.post).toHaveBeenCalledWith(ENDPOINTS.JUDGE0.SUBMIT, {
      source_code: btoa('print(1)'),
      language_id: 71,
      stdin: btoa('input'),
    });
    expect(apiClient.get).toHaveBeenCalledWith(ENDPOINTS.JUDGE0.RESULT('token-1'));
    await waitFor(() => expect(ref.status).toBe(ref.STATUS.ACCEPTED));
    expect(ref.result.status.description).toBe('Accepted');
  });

  it('polls queued submissions before setting final status', async () => {
    let scheduledPoll;
    const setTimeoutSpy = vi.spyOn(globalThis, 'setTimeout').mockImplementation((callback) => {
      scheduledPoll = callback();
      return 1;
    });
    const ref = renderHookHarness();
    apiClient.post.mockResolvedValueOnce({ data: { token: 'token-2' } });
    apiClient.get
      .mockResolvedValueOnce({ data: { status: { id: 1, description: 'In Queue' } } })
      .mockResolvedValueOnce({ data: { status: { id: 4, description: 'Wrong Answer' } } });

    await act(async () => {
      await ref.submit({ sourceCode: 'print(1)', languageId: 71 });
    });

    expect(setTimeoutSpy).toHaveBeenCalledWith(expect.any(Function), 1500);
    setTimeoutSpy.mockRestore();

    await act(async () => {
      await scheduledPoll;
    });

    expect(ref.status).toBe(ref.STATUS.WRONG_ANSWER);
  });

  it('maps compile errors and runtime-like errors', async () => {
    const ref = renderHookHarness();
    apiClient.post.mockResolvedValue({ data: { token: 'token-3' } });
    apiClient.get.mockResolvedValueOnce({ data: { status: { id: 6, description: 'Compile Error' } } });

    await act(async () => {
      await ref.submit({ sourceCode: 'bad', languageId: 54 });
    });
    expect(ref.status).toBe(ref.STATUS.COMPILE_ERROR);

    apiClient.get.mockResolvedValueOnce({ data: { status: { id: 7, description: 'Runtime Error' } } });
    await act(async () => {
      await ref.submit({ sourceCode: 'bad', languageId: 54 });
    });
    expect(ref.status).toBe(ref.STATUS.RUNTIME_ERROR);
  });

  it('sets error status on API failure and can reset', async () => {
    const ref = renderHookHarness();
    apiClient.post.mockRejectedValueOnce(new Error('Network failed'));

    await act(async () => {
      await ref.submit({ sourceCode: 'print(1)', languageId: 71 });
    });

    expect(ref.status).toBe(ref.STATUS.ERROR);
    expect(ref.logs.at(-1)).toContain('Network failed');

    act(() => ref.reset());
    expect(ref.status).toBe(ref.STATUS.IDLE);
    expect(ref.result).toBeNull();
    expect(ref.logs).toEqual([]);
  });
});
