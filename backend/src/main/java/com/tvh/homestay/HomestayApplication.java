package com.tvh.homestay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật lập lịch ở đây: bộ quét hết hạn giữ chỗ là thứ trả phòng lại cho khách
 * khác. Quên bật thì nó im lặng không chạy, phòng bị giam cho tới khi có người
 * để ý — và test hết hạn vẫn xanh vì nó gọi thẳng phương thức.
 */
@SpringBootApplication
@EnableScheduling
public class HomestayApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomestayApplication.class, args);
    }
}
