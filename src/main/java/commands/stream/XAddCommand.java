package commands.stream;

import commands.RedisCommand;
import storage.RedisDatabase;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class XAddCommand implements RedisCommand {
    private final RedisDatabase db;

    public XAddCommand(RedisDatabase db) {
        this.db = db;
    }
    @Override
    public void execute(String[] parts, OutputStream out) throws IOException {
        // Cấu trúc: XADD key id field1 value1 field2 value2...
        // Index:   2    4   6  8      10     12     14
        String key = parts[4];
        String id = parts[6];
        Map<String,String> fields = new LinkedHashMap<>();
        for (int i = 8; i < parts.length; i+=4) {
            String fieldKey = parts[i];
            String fieldValue = parts[i+2];
            fields.put(fieldKey,fieldValue);
        }
        String resultId = db.xadd(key,id,fields);
        String response = "$" + resultId.length() + "\r\n" + resultId + "\r\n";
        out.write(response.getBytes());
        out.flush();
    }
}
