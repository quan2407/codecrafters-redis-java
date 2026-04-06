package commands.stream;

import commands.RedisCommand;
import models.StreamEntry;
import storage.RedisDatabase;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class XReadCommand implements RedisCommand {
    private final RedisDatabase db;

    public XReadCommand(RedisDatabase db) {
        this.db = db;
    }

    @Override
    public void execute(String[] parts, OutputStream out) throws IOException {
        String key = parts[4];
        String lastId = parts[6];

        List<StreamEntry> entries = db.xread(key,lastId);

        StringBuilder resp = new StringBuilder();
        // *1 -> Đọc từ 1 stream
        resp.append("*1\r\n");
        // *2 -> [key, mảng entry]
        resp.append("*2\r\n");
        // Tên key
        resp.append("$").append(key.length()).append("\r\n").append(key).append("\r\n");

        // Mảng entry
        resp.append("*").append(entries.size()).append("\r\n");
        for (StreamEntry entry : entries){
            // Mỗi entry gồm 2 phaan tu ID và fields
            resp.append("*2\r\n");
            resp.append("$").append(entry.getId().length()).append("\r\n").append(entry.getId()).append("\r\n");

            Map<String, String> fields = entry.getFields();
            resp.append("*").append(fields.size() * 2).append("\r\n");
            for (Map.Entry<String,String> f : fields.entrySet()) {
                resp.append("$").append(f.getKey().length()).append("\r\n").append(f.getKey()).append("\r\n");
                resp.append("$").append(f.getValue().length()).append("\r\n").append(f.getValue()).append("\r\n");            }
        }
        out.write(resp.toString().getBytes());
    }
}
