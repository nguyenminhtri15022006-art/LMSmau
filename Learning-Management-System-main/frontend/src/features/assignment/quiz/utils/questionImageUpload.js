import { ENDPOINTS } from '../../../../constants/endpoints';

export function createPendingQuestionImage(file) {
  const previewUrl = URL.createObjectURL(file);

  return {
    imageUrl: previewUrl,
    pendingImageFile: file,
    pendingImagePreviewUrl: previewUrl,
    pendingImagePath: file.webkitRelativePath || file.name,
  };
}

export function revokePendingQuestionImage(question) {
  if (question?.pendingImagePreviewUrl) {
    URL.revokeObjectURL(question.pendingImagePreviewUrl);
  }
}

export function stripPendingQuestionImage(question) {
  const {
    pendingImageFile,
    pendingImagePreviewUrl,
    pendingImagePath,
    ...cleanQuestion
  } = question;

  return cleanQuestion;
}

export async function uploadPendingQuestionImages(questions, upload, courseId, { onUploaded } = {}) {
  const uploadedUrls = [];
  const resolvedQuestions = [];

  for (const question of questions) {
    if (!question.pendingImageFile) {
      resolvedQuestions.push(question);
      continue;
    }

    const imageUrl = await upload(question.pendingImageFile, {
      endpoint: ENDPOINTS.UPLOAD.ASSIGNMENT_PRESIGNED,
      folder: courseId ? `question-images/${courseId}` : 'question-images',
    });

    uploadedUrls.push(imageUrl);
    onUploaded?.(imageUrl);
    resolvedQuestions.push(stripPendingQuestionImage({ ...question, imageUrl }));
  }

  return { questions: resolvedQuestions, uploadedUrls };
}
