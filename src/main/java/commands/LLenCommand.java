package commands;

import storage.RedisDatabase;

import java.io.IOException;
import java.io.OutputStream;

public class LLenCommand implements RedisCommand{
    private final RedisDatabase db;

    public LLenCommand(RedisDatabase db) {
        this.db = db;
    }
    @Override
    public void execute(String[] parts, OutputStream out) throws IOException {
        // Cấu trúc: *2\r\n$4\r\nLLEN\r\n$8\r\nlist_key\r\n
        if (parts.length >= 5) {
            String key = parts[4];
            int length = db.llen(key);
            // Trả về RESP Integer: :<value>\r\n
            out.write((":" + length + "\r\n").getBytes());
        } else {
            out.write("-ERR wrong number of arguments for 'llen' command\r\n".getBytes());
        }

    }
}
