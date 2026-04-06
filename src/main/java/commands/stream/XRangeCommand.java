package commands.stream;

import commands.RedisCommand;
import models.StreamEntry;
import storage.RedisDatabase;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/*
* *4\r\n
$6\r\nXRANGE\r\n
$8\r\nsome_key\r\n
$13\r\n1526985054069\r\n
$13\r\n1526985054079\r\n
* */
public class XRangeCommand implements RedisCommand {
    private final RedisDatabase db;

    public XRangeCommand(RedisDatabase db) {
        this.db = db;
    }
    @Override
    public void execute(String[] parts, OutputStream out) throws IOException {
        String key = parts[4];
        String start = parts[6];
        String end = parts[8];

        List<StreamEntry> entries = db.xrange(key,start,end);

        StringBuilder resp = new StringBuilder();
        resp.append("*").append(entries.size()).append("\r\n");

        // Build RESP
        for (StreamEntry entry : entries){
            // mỗi entry là một mảng 2 phần tử : ID và list fields(map)
            resp.append("*2\r\n");
            // 1. ID
            resp.append("$").append(entry.getId().length()).append("\r\n");
            resp.append(entry.getId()).append("\r\n");

            // 2. Mảng Fields: *<số lượng key+value>
            Map<String, String> fields = entry.getFields();
            resp.append("*").append(fields.size() * 2).append("\r\n");

            for (Map.Entry<String, String> f : fields.entrySet()){
                // Key
                resp.append("$").append(f.getKey().length()).append("\r\n");
                resp.append(f.getKey()).append("\r\n");
                // Value
                resp.append("$").append(f.getValue().length()).append("\r\n");
                resp.append(f.getValue()).append("\r\n");
            }
        }
        out.write(resp.toString().getBytes());
        out.flush();
    }
}
