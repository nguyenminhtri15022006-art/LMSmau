import React, { useState } from 'react';
import '../../../../styles/admin/WebContentMgmt/InspireImagesTab.css';

const DEFAULT_IMAGES = [
  {
    id: 'img-1',
    title: 'Bình minh học tập — Ảnh hero trang chủ',
    position: 'Hero Banner',
    alt: 'Học viên học trực tuyến buổi bình minh',
    imageUrl: 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?q=80&w=600&auto=format&fit=crop',
    status: 'Hiển thị',
  },
  {
    id: 'img-2',
    title: 'Sáng tạo & Đổi mới — Section giữa trang',
    position: 'Inspiration Section',
    alt: 'Ý tưởng sáng tạo đổi mới',
    imageUrl: 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?q=80&w=600&auto=format&fit=crop',
    status: 'Hiển thị',
  },
  {
    id: 'img-3',
    title: 'Khởi động sự nghiệp — CTA Section',
    position: 'CTA Banner',
    alt: 'Khởi động sự nghiệp lập trình viên',
    imageUrl: 'https://images.unsplash.com/photo-1522071820081-009f0129c71c?q=80&w=600&auto=format&fit=crop',
    status: 'Ẩn',
  },
  {
    id: 'img-4',
    title: 'Giới thiệu — Ảnh trang About Us',
    position: 'About Us Banner',
    alt: 'Hình ảnh trang giới thiệu EduLearn',
    imageUrl: 'https://res.cloudinary.com/drjgjihjh/image/upload/q_auto/f_auto/v1775215503/Aboutus_pvuxks.jpg',
    status: 'Hiển thị',
  },
];

const INITIAL_IMAGE_FORM = {
  title: '',
  position: 'Hero Banner',
  alt: '',
  imageUrl: '',
  status: 'Hiển thị',
};

const POSITION_OPTIONS = ['Hero Banner', 'Inspiration Section', 'CTA Banner', 'About Us Banner'];

const loadImages = () => {
  const saved = localStorage.getItem('web_content_inspire');
  if (!saved) return DEFAULT_IMAGES;

  try {
    const parsed = JSON.parse(saved);
    const hasAboutUs = parsed.some((img) => img.position === 'About Us Banner');

    if (!hasAboutUs) {
      const aboutUsImage = DEFAULT_IMAGES.find((img) => img.position === 'About Us Banner');
      if (aboutUsImage) {
        const updatedImages = [...parsed, aboutUsImage];
        localStorage.setItem('web_content_inspire', JSON.stringify(updatedImages));
        return updatedImages;
      }
    }

    return parsed;
  } catch (error) {
    console.error(error);
    return DEFAULT_IMAGES;
  }
};

