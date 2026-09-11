package com.tvh.homestay.storage;

import com.tvh.homestay.storage.ImageValidator.ValidatedImage;

/**
 * Nơi ảnh đã chuẩn hoá được cất giữ.
 *
 * <p>Hai cài đặt: {@link CloudinaryImageStorage} khi có {@code CLOUDINARY_URL},
 * {@link LocalImageStorage} khi không. Chọn theo CẤU HÌNH, không theo profile —
 * bản demo đem đi bảo vệ có lúc có Cloudinary và có lúc không, và profile không
 * phải là thứ trả lời được câu hỏi đó.
 */
public interface ImageStorageService {

    /** Ảnh đã cất giữ xong. {@code publicId} chỉ có với Cloudinary. */
    record StoredImage(String url, String publicId) {}

    StoredImage store(ValidatedImage image, String folder);

    /**
     * Xoá ảnh.
     *
     * <p>Nhận cả {@code url} lẫn {@code publicId} vì hai nơi lưu trữ định danh
     * ảnh theo hai cách khác nhau, và người gọi không nên phải biết nơi lưu trữ
     * nào đang chạy.
     */
    void delete(String url, String publicId);

    /** Tên hiển thị trong log và báo cáo, để biết bản đang chạy dùng đường nào. */
    String describe();
}
