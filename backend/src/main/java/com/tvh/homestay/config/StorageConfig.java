package com.tvh.homestay.config;

import com.cloudinary.Cloudinary;
import com.tvh.homestay.storage.CloudinaryImageStorage;
import com.tvh.homestay.storage.ImageStorageService;
import com.tvh.homestay.storage.LocalImageStorage;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chọn nơi lưu ảnh theo CẤU HÌNH, không theo profile.
 *
 * <p>Cùng lý do với {@code MailConfig}: bản {@code demo} đem đi bảo vệ có lúc
 * có Cloudinary và có lúc không. Điều kiện thật là "có {@code CLOUDINARY_URL}
 * hay không", nên đó mới là thứ được kiểm ở đây. Nhánh cục bộ không phải một
 * bản giả để cho qua chuyện — nó là đường chạy thật khi Cloudinary hết quota
 * hoặc máy không có mạng.
 */
@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Bean
    public ImageStorageService imageStorageService(
            @Value("${CLOUDINARY_URL:}") String cloudinaryUrl,
            @Value("${storage.local.dir:uploads}") String localDir) {

        if (cloudinaryUrl.isBlank()) {
            LocalImageStorage local = new LocalImageStorage(Path.of(localDir));
            log.info("Chưa cấu hình CLOUDINARY_URL — ảnh lưu tại {}", local.describe());
            return local;
        }
        log.info("Ảnh được lưu trên Cloudinary");
        return new CloudinaryImageStorage(new Cloudinary(cloudinaryUrl));
    }
}
