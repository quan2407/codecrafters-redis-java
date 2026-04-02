package commands;

import storage.RedisDatabase;
import storage.StorageValue;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class GetCommand implements RedisCommand{
    private final RedisDatabase db;

    public GetCommand(RedisDatabase db) {
        this.db = db;
    }

    @Override
    public void execute(String[] parts, OutputStream out) throws IOException {
        String key = parts[4];
        String val = db.get(key);
        if (val == null){
            out.write("$-1\r\n".getBytes());
        } else {
            String response = "$" + val.length() + "\r\n" + val + "\r\n";
            out.write(response.getBytes());
        }
    }
}
