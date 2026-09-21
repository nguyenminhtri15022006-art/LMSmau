import { PUBLIC_IP_API_URL, PUBLIC_IP_TIMEOUT_MS } from '../configurations/env';

/**
 * Utility functions to gather device and network information
 */

/**
 * Get public IP address from IP geolocation service
 * @returns {Promise<string>} IP address
 */
export async function getPublicIP() {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), PUBLIC_IP_TIMEOUT_MS);

  try {
    const response = await fetch(PUBLIC_IP_API_URL, {
      method: 'GET',
      headers: { 'Accept': 'application/json' },
      signal: controller.signal,
    });

    if (!response.ok) throw new Error('Failed to fetch IP');
    const data = await response.json();
    return data.ip;
  } catch (error) {
    console.warn('Could not fetch public IP:', error);
    return 'unknown';
  } finally {
    clearTimeout(timeoutId);
  }
}

/**
 * Get device information (browser, OS, screen resolution)
 * @returns {string} Device info string
 */
export function getDeviceInfo() {
  try {
    const ua = navigator.userAgent;
    const browser = extractBrowser(ua);
    const os = extractOS(ua);
    const screen = `${window.innerWidth}x${window.innerHeight}`;
    
    return `${browser} | ${os} | ${screen}`;
  } catch (error) {
    console.warn('Could not get device info:', error);
    return 'unknown';
  }
}

/**
 * Extract browser information from user agent
 * @private
 */
function extractBrowser(ua) {
  if (ua.includes('Chrome')) {
    const match = ua.match(/Chrome\/(\d+)/);
    return `Chrome ${match?.[1] || '?'}`;
  }
  if (ua.includes('Firefox')) {
    const match = ua.match(/Firefox\/(\d+)/);
    return `Firefox ${match?.[1] || '?'}`;
  }
  if (ua.includes('Safari') && !ua.includes('Chrome')) {
    const match = ua.match(/Version\/(\d+)/);
    return `Safari ${match?.[1] || '?'}`;
  }
  if (ua.includes('Edge')) {
    const match = ua.match(/Edge(x)?\/(\d+)/);
    return `Edge ${match?.[2] || '?'}`;
  }
  return 'Unknown Browser';
}

/**
 * Extract OS information from user agent
 * @private
 */
function extractOS(ua) {
  if (ua.includes('Windows')) return 'Windows';
  if (ua.includes('Mac')) return 'macOS';
  if (ua.includes('Linux')) return 'Linux';
  if (ua.includes('Android')) return 'Android';
  if (ua.includes('iOS') || ua.includes('iPhone')) return 'iOS';
  return 'Unknown OS';
}

/**
 * Gather all device and IP information
 * @returns {Promise<{ip: string, deviceInfo: string}>}
 */
export async function gatherDeviceInfo() {
  const [ip, deviceInfo] = await Promise.all([
    getPublicIP(),
    Promise.resolve(getDeviceInfo())
  ]);

  return {
    ip,
    deviceInfo
  };
}
