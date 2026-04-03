package commands.list;

import commands.RedisCommand;
import storage.RedisDatabase;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
// Lệnh LRANGE dùng để lấy phần tử từ start index đến stop index
// Cấu trúc: LRANGE start_index stop_index
public class LRangeCommand implements RedisCommand {
    private final RedisDatabase db;

    public LRangeCommand(RedisDatabase db) {
        this.db = db;
    }

    @Override
    public void execute(String[] parts, OutputStream out) throws IOException {
        String key = parts[4];
        int start = Integer.parseInt(parts[6]);
        int stop = Integer.parseInt(parts[8]);

        List<String> subList = db.lrange(key, start, stop);

        out.write(("*" + subList.size() + "\r\n").getBytes());

        for (String item : subList) {
            out.write(("$" + item.length() + "\r\n" + item + "\r\n").getBytes());
        }
    }
}
