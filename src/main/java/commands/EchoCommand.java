package commands;

import java.io.IOException;
import java.io.OutputStream;
/*
 * Lệnh echo, dùng để kiểm tra đường truyền, check dữ liệu gửi
 * và trả về có chính xác không
 * cú pháp redis-cli ECHO (noidung)*/
/*
 * Bulk String:(1 loại dữ liệu thuộc resp) loại container dùng cho chuỗi dữ liệu có độ dài
 * bất kỳ (chữ cái hoặc file ảnh gì đo)
 * Cấu trúc: $ + độ dài chuỗi + \r\n + nội dung chuỗi + \r\n
 * 1 vài ký tự khác: * + số nguyên: báo hiệu gửi 1 mảng gồm số nguyên phần tử*/

/*
 * vậy khi chạy lệnh redis-cli ECHO + nội dung thì sẽ gửi vào file socket dạng như này
 * *2\r\n$4\r\nECHO\r\n$3\r\n(nội dung)\r\n
 * mục tiêu là mình sẽ phải trích xuất được nội dung trong đó rồi gói nó lại thành 1 bulk string để gửi lại*/
public class EchoCommand implements RedisCommand {
    @Override
    public void execute(String[] parts, OutputStream out) throws IOException {
        // parts[0] = *2
        // parts[1] = $4
        // parts[2] = ECHO
        // parts[3] = $3 (độ dài nội dung)
        // parts[4] = nội dung (ví dụ: hey)

        if (parts.length >= 5) {
            String argument = parts[4];

            // Xây dựng Bulk String response: $<length>\r\n<data>\r\n
            String response = "$" + argument.length() + "\r\n" + argument + "\r\n";

            out.write(response.getBytes());
        } else {
            // Trường hợp user chỉ gõ ECHO mà không có nội dung
            out.write("-ERR wrong number of arguments for 'echo' command\r\n".getBytes());
        }
    }
}