package commands;

import storage.RedisDatabase;

import java.io.IOException;
import java.io.OutputStream;
/*
* Lệnh lấy phần tử ở đầu danh sách có hỗ tro chờ đợi
* List có phần tử: lấy ra và trả về ngay
* List empty: treo kết nối đợi khi có dữ liệu hoặc hết timeout
* nếu lệnh ghi là timeout = 0: chờ cho đến khi có lệnh rpush/lpush từ client khác*/
public class BLPopCommand implements RedisCommand{
    private final RedisDatabase db;

    public BLPopCommand(RedisDatabase db) {
        this.db = db;
    }
    @Override
    public void execute(String[] parts, OutputStream out) throws IOException {
        // Cấu trúc request RESP: *4\r\n$5\r\nBLPOP\r\n$8\r\nlist_key\r\n$1\r\n0\r\n
        String key = parts[4];
        try {
            double timeoutDouble = Double.parseDouble(parts[6]);
            double timeoutMillis = timeoutDouble * 1000;
            if (timeoutMillis > Long.MAX_VALUE) {
                timeoutMillis = Long.MAX_VALUE; // Chặn lại ở mức tối đa
            }
            long finalTimeout = (long) timeoutMillis;
            String result = db.blpop(key,finalTimeout);
            if (result == null){
                out.write("*-1\r\n".getBytes());
            } else {
                // BLPOP khi thành công trả về Array 2 phần tử: [tên_key, giá_trị_pop]
                StringBuilder resp = new StringBuilder();
                resp.append("*2\r\n");
                resp.append("$").append(key.length()).append("\r\n").append(key).append("\r\n");
                resp.append("$").append(result.length()).append("\r\n").append(result).append("\r\n");
                out.write(resp.toString().getBytes());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            out.write("-ERR Interrupted\r\n".getBytes());
        }
    }
}
