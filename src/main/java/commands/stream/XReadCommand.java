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
        int streamsIndex = -1;
        for (int i = 0; i < parts.length; i++){
            if ("STREAMS".equalsIgnoreCase(parts[i])){
                streamsIndex = i;
                break;
            }
        }

        // Tính toán số cặp key và id (streams)
        // Số tham số còn lại = Tổng độ dài - (vị trí stream + 1)
        // cần phải \2 nữa vì mỗi giá trị nội dung đi kèm 1 dòng len(không cần thiết)
        int totalArgsAfterStreams = (parts.length - (streamsIndex+1))/2;
        int numStreams = totalArgsAfterStreams / 2;

        StringBuilder resp = new StringBuilder();
        resp.append("*").append(numStreams).append("\r\n");
        // tìm key dựa từ stream index, + thêm bước nhảy i lên 2 ô tiếp để né cái length
        for (int i = 0; i < numStreams; i++) {
            String key = parts[streamsIndex + 2 + (i*2)];
            String lastId = parts[streamsIndex + 2 + (numStreams * 2) + (i*2)];

            List<StreamEntry> entries = db.xread(key,lastId);

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
        }
        out.write(resp.toString().getBytes());
    }
}
