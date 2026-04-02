import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    // storage dữ liệu chung cho tất cả cac luồng
    private static final Map<String, StorageValue> storage = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> listStorage = new ConcurrentHashMap<>();
  public static void main(String[] args){
    // You can use print statements as follows for debugging, they'll be visible when running tests.


      /*
      * Socket là 1 file giúp máy tính có thể đẩy dữ liệu ra phía card mạng
      * Phân tích:
      * Về mặt phần cứng, CPU và RAM chỉ có thể nói chuyện với các thiết bị ngoại vi thông
      * qua các chuẩn giao tiếp. Card mạng (NIC -Network Interface Card) là thiết bị như vậy
      * ứng dụng java không được phép điều khển trực tiếp card mạng
      * phải thông qua kernel( nhân hệ điều hành)
      * socket chính là tay nắm cửa mà kernel đưa cho. khi đẩy dữ liệu vào socket, kernel sẽ tiếp quản
      * chia nhỏ dữ liệu thành các gói tin(packets), thêm địa chỉ ip/mac và ra lệnh cho card phát sóng điện
      * tu (wifi) hoặc xung điện (dây cáp)
      * */
    System.out.println("Logs from your program will appear here!");
    int port = 6379;
    try (ServerSocket serverSocket = new ServerSocket(port)){
        serverSocket.setReuseAddress(true);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New user is coming: " + clientSocket.getRemoteSocketAddress());

            final Socket finalClientSocket = clientSocket;

            Thread thread = new Thread(() -> {
                try (Socket socket = finalClientSocket){
                    /*
                    * Socket khi khởi tại sẽ đi kèm 2 vùng nhớ như ở dưới
                    * */
                    // đây là vùng nhớ receive buffer: nhận dữ liệu từ mạng gửi tới
                    InputStream input = socket.getInputStream();
                    // đây là vùng nhớ send buffer, dữ liệu được đẩy vào đây, os sẽ quyết định khi nào
                    // chia nhỏ vùng nhớ này thành các TCP Segments dể gửi đi
                    OutputStream output = socket.getOutputStream();
                    // đây là thùng hứng dữ liệu được lấy về vùng input
                    byte[] buffer = new byte[1024];
                    int bytesRead;

                    while ((bytesRead = input.read(buffer)) != -1){
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
                        String request = new String(buffer,0,bytesRead);

                        String[] parts = request.split("\r\n");
                        // Mảng sau khi cắt sẽ là: ["*2", "$4", "ECHO", "$3", "hey"].
                        String command = parts[2];
                        // parts[0] thường là *<số lượng phần tử>
                        // parts[2] thường là lệnh (ví dụ: ECHO hoặc PING)
                        // parts[4] thường là đối số đầu tiên (ví dụ: hey)
                        if (command.equals("PING")){
                            output.write("+PONG\r\n".getBytes());
                        } else if (command.equals("ECHO")) {
                            String argument = parts[4];
                            String response = "$" + argument.length() + "\r\n" + argument + "\r\n";
                            output.write(response.getBytes());

                        } else if (command.equals("SET")){
                            String key = parts[4];
                            String value = parts[6];
                            Long expiry = null;
                            if (parts.length >= 11 && parts[8].equalsIgnoreCase("PX")){
                                long duration = Long.parseLong(parts[10]);
                                expiry = System.currentTimeMillis() + duration;
                            }
                            storage.put(key,new StorageValue(value, expiry));
                            output.write("+OK\r\n".getBytes());
                        } else if (command.equals("GET")){
                            String key = parts[4];
                            StorageValue entry = storage.get(key);
                            if (entry == null || entry.isExpired()){
                                if (entry != null) storage.remove(key);
                                output.write("$-1\r\n".getBytes());
                            } else {
                                String val = entry.value;
                                String response = "$" + val.length() + "\r\n" + val + "\r\n";
                                output.write(response.getBytes());
                            }
                        } else if (command.equals("RPUSH")) {
                            // Lệnh RPUSH để thêm 1 phần tử vào 1 list
                            // Syntax:redis-cli RPUSH list element

                            String key = parts[4];
                            /*
                            * Trong môi trường đa luồng, nếu một ngươ đang đọc mà người khác đang
                            * thêm vào list thì java sẽ ném lỗi ConcurrentModificationException
                            * CopyOnWriteArrayList: khi gọi add thì nó sẽ copy tạo ra bản sao mới của list
                            * hiện tại, thêm phần tử vào đó rồi sẽ copy mảng mới vào nội bộ mảng cũ
                            * */
                            listStorage.putIfAbsent(key, new CopyOnWriteArrayList<>());
                            List<String> list = listStorage.get(key);
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
                            //RESP Integer: : + số + \r\n
                            String response = ":" + list.size() + "\r\n";
                            output.write(response.getBytes());
                        }
                        // yêu cầu gửi luôn dữ liệu không đợi đổ dữ liệu khác đầy rồi mới gửi
                        output.flush();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            thread.start();
        }
    } catch (IOException e) {
        System.err.println("ServerSocket error: " + e.getMessage());
    }
  }
}
