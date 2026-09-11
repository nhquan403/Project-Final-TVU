package com.tvh.homestay.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tvh.homestay.admin.exception.AdminExceptions.ImageStorageFailed;
import com.tvh.homestay.storage.ImageValidator.ValidatedImage;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Lưu ảnh lên Cloudinary. Giữ lại {@code public_id} để về sau xoá được. */
public class CloudinaryImageStorage implements ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryImageStorage.class);

    private final Cloudinary cloudinary;

    public CloudinaryImageStorage(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public StoredImage store(ValidatedImage image, String folder) {
        try {
            // Nội dung gửi lên là bản ĐÃ chuẩn hoá của ImageValidator, không
            // phải tệp gốc của client. Cloudinary cũng tự kiểm, nhưng đó là lớp
            // thứ hai — lớp thứ nhất phải là của chính hệ thống này.
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    image.content(),
                    ObjectUtils.asMap(
                            "folder", folder == null || folder.isBlank() ? "homestay" : folder,
                            "public_id", UUID.randomUUID().toString(),
                            "resource_type", "image",
                            "overwrite", false));
            String url = String.valueOf(result.getOrDefault("secure_url", result.get("url")));
            String publicId = String.valueOf(result.get("public_id"));
            return new StoredImage(url, publicId);
        } catch (Exception e) {
            throw new ImageStorageFailed("Không tải được ảnh lên Cloudinary: " + e.getMessage());
        }
    }

    @Override
    public void delete(String url, String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.warn("Không xoá được ảnh {} trên Cloudinary: {}", publicId, e.getMessage());
        }
    }

    @Override
    public String describe() {
        return "CloudinaryImageStorage";
    }
}
