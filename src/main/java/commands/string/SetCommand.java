package commands.string;

import commands.RedisCommand;
import storage.RedisDatabase;

import java.io.IOException;
import java.io.OutputStream;

public class SetCommand implements RedisCommand {
    private final RedisDatabase db;

    public SetCommand(RedisDatabase db) {
        this.db = db;
    }

    @Override
    public void execute(String[] parts, OutputStream out) throws IOException {
        String key = parts[4];
        String value = parts[6];
        Long expiry = null;
        // Kiểm tra xem có đủ phần tử cho PX hay không
        // Cấu trúc: SET (2) key (4) value (6) PX (8) time (10)
        if (parts.length >= 11 && "PX".equalsIgnoreCase(parts[8])) {
            try {
                long duration = Long.parseLong(parts[10]);
                expiry = System.currentTimeMillis() + duration;
            } catch (NumberFormatException e) {
                // Nếu gửi PX mà không kèm số hợp lệ, có thể báo lỗi hoặc bỏ qua
            }
        }
        db.set(key,value,expiry);
        out.write("+OK\r\n".getBytes());
    }
}
