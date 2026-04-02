package commands;

import storage.RedisDatabase;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class LPopCommand implements RedisCommand {
    private final RedisDatabase db;

    public LPopCommand(RedisDatabase db) {
        this.db = db;
    }

    @Override
    public void execute(String[] parts, OutputStream out) throws IOException {
        // Request: LPOP list_key
        // RESP: *2\r\n$4\r\nLPOP\r\n$8\r\nlist_key\r\n

        // Response (Success): Bulk String
        // $<length>\r\n<data>\r\n
        // Ví dụ: $3\r\none\r\n

        // Response (Empty/Missing): Null Bulk String
        // $-1\r\n
        String key = parts[4];
        if (parts.length <= 5) {
            String value = db.lpop(key);

            if (value == null) {
                // Trả về Null Bulk String nếu không có gì để pop
                out.write("$-1\r\n".getBytes());
            } else {
                // Trả về Bulk String của phần tử bị xóa
                String response = "$" + value.length() + "\r\n" + value + "\r\n";
                out.write(response.getBytes());
            }
        }
        // Request: LPOP list_key 2
        // RESP: *3\r\n$4\r\nLPOP\r\n$8\r\nlist_key\r\n$1\r\n2\r\n

        // Response (Success): Array
        // *<number_of_elements>\r\n$<len1>\r\n<val1>\r\n$<len2>\r\n<val2>\r\n
        // Ví dụ: *2\r\n$3\r\none\r\n$3\r\ntwo\r\n

        // Response (Empty/Missing): Null Array (hoặc Empty Array tùy version)
        // *-1\r\n  (hoặc *0\r\n)
        else {
            int count = Integer.parseInt(parts[6]);
            List<String> values = db.lpop(key,count);

            if (values.isEmpty()){
                out.write("*-1\r\n".getBytes());
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("*").append(values.size()).append("\r\n");
                for (String val : values) {
                    sb.append("$").append(val.length()).append("\r\n").append(val).append("\r\n");
                }
                out.write(sb.toString().getBytes());
            }
        }
    }
}
