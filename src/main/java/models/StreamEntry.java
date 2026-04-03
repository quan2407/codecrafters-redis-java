package models;

import java.util.Map;
/**
 * StreamEntry đại diện cho một "bản ghi" (Entry) duy nhất trong Redis Stream.
 * Theo định nghĩa của Redis:
 * - Stream là một chuỗi các mục (entries) được lưu theo thứ tự thời gian (chronological order).
 * - Mỗi mục (Entry) là một thực thể độc lập có định danh và dữ liệu riêng.
 */
public class StreamEntry {
    // ID duy nhất của mục (Ví dụ: "1526985054069-0").
    // Dùng để định vị mục trong dòng thời gian của Stream.
    private String id;
    // Chứa một hoặc nhiều cặp khóa-giá trị (Field-Value pairs).
    // Ví dụ: { "temperature": "36", "humidity": "95" }
    // Sử dụng Map để linh hoạt số lượng cặp dữ liệu mà người dùng gửi qua lệnh XADD.
    Map<String,String> fields;

    public StreamEntry(String id, Map<String, String> fields) {
        this.id = id;
        this.fields = fields;
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getFields() {
        return fields;
    }
}
