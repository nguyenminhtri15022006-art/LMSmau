import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { gatherDeviceInfo, getDeviceInfo, getPublicIP } from '../../services/deviceInfo';

const setUserAgent = (value) => {
  Object.defineProperty(window.navigator, 'userAgent', {
    value,
    configurable: true,
  });
};

describe('deviceInfo service', () => {
  beforeEach(() => {
    vi.spyOn(console, 'warn').mockImplementation(() => {});
    vi.stubGlobal('fetch', vi.fn());
    Object.defineProperty(window, 'innerWidth', { value: 1280, configurable: true });
    Object.defineProperty(window, 'innerHeight', { value: 720, configurable: true });
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('returns public IP from configured service', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve({ ip: '203.0.113.10' }),
    });

    await expect(getPublicIP()).resolves.toBe('203.0.113.10');
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('format=json'),
      expect.objectContaining({ method: 'GET', headers: { Accept: 'application/json' } }),
    );
  });

  it('falls back to unknown when public IP lookup fails', async () => {
    fetch.mockResolvedValueOnce({ ok: false });
    await expect(getPublicIP()).resolves.toBe('unknown');
  });

  it('extracts browser, operating system, and viewport information', () => {
    setUserAgent('Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36');
    expect(getDeviceInfo()).toBe('Chrome 120 | Windows | 1280x720');

    setUserAgent('Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) Version/17.0 Safari/605.1.15');
    expect(getDeviceInfo()).toBe('Safari 17 | macOS | 1280x720');

    setUserAgent('Mozilla/5.0 Firefox/121.0 Linux');
    expect(getDeviceInfo()).toBe('Firefox 121 | Linux | 1280x720');
  });

  it('gathers IP and device info together', async () => {
    setUserAgent('Mozilla/5.0 (Windows NT 10.0) Edg/120.0.0.0');
    fetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve({ ip: '198.51.100.4' }),
    });

    await expect(gatherDeviceInfo()).resolves.toEqual({
      ip: '198.51.100.4',
      deviceInfo: 'Unknown Browser | Windows | 1280x720',
    });
  });
});
