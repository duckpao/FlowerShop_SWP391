package com.example.flowershop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Service
public class CloudinaryService {
    private static final String IMAGE_FOLDER = "flowershop/products";
    private static final String VIDEO_FOLDER = "flowershop/product-videos";
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public CloudinaryService(@Value("${app.cloudinary.cloud-name}") String cloudName,
                              @Value("${app.cloudinary.api-key}") String apiKey,
                              @Value("${app.cloudinary.api-secret}") String apiSecret) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public String upload(byte[] bytes, String filename, String contentType) {
        return upload(bytes, filename, contentType, "image", IMAGE_FOLDER, "Upload ảnh lên Cloudinary thất bại.", "Cloudinary không trả về URL ảnh.");
    }

    public String uploadVideo(byte[] bytes, String filename, String contentType) {
        return upload(bytes, filename, contentType, "video", VIDEO_FOLDER, "Upload video lên Cloudinary thất bại.", "Cloudinary không trả về URL video.");
    }

    public String uploadAvatar(byte[] bytes) {
        return upload(bytes, "avatar.png", "image/png", "image", "flowershop/avatars",
                "Không thể tải ảnh đại diện lên.", "Không nhận được URL ảnh đại diện.");
    }

    private String upload(byte[] bytes, String filename, String contentType, String resourceType, String folder, String uploadErrorMessage, String missingUrlMessage) {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Chưa cấu hình Cloudinary trên server.");
        long timestamp = System.currentTimeMillis() / 1000;
        String signature = sha1Hex("folder=" + folder + "&timestamp=" + timestamp + apiSecret);
        String boundary = "----FlowerShopBoundary" + timestamp;
        byte[] body = buildMultipartBody(boundary, bytes, filename, contentType, folder, timestamp, signature);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cloudinary.com/v1_1/" + cloudName + "/" + resourceType + "/upload"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(response.body());
            if (response.statusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, json.path("error").path("message").asText(uploadErrorMessage));
            }
            String url = json.path("secure_url").asText(null);
            if (url == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, missingUrlMessage);
            return url;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không thể kết nối tới Cloudinary.", e);
        }
    }

    private byte[] buildMultipartBody(String boundary, byte[] fileBytes, String filename, String contentType, String folder, long timestamp, String signature) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeField(out, boundary, "api_key", apiKey);
            writeField(out, boundary, "timestamp", String.valueOf(timestamp));
            writeField(out, boundary, "folder", folder);
            writeField(out, boundary, "signature", signature);
            out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(fileBytes);
            out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể chuẩn bị dữ liệu upload.", e);
        }
    }

    private void writeField(ByteArrayOutputStream out, String boundary, String name, String value) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private static String sha1Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
