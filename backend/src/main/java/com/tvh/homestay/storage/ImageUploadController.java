package com.tvh.homestay.storage;

import com.tvh.homestay.admin.dto.AdminDtos.UploadedImage;
import com.tvh.homestay.storage.ImageStorageService.StoredImage;
import com.tvh.homestay.storage.ImageValidator.ValidatedImage;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Nhận ảnh tải lên từ khu quản trị.
 *
 * <p>Hai bước, không gộp: {@link ImageValidator} kiểm và ghi lại ảnh, rồi
 * {@link ImageStorageService} mới cất giữ. Nơi lưu trữ không bao giờ nhìn thấy
 * byte gốc của client.
 */
@RestController
public class ImageUploadController {

    private final ImageValidator validator;
    private final ImageStorageService storage;

    public ImageUploadController(ImageValidator validator, ImageStorageService storage) {
        this.validator = validator;
        this.storage = storage;
    }

    @PostMapping("/api/admin/images")
    public UploadedImage upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "folder", required = false) String folder) {

        ValidatedImage validated = validator.validate(file);
        StoredImage stored = storage.store(validated, folder);
        return new UploadedImage(
                stored.url(),
                stored.publicId(),
                validated.contentType(),
                validated.content().length);
    }
}
