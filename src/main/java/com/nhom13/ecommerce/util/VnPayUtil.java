package com.nhom13.ecommerce.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

public class VnPayUtil {

    /**
     * Tạo chữ ký HMAC-SHA512.
* [12, 13]
     */
    public static String hmacSHA512(final String key, final String data) {
        try {
            //
            if (key == null || data == null) {
                throw new NullPointerException();
}
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
// Sửa 'byte' thành 'byte[]'
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            
            // Sửa 'byte' thành 'byte[]'
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
// Sửa 'byte' thành 'byte[]'
            byte[] result = hmac512.doFinal(dataBytes);
// Chuyển byte array sang hex string
            // (Các lỗi 16, 17) Vòng lặp
            StringBuilder sb = new StringBuilder(2 * result.length);
for (byte b : result) {
                sb.append(String.format("%02x", b));
}
            return sb.toString();
} catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC-SHA512", e);
}
    }
    
    /**
     * Xây dựng chuỗi dữ liệu (query string) từ Map, đã sắp xếp và URL-encoded.
* [14, 15]
     */
    public static String buildQueryString(Map<String, String> params) {
        // Sắp xếp params theo thứ tự bảng chữ cái (bắt buộc)
        Map<String, String> sortedParams = new TreeMap<>(params);
StringJoiner sj = new StringJoiner("&");
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            String key = entry.getKey();
String value = entry.getValue();
            if (value!= null &&!value.isEmpty()) {
                // Mã hóa URL-encode cho giá trị
                sj.add(key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
}
        }
        return sj.toString();
}

    /**
     * Phương thức xác minh chữ ký VNPAY.
* [3, 4]
     */
    public static boolean verifySignature(Map<String, String> params, String secretKey) {
        String receivedHash = params.get("vnp_SecureHash");
//
        if (receivedHash == null || receivedHash.isEmpty()) {
            return false;
}
        
        // Loại bỏ hash và hashType khỏi map
        params.remove("vnp_SecureHash");
params.remove("vnp_SecureHashType"); // Loại bỏ cả hashType nếu có

        String dataToHash = buildQueryString(params);
String generatedHash = hmacSHA512(secretKey, dataToHash);
        
        return generatedHash.equalsIgnoreCase(receivedHash);
    }
}
