import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  createPendingQuestionImage,
  revokePendingQuestionImage,
  stripPendingQuestionImage,
  uploadPendingQuestionImages,
} from '../../features/assignment/quiz/utils/questionImageUpload';
import { ENDPOINTS } from '../../constants/endpoints';

describe('questionImageUpload utilities', () => {
  beforeEach(() => {
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob:preview'),
      revokeObjectURL: vi.fn(),
    });
  });

  it('creates, revokes, and strips pending image metadata', () => {
    const file = new File(['x'], 'question.png', { type: 'image/png' });
    const pending = createPendingQuestionImage(file);

    expect(pending).toMatchObject({
      imageUrl: 'blob:preview',
      pendingImageFile: file,
      pendingImagePreviewUrl: 'blob:preview',
      pendingImagePath: 'question.png',
    });

    revokePendingQuestionImage(pending);
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:preview');
    expect(stripPendingQuestionImage({ id: 1, ...pending })).toEqual({ id: 1, imageUrl: 'blob:preview' });
  });

  it('uploads only pending image files and returns resolved questions', async () => {
    const file = new File(['x'], 'question.png', { type: 'image/png' });
    const upload = vi.fn().mockResolvedValue('https://cdn/question.png');
    const onUploaded = vi.fn();

    const result = await uploadPendingQuestionImages(
      [
        { id: 1, text: 'No image' },
        { id: 2, pendingImageFile: file, pendingImagePreviewUrl: 'blob:preview', pendingImagePath: 'question.png' },
      ],
      upload,
      'course-1',
      { onUploaded },
    );

    expect(upload).toHaveBeenCalledWith(file, {
      endpoint: ENDPOINTS.UPLOAD.ASSIGNMENT_PRESIGNED,
      folder: 'question-images/course-1',
    });
    expect(onUploaded).toHaveBeenCalledWith('https://cdn/question.png');
    expect(result.uploadedUrls).toEqual(['https://cdn/question.png']);
    expect(result.questions).toEqual([
      { id: 1, text: 'No image' },
      { id: 2, imageUrl: 'https://cdn/question.png' },
    ]);
  });
});
