import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, render, waitFor } from '@testing-library/react';
import React from 'react';
import useFileUpload from '../../hooks/useFileUpload';
import apiClient from '../../services/api.client';
import { ENDPOINTS } from '../../constants/endpoints';

vi.mock('../../services/api.client', () => ({
  default: {
    post: vi.fn(),
  },
}));

function HookHarness({ onReady }) {
  const hook = useFileUpload();
  React.useEffect(() => {
    onReady(hook);
  }, [hook, onReady]);
  return <div data-testid="state">{hook.uploading ? 'uploading' : `${hook.progress}|${hook.error || ''}`}</div>;
}

const renderHookHarness = () => {
  const ref = {};
  render(<HookHarness onReady={(hook) => Object.assign(ref, hook)} />);
  return ref;
};

describe('useFileUpload', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn());
    apiClient.post.mockReset();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('uploads file using presigned post fields and returns public URL', async () => {
    const ref = renderHookHarness();
    const file = new File(['hello'], 'lesson.txt', { type: 'text/plain' });
    apiClient.post.mockResolvedValueOnce({
      data: {
        data: {
          uploadUrl: 'https://minio/upload',
          formFields: { key: 'uploads/lesson.txt', policy: 'policy' },
          publicUrl: 'https://cdn/lesson.txt',
        },
      },
    });
    fetch.mockResolvedValueOnce({ ok: true });

    await expect(act(async () => ref.upload(file, { folder: 'courses' }))).resolves.toBe('https://cdn/lesson.txt');

    expect(apiClient.post).toHaveBeenCalledWith(ENDPOINTS.UPLOAD.PRESIGNED, {
      fileName: 'lesson.txt',
      fileType: 'text/plain',
      folder: 'courses',
    });
    const formData = fetch.mock.calls[0][1].body;
    expect(formData.get('key')).toBe('uploads/lesson.txt');
    expect(formData.get('Content-Type')).toBe('text/plain');
    expect(formData.get('file')).toBe(file);
    await waitFor(() => expect(ref.progress).toBe(100));
    expect(ref.uploading).toBe(false);
  });

  it('uses custom endpoint and default upload folder', async () => {
    const ref = renderHookHarness();
    const file = new File(['hello'], 'image.png', { type: 'image/png' });
    apiClient.post.mockResolvedValueOnce({
      data: { data: { uploadUrl: 'https://minio/upload', formFields: {}, publicUrl: 'https://cdn/image.png' } },
    });
    fetch.mockResolvedValueOnce({ ok: true });

    await act(async () => {
      await ref.upload(file, { endpoint: '/custom-presigned' });
    });

    expect(apiClient.post).toHaveBeenCalledWith('/custom-presigned', {
      fileName: 'image.png',
      fileType: 'image/png',
      folder: 'uploads',
    });
  });

  it('sets error state when direct storage upload fails', async () => {
    const ref = renderHookHarness();
    const file = new File(['hello'], 'lesson.txt', { type: 'text/plain' });
    apiClient.post.mockResolvedValueOnce({
      data: { data: { uploadUrl: 'https://minio/upload', formFields: {}, publicUrl: 'https://cdn/lesson.txt' } },
    });
    fetch.mockResolvedValueOnce({ ok: false, status: 500 });

    let caughtError;
    await act(async () => {
      try {
        await ref.upload(file);
      } catch (err) {
        caughtError = err;
      }
    });

    expect(caughtError.message).toBe('Upload failed with status 500');
    await waitFor(() => expect(ref.error).toBe('Upload failed with status 500'));
    expect(ref.uploading).toBe(false);
  });
});
