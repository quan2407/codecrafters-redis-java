package commands.list;

import commands.RedisCommand;
import storage.RedisDatabase;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class LPushCommand implements RedisCommand {
    private final RedisDatabase db;

    public LPushCommand(RedisDatabase db) {
        this.db = db;
    }
    @Override
    public void execute(String[] parts, OutputStream out) throws IOException {
        String key = parts[4];
        List<String> elements = new ArrayList<>();

        for (int i = 6; i < parts.length; i+=2) {
            elements.add(parts[i]);
        }
        int size = db.lpush(key,elements);
        out.write((":" + size + "\r\n").getBytes());
    }
}
