import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
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
                        output.write("+PONG\r\n".getBytes());
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
