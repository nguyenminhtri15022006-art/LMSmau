export const IMAGE_UPLOAD_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg'];
export const IMAGE_UPLOAD_ACCEPT = IMAGE_UPLOAD_EXTENSIONS.map(ext => `.${ext}`).join(',');

export const COURSE_THUMBNAIL_MAX_BYTES = 10 * 1024 * 1024;
export const BLOG_THUMBNAIL_MAX_BYTES = 5 * 1024 * 1024;

export const IMAGE_UPLOAD_FORMAT_LABEL = 'JPG, JPEG, PNG, GIF, WEBP, BMP, SVG';

const getFileExtension = (fileName = '') => {
    const parts = fileName.split('.');
    return parts.length > 1 ? parts.pop().toLowerCase() : '';
};

export const formatUploadSize = (bytes) => {
    const sizeInMb = bytes / 1024 / 1024;
    return `${Number.isInteger(sizeInMb) ? sizeInMb : sizeInMb.toFixed(1)}MB`;
};

export const validateImageUpload = (file, { maxBytes, label = 'Ảnh' }) => {
    if (!file) return '';

    const extension = getFileExtension(file.name);
    const hasValidExtension = IMAGE_UPLOAD_EXTENSIONS.includes(extension);
    const hasValidMimeType = !file.type || file.type.startsWith('image/');

    if (!hasValidExtension || !hasValidMimeType) {
        return `${label} chỉ hỗ trợ ${IMAGE_UPLOAD_FORMAT_LABEL}.`;
    }

    if (file.size > maxBytes) {
        return `${label} không được vượt quá ${formatUploadSize(maxBytes)}.`;
    }

    return '';
};
