package com.tvh.homestay.common;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Đọc trước toàn bộ thân request vào bộ nhớ để đọc được NHIỀU LẦN.
 *
 * <p>Cần thiết vì giới hạn tần suất theo email phải biết email TRƯỚC khi
 * controller chạy, mà {@code ServletInputStream} chỉ đọc được một lần: filter
 * đọc xong thì controller nhận thân rỗng.
 *
 * <p>Chỉ bọc những endpoint có khoá giới hạn lấy từ thân request, và thân
 * những endpoint đó nhỏ (email, mật khẩu) nên giữ trong bộ nhớ là an toàn.
 */
public class CachedBodyRequest extends HttpServletRequestWrapper {

    private final byte[] body;

    public CachedBodyRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.body = request.getInputStream().readAllBytes();
    }

    public byte[] getBody() {
        return body;
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream buffer = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return buffer.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException("Chỉ dùng ở chế độ đồng bộ");
            }

            @Override
            public int read() {
                return buffer.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
