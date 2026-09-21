import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import {
  AddContentPartModal,
  validateLessonFileExtension,
} from '../../../features/courses/components/teacher/CreateCourse/CurriculumModals';
import { courseApi } from '../../../services/course.api';

vi.mock('@fortawesome/react-fontawesome', () => ({
  FontAwesomeIcon: ({ icon }) => React.createElement('span', { 'data-icon': icon?.iconName || 'mock' }),
}));

vi.mock('@fortawesome/free-solid-svg-icons', () => ({
  faTimes: { iconName: 'times' },
  faLayerGroup: { iconName: 'layer-group' },
  faFolderOpen: { iconName: 'folder-open' },
  faVideo: { iconName: 'video' },
  faBook: { iconName: 'book' },
  faFileAlt: { iconName: 'file-alt' },
  faCheckCircle: { iconName: 'check-circle' },
  faTimesCircle: { iconName: 'times-circle' },
  faSpinner: { iconName: 'spinner' },
}));

vi.mock('../../../services/course.api', () => ({
  courseApi: {
    getPresignedUrl: vi.fn(),
  },
}));

class FakeXMLHttpRequest {
  static instances = [];

  constructor() {
    this.upload = {};
    this.status = 200;
    FakeXMLHttpRequest.instances.push(this);
  }

  open = vi.fn();

  setRequestHeader = vi.fn();

  send = vi.fn(() => {
    setTimeout(() => {
      this.onload?.();
    }, 0);
  });

  abort = vi.fn(() => {
    this.onabort?.();
  });
}

const renderContentModal = (props = {}) => render(
  <AddContentPartModal
    isOpen
    onClose={vi.fn()}
    onSave={vi.fn()}
    onFileUploaded={vi.fn()}
    onFileRemoved={vi.fn()}
    {...props}
  />,
);

const uploadCurrentFile = async (container, file, publicUrl) => {
  courseApi.getPresignedUrl.mockResolvedValueOnce({
    presignedUrl: `https://minio/upload/${file.name}`,
    publicUrl,
  });

  const input = container.querySelector('input[type="file"]');
  fireEvent.change(input, { target: { files: [file] } });

  await waitFor(() => {
    expect(courseApi.getPresignedUrl).toHaveBeenCalledWith(file.name, file.type, expect.any(String));
  });
};

