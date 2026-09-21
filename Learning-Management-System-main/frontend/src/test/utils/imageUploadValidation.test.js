import { describe, expect, it } from 'vitest';
import {
  BLOG_THUMBNAIL_MAX_BYTES,
  COURSE_THUMBNAIL_MAX_BYTES,
  IMAGE_UPLOAD_ACCEPT,
  IMAGE_UPLOAD_FORMAT_LABEL,
  validateImageUpload,
} from '../../utils/imageUploadValidation';

const createFileLike = ({ name, type = 'image/png', size = 1024 }) => ({ name, type, size });

describe('imageUploadValidation', () => {
  it('accepts supported image extensions case-insensitively', () => {
    const file = createFileLike({ name: 'thumbnail.JPG', type: 'image/jpeg' });

    expect(validateImageUpload(file, {
      maxBytes: COURSE_THUMBNAIL_MAX_BYTES,
      label: 'Course thumbnail',
    })).toBe('');
  });

  it('rejects files with unsupported extensions', () => {
    const file = createFileLike({ name: 'thumbnail.txt', type: 'image/png' });

    const error = validateImageUpload(file, {
      maxBytes: COURSE_THUMBNAIL_MAX_BYTES,
      label: 'Course thumbnail',
    });

    expect(error).toContain(IMAGE_UPLOAD_FORMAT_LABEL);
  });

  it('rejects files with non-image MIME types even when the extension is supported', () => {
    const file = createFileLike({ name: 'thumbnail.png', type: 'text/plain' });

    const error = validateImageUpload(file, {
      maxBytes: COURSE_THUMBNAIL_MAX_BYTES,
      label: 'Course thumbnail',
    });

    expect(error).toContain(IMAGE_UPLOAD_FORMAT_LABEL);
  });

  it('allows supported extensions when the browser does not provide a MIME type', () => {
    const file = createFileLike({ name: 'thumbnail.webp', type: '' });

    expect(validateImageUpload(file, {
      maxBytes: COURSE_THUMBNAIL_MAX_BYTES,
      label: 'Course thumbnail',
    })).toBe('');
  });

  it('rejects course thumbnails larger than 10MB', () => {
    const file = createFileLike({
      name: 'thumbnail.png',
      size: COURSE_THUMBNAIL_MAX_BYTES + 1,
    });

    const error = validateImageUpload(file, {
      maxBytes: COURSE_THUMBNAIL_MAX_BYTES,
      label: 'Course thumbnail',
    });

    expect(error).toContain('10MB');
  });

  it('rejects blog thumbnails larger than 5MB', () => {
    const file = createFileLike({
      name: 'thumbnail.svg',
      type: 'image/svg+xml',
      size: BLOG_THUMBNAIL_MAX_BYTES + 1,
    });

    const error = validateImageUpload(file, {
      maxBytes: BLOG_THUMBNAIL_MAX_BYTES,
      label: 'Blog thumbnail',
    });

    expect(error).toContain('5MB');
  });

  it('exposes the supported extensions for file inputs', () => {
    expect(IMAGE_UPLOAD_ACCEPT).toBe('.jpg,.jpeg,.png,.gif,.webp,.bmp,.svg');
  });
});
