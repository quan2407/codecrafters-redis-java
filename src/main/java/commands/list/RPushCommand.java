package commands.list;

import commands.RedisCommand;
import storage.RedisDatabase;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
// Lệnh RPUSH để thêm 1 phần tử vào 1 list
// Syntax:redis-cli RPUSH list element
public class RPushCommand implements RedisCommand {
    private final RedisDatabase db;

    public RPushCommand(RedisDatabase db) {
        this.db = db;
    }

    @Override
    public void execute(String[] parts, OutputStream out) throws IOException {
        String key = parts[4];
        List<String> list = new ArrayList<>();
        /*
         * Phương pháp thêm nhiều phần tử:
         * nội dung file khi dùng lenh rpush
         * *5\r\n$5\r\nRPUSH\r\n$8\r\nlist_key\r\n$2\r\ne1\r\n$2\r\ne2\r\n$2\r\n
         * phân tích ta thấy từ part 4 trở đi, cứ mỗi +2 index sẽ la 1 phần tu(phan tu le la số lượng
         * phần tử) => dùng vòng lặp for*/
        for (int i = 6; i < parts.length; i+=2) {
            String element = parts[i];
            list.add(element);
        }
        int size = db.rpush(key,list);
        //RESP Integer: : + số + \r\n
        String response = ":" + size + "\r\n";
        out.write(response.getBytes());
    }
}