describe('CurriculumModals lesson file validation', () => {
  beforeEach(() => {
    courseApi.getPresignedUrl.mockReset();
    FakeXMLHttpRequest.instances = [];
    vi.stubGlobal('XMLHttpRequest', FakeXMLHttpRequest);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('rejects invalid video extension before requesting upload URL', async () => {
    const { container } = renderContentModal();
    const input = container.querySelector('input[type="file"]');
    const file = new File(['not a video'], 'lesson.txt', { type: 'text/plain' });

    fireEvent.change(input, { target: { files: [file] } });

    expect(await screen.findByText('File video chỉ hỗ trợ MP4, MOV, WEBM.')).toBeInTheDocument();
    expect(courseApi.getPresignedUrl).not.toHaveBeenCalled();
    expect(FakeXMLHttpRequest.instances).toHaveLength(0);
  });

  it('accepts uppercase video extension and starts upload', async () => {
    const onFileUploaded = vi.fn();
    courseApi.getPresignedUrl.mockResolvedValueOnce({
      presignedUrl: 'https://minio/upload',
      publicUrl: 'https://cdn/lesson.MP4',
    });
    const { container } = renderContentModal({ onFileUploaded });
    const input = container.querySelector('input[type="file"]');
    const file = new File(['video'], 'lesson.MP4', { type: 'video/mp4' });

    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(courseApi.getPresignedUrl).toHaveBeenCalledWith('lesson.MP4', 'video/mp4', 'lessons/videos');
    });
    await waitFor(() => expect(onFileUploaded).toHaveBeenCalledWith('https://cdn/lesson.MP4'));
    expect(FakeXMLHttpRequest.instances[0].open).toHaveBeenCalledWith('PUT', 'https://minio/upload');
  });

  it('rejects invalid document extension before requesting upload URL', async () => {
    const { container } = renderContentModal();

    fireEvent.click(screen.getByRole('button', { name: /tài liệu/i }));
    const input = container.querySelector('input[type="file"]');
    const file = new File(['image'], 'diagram.png', { type: 'image/png' });

    fireEvent.change(input, { target: { files: [file] } });

    expect(await screen.findByText('Tài liệu chỉ hỗ trợ PDF, DOC, DOCX, PPT, PPTX, XLSX.')).toBeInTheDocument();
    expect(courseApi.getPresignedUrl).not.toHaveBeenCalled();
  });

  it('clears validation error when switching content type', async () => {
    const { container } = renderContentModal();
    const input = container.querySelector('input[type="file"]');
    const file = new File(['not a video'], 'lesson.txt', { type: 'text/plain' });

    fireEvent.change(input, { target: { files: [file] } });
    expect(await screen.findByText('File video chỉ hỗ trợ MP4, MOV, WEBM.')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /tài liệu/i }));

    expect(screen.queryByText('File video chỉ hỗ trợ MP4, MOV, WEBM.')).not.toBeInTheDocument();
  });

  it('validates document extensions case-insensitively', () => {
    const file = new File(['pdf'], 'slides.PDF', { type: 'application/pdf' });

    expect(validateLessonFileExtension(file, 'document')).toBeNull();
  });

  it('keeps uploaded video when switching to document and back', async () => {
    const { container } = renderContentModal();
    const file = new File(['video'], 'intro.mp4', { type: 'video/mp4' });

    await uploadCurrentFile(container, file, 'https://cdn/intro.mp4');
    expect(await screen.findByText('intro.mp4')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /tài liệu/i }));
    expect(screen.queryByText('intro.mp4')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /^video$/i }));
    expect(await screen.findByText('intro.mp4')).toBeInTheDocument();
  });

  it('saves the active document file and cleans up inactive video upload', async () => {
    const onSave = vi.fn();
    const onFileRemoved = vi.fn();
    const { container } = renderContentModal({ onSave, onFileRemoved });

    fireEvent.change(screen.getByPlaceholderText('VD: Nguyên tắc thiết kế nâng cao'), {
      target: { value: 'Bài học mới' },
    });

    await uploadCurrentFile(
      container,
      new File(['video'], 'intro.mp4', { type: 'video/mp4' }),
      'https://cdn/intro.mp4',
    );

    fireEvent.click(screen.getByRole('button', { name: /tài liệu/i }));
    await uploadCurrentFile(
      container,
      new File(['doc'], 'slides.pdf', { type: 'application/pdf' }),
      'https://cdn/slides.pdf',
    );

    fireEvent.click(screen.getByRole('button', { name: 'Thêm bài học' }));

    expect(onSave).toHaveBeenCalledWith({
      title: 'Bài học mới',
      contentType: 'document',
      fileUrl: 'https://cdn/slides.pdf',
      fileType: 'application/pdf',
      replacedUrl: null,
    });
    expect(onFileRemoved).toHaveBeenCalledWith('https://cdn/intro.mp4');
    expect(onFileRemoved).not.toHaveBeenCalledWith('https://cdn/slides.pdf');
  });

  it('marks original video as replaced when saving a document over it', async () => {
    const onSave = vi.fn();
    const { container } = renderContentModal({
      onSave,
      partData: {
        title: 'Bài học cũ',
        contentType: 'video',
        fileUrl: 'https://cdn/original.mp4',
      },
    });

    fireEvent.click(screen.getByRole('button', { name: /tài liệu/i }));
    await uploadCurrentFile(
      container,
      new File(['doc'], 'slides.pdf', { type: 'application/pdf' }),
      'https://cdn/slides.pdf',
    );

    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }));

    expect(onSave).toHaveBeenCalledWith({
      title: 'Bài học cũ',
      contentType: 'document',
      fileUrl: 'https://cdn/slides.pdf',
      fileType: 'application/pdf',
      replacedUrl: 'https://cdn/original.mp4',
    });
  });

  it('does not replace original video when switching away and back before saving', () => {
    const onSave = vi.fn();
    renderContentModal({
      onSave,
      partData: {
        title: 'Bài học cũ',
        contentType: 'video',
        fileUrl: 'https://cdn/original.mp4',
      },
    });

    fireEvent.click(screen.getByRole('button', { name: /tài liệu/i }));
    fireEvent.click(screen.getByRole('button', { name: /^video$/i }));
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }));

    expect(onSave).toHaveBeenCalledWith({
      title: 'Bài học cũ',
      contentType: 'video',
      fileUrl: 'https://cdn/original.mp4',
      fileType: null,
      replacedUrl: null,
    });
  });
});