const InspireImagesTab = ({ showToast }) => {
  const [images, setImages] = useState(loadImages);
  const [editingImage, setEditingImage] = useState(null);
  const [imageForm, setImageForm] = useState(INITIAL_IMAGE_FORM);

  const saveImages = (nextImages) => {
    try {
      localStorage.setItem('web_content_inspire', JSON.stringify(nextImages));
      setImages(nextImages);
      return true;
    } catch (error) {
      console.error('Failed to save images to localStorage:', error);
      showToast('Lỗi: Dung lượng lưu trữ đầy hoặc định dạng không được hỗ trợ.', 'danger');
      return false;
    }
  };

  const resetImageForm = () => {
    setEditingImage(null);
    setImageForm(INITIAL_IMAGE_FORM);
  };

  const handleImageFileChange = (event) => {
    const file = event.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onloadend = () => {
      const image = new Image();
      image.onload = () => {
        const MAX_WIDTH = 1200;
        const MAX_HEIGHT = 1200;
        let { width, height } = image;

        if (width > height && width > MAX_WIDTH) {
          height *= MAX_WIDTH / width;
          width = MAX_WIDTH;
        } else if (height > MAX_HEIGHT) {
          width *= MAX_HEIGHT / height;
          height = MAX_HEIGHT;
        }

        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        const context = canvas.getContext('2d');

        if (!context) return;

        context.drawImage(image, 0, 0, width, height);
        const dataUrl = canvas.toDataURL('image/jpeg', 0.7);
        setImageForm((prev) => ({ ...prev, imageUrl: dataUrl }));
      };

      image.src = reader.result;
    };

    reader.readAsDataURL(file);
  };

  const handleSaveImageSubmit = (event) => {
    event.preventDefault();

    if (!imageForm.title || !imageForm.imageUrl) {
      showToast('Vui lòng điền tiêu đề và chọn hình ảnh', 'danger');
      return;
    }

    if (editingImage) {
      const updatedImages = images.map((img) =>
        img.id === editingImage.id ? { ...imageForm, id: img.id } : img,
      );

      if (saveImages(updatedImages)) {
        showToast('Đã cập nhật thông tin hình ảnh thành công');
        resetImageForm();
      }

      return;
    }

    const newImage = {
      ...imageForm,
      id: `img-${Date.now()}`,
    };

    if (saveImages([...images, newImage])) {
      showToast('Đã thêm ảnh truyền cảm hứng mới');
      resetImageForm();
    }
  };

  const handleEditImage = (image) => {
    setEditingImage(image);
    setImageForm({
      title: image.title || '',
      position: image.position || 'Hero Banner',
      alt: image.alt || '',
      imageUrl: image.imageUrl || '',
      status: image.status || 'Hiển thị',
    });
  };

  const handleDeleteImage = (id) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa ảnh này?')) return;

    const filteredImages = images.filter((img) => img.id !== id);
    saveImages(filteredImages);
    showToast('Đã xóa ảnh thành công');
  };

  const handleMoveImage = (index, direction) => {
    const nextIndex = index + direction;
    if (nextIndex < 0 || nextIndex >= images.length) return;

    const reorderedImages = [...images];
    const current = reorderedImages[index];
    reorderedImages[index] = reorderedImages[nextIndex];
    reorderedImages[nextIndex] = current;
    saveImages(reorderedImages);
  };

  return (
    <div className="admin-fade-in">
      <div className="web-content-section-note">
        Ảnh hiển thị luân phiên tại các khu vực banner trang chủ. Sắp xếp thứ tự bằng các nút di chuyển.
      </div>

      <div className="admin-grid-2 web-content-inspire-grid">
        <div className="web-content-inspire-list">
          {images.map((image, index) => (
            <div key={image.id} className="admin-card web-content-inspire-item">
              <div className="web-content-inspire-item-body">
                <div className="web-content-inspire-thumb">
                  <img src={image.imageUrl} alt="" className="web-content-inspire-thumb-image" />
                </div>

                <div className="web-content-inspire-meta">
                  <div className="web-content-inspire-title">{image.title}</div>
                  <div className="web-content-inspire-subtitle">
                    Vị trí: {image.position} · Alt: {image.alt || '--'}
                  </div>
                  <div className="web-content-inspire-status-row">
                    <span
                      className={`admin-status-pill ${
                        image.status === 'Hiển thị' ? 'admin-pill-green' : 'admin-pill-yellow'
                      }`}
                    >
                      ● {image.status}
                    </span>
                  </div>
                </div>

                <div className="web-content-inspire-actions">
                  <button
                    type="button"
                    className="icon-btn"
                    title="Di chuyển lên"
                    disabled={index === 0}
                    onClick={() => handleMoveImage(index, -1)}
                  >
                    <i className="ti ti-arrow-up"></i>
                  </button>
                  <button
                    type="button"
                    className="icon-btn"
                    title="Di chuyển xuống"
                    disabled={index === images.length - 1}
                    onClick={() => handleMoveImage(index, 1)}
                  >
                    <i className="ti ti-arrow-down"></i>
                  </button>
                  <button
                    type="button"
                    className="icon-btn"
                    title="Chỉnh sửa"
                    onClick={() => handleEditImage(image)}
                  >
                    <i className="ti ti-edit"></i>
                  </button>
                  <button
                    type="button"
                    className="icon-btn web-content-danger-action"
                    title="Xóa"
                    onClick={() => handleDeleteImage(image.id)}
                  >
                    <i className="ti ti-trash"></i>
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="admin-card">
          <div className="admin-card-hd">
            <i className={`ti ti-${editingImage ? 'edit' : 'photo-plus'}`}></i>
            {editingImage ? 'Chỉnh sửa ảnh truyền cảm hứng' : 'Thêm ảnh truyền cảm hứng mới'}
          </div>

          <form className="admin-card-body" onSubmit={handleSaveImageSubmit}>
            <div className="admin-field">
              <label>Tiêu đề mô tả ảnh</label>
              <input
                placeholder="VD: Học viên lập trình say mê học bài..."
                value={imageForm.title}
                onChange={(event) => setImageForm({ ...imageForm, title: event.target.value })}
              />
            </div>

            <div className="admin-grid-2">
              <div className="admin-field">
                <label>Vị trí hiển thị</label>
                <select
                  value={imageForm.position}
                  onChange={(event) => setImageForm({ ...imageForm, position: event.target.value })}
                >
                  {POSITION_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
              </div>

              <div className="admin-field">
                <label>Trạng thái</label>
                <select
                  value={imageForm.status}
                  onChange={(event) => setImageForm({ ...imageForm, status: event.target.value })}
                >
                  <option value="Hiển thị">Hiển thị</option>
                  <option value="Ẩn">Ẩn</option>
                </select>
              </div>
            </div>

            <div className="admin-field">
              <label>Alt Text (Hỗ trợ SEO hình ảnh)</label>
              <input
                placeholder="Mô tả ngắn gọn ảnh..."
                value={imageForm.alt}
                onChange={(event) => setImageForm({ ...imageForm, alt: event.target.value })}
              />
            </div>

            <div className="admin-grid-2 web-content-inspire-form-grid">
              <div className="admin-field">
                <label>Hoặc điền trực tiếp Image URL</label>
                <input
                  placeholder="https://..."
                  value={imageForm.imageUrl}
                  onChange={(event) => setImageForm({ ...imageForm, imageUrl: event.target.value })}
                />
              </div>

              <div className="admin-field">
                <label>Tải ảnh từ máy</label>
                <label className="web-content-upload-field">
                  <span>Chọn file ảnh cục bộ</span>
                  <input type="file" accept="image/*" onChange={handleImageFileChange} />
                </label>
              </div>
            </div>

            {imageForm.imageUrl && (
              <div className="web-content-inspire-preview">
                <span className="web-content-inspire-preview-label">Ảnh xem trước:</span>
                <img src={imageForm.imageUrl} alt="preview" className="web-content-inspire-preview-image" />
              </div>
            )}

            <div className="web-content-form-actions">
              <button className="admin-btn admin-btn-primary" type="submit">
                <i className="ti ti-device-floppy"></i>
                Lưu ảnh
              </button>
              {editingImage && (
                <button className="admin-btn" type="button" onClick={resetImageForm}>
                  Hủy bỏ
                </button>
              )}
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default InspireImagesTab;
