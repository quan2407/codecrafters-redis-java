package commands;

import storage.RedisDatabase;

import java.io.IOException;
import java.io.OutputStream;

public class LPopCommand implements RedisCommand{
    private final RedisDatabase db;

    public LPopCommand(RedisDatabase db) {
        this.db = db;
    }
    @Override
    public void execute(String[] parts, OutputStream out) throws IOException {
        if (parts.length >= 5) {
            String key = parts[4];
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
    }
}
