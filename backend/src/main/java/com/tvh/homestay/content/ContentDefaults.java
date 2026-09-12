package com.tvh.homestay.content;

import com.tvh.homestay.content.dto.ContentDtos.SectionView;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Nội dung mặc định cho từng khối của trang chủ.
 *
 * <p>Trang chủ phải đứng vững khi CMS còn TRỐNG. Không có lớp này, một bản
 * triển khai mới — hoặc bản demo trước khi ai kịp nhập nội dung — sẽ hiện ra
 * một trang có hero trắng, phần giới thiệu trắng và phần liên hệ trắng, và
 * người xem không phân biệt được "chưa nhập nội dung" với "trang hỏng".
 *
 * <p>Mặc định nằm ở BACKEND chứ không ở frontend: hai nơi cùng giữ một bản
 * mặc định là hai bản sẽ lệch nhau, và bản lệch sẽ là bản không ai nhớ đã sửa.
 */
final class ContentDefaults {

    static final String HERO = "hero";
    static final String ABOUT = "about";
    static final String CONTACT = "contact";
    static final String MAP = "map";

    private static final Map<String, SectionView> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put(HERO, new SectionView(
                HERO,
                "Homestay TVH",
                "Nghỉ ngơi giữa miền Tây sông nước",
                "<p>Không gian yên tĩnh, gần gũi thiên nhiên, cách trung tâm Trà Vinh vài phút đi xe.</p>",
                null,
                null));
        DEFAULTS.put(ABOUT, new SectionView(
                ABOUT,
                "Về Homestay TVH",
                "Một nơi để thở chậm lại",
                "<p>Homestay TVH là nhà của chúng tôi, mở cửa đón khách phương xa. "
                        + "Mỗi phòng đều nhìn ra vườn, và bữa sáng là món nhà nấu.</p>",
                null,
                null));
        DEFAULTS.put(CONTACT, new SectionView(
                CONTACT,
                "Liên hệ",
                "Chúng tôi luôn sẵn sàng trả lời",
                "<p>Điện thoại: 0294 3855 246<br />Email: lienhe@homestaytvh.vn</p>",
                null,
                null));
        DEFAULTS.put(MAP, new SectionView(
                MAP,
                "Đường tới homestay",
                "Trà Vinh",
                null,
                null,
                null));
    }

    private ContentDefaults() {}

    static Map<String, SectionView> all() {
        return new LinkedHashMap<>(DEFAULTS);
    }
}
