import commands.CommandRegistry;
import storage.RedisDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class Main {
    // Database and Registry Singleton
    private static final RedisDatabase db = new RedisDatabase();
    private static final CommandRegistry registry = new CommandRegistry(db);

    // Tạo Thread Pool để quản lý kết nối (10 client cùng lúc)
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
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
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New user is coming: " + clientSocket.getRemoteSocketAddress());
                // Đẩy việc xử lý vào Thread Pool
                executor.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("ServerSocket error: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    private static void handleClient(Socket clientSocket) {
        /*
         * Socket khi khởi tại sẽ đi kèm 2 vùng nhớ như ở dưới
         * */
        // InputStream là vùng nhớ receive buffer: nhận dữ liệu từ mạng gửi tới
        // OutputStream là vùng nhớ send buffer, dữ liệu được đẩy vào đây, os sẽ quyết định khi nào
        // chia nhỏ vùng nhớ này thành các TCP Segments dể gửi đi
        try (clientSocket;
             InputStream input = clientSocket.getInputStream();
             OutputStream output = clientSocket.getOutputStream()) {
            // đây là thùng hứng dữ liệu được lấy về vùng input
            byte[] buffer = new byte[1024];
            int bytesRead;
            // Vòng lặp giữ kết nối (Persistent Connection)
            while ((bytesRead = input.read(buffer)) != -1) {
                String request = new String(buffer, 0, bytesRead);

                // 1. Parse request thành các phần (Basic split)
                String[] parts = request.split("\r\n");

                // 2. Kiểm tra xem có đủ dữ liệu để đọc Command name không
                if (parts.length > 2) {
                    // 3. Ủy quyền xử lý cho Registry
                    registry.handle(parts, output);
                }

                // Đẩy dữ liệu đi ngay lập tức
                output.flush();
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        }
    }
}
