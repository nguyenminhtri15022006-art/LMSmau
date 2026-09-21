import { useState, useCallback } from 'react';
import apiClient from '../services/api.client';
import { ENDPOINTS } from '../constants/endpoints';

/**
 * Usage:
 *   const { upload, uploading, progress, error } = useFileUpload();
 *   const url = await upload(file, { folder: 'courses' });
 */
export function useFileUpload() {
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState(null);

  const upload = useCallback(async (file, options = {}) => {
    setUploading(true);
    setProgress(0);
    setError(null);

    try {
      // Step 1: Request Post Policy from backend
      const presignedEndpoint = options.endpoint || ENDPOINTS.UPLOAD.PRESIGNED;
      const response = await apiClient.post(presignedEndpoint, {
        fileName: file.name,
        fileType: file.type,
        folder: options.folder || 'uploads',
      });

      const { uploadUrl, formFields, publicUrl } = response.data.data;

      // Step 2: Build FormData with Post Policy fields
      const formData = new FormData();
      for (const [key, value] of Object.entries(formFields)) {
        formData.append(key, value);
      }
      if (file.type) {
        formData.append('Content-Type', file.type);
      }
      formData.append('file', file);

      // Step 3: POST the form directly to MinIO
      const uploadResponse = await fetch(uploadUrl, {
        method: 'POST',
        body: formData,
      });
      if (!uploadResponse.ok) {
        throw new Error(`Upload failed with status ${uploadResponse.status}`);
      }

      setProgress(100);
      return publicUrl;
    } catch (err) {
      setError(err.message || 'Upload failed');
      throw err;
    } finally {
      setUploading(false);
    }
  }, []);

  return { upload, uploading, progress, error };
}

export default useFileUpload;
