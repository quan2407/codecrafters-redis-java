package commands.generic;

import commands.RedisCommand;
import storage.RedisDatabase;

import java.io.IOException;
import java.io.OutputStream;

public class TypeCommand implements RedisCommand {
    private final RedisDatabase db;

    public TypeCommand(RedisDatabase db) {
        this.db = db;
    }

    @Override
    public void execute(String[] parts, OutputStream out) throws IOException {
        // Cấu trúc: *2\r\n$4\r\nTYPE\r\n$3\r\nkey\r\n
        String key = parts[4];
        String type = db.getType(key);
        String response = "+" + type + "\r\n";
        out.write(response.getBytes());
    }
}
