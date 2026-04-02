package storage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RedisDatabase {
    private final Map<String, StorageValue> stringStorage = new ConcurrentHashMap<>();
    private final Map<String, List<String>> listStorage = new ConcurrentHashMap<>();

    public void set(String key, String value, Long expiryTime){
        stringStorage.put(key, new StorageValue(value,expiryTime));
    }

    public String get(String key){
        StorageValue entry = stringStorage.get(key);
        if (entry == null || entry.isExpired()) {
            if (entry != null) stringStorage.remove(key);
            return null;
        }
        return entry.getValue();
    }

    /*
     * Trong môi trường đa luồng, nếu một ngươ đang đọc mà người khác đang
     * thêm vào list thì java sẽ ném lỗi ConcurrentModificationException
     * CopyOnWriteArrayList: khi gọi add thì nó sẽ copy tạo ra bản sao mới của list
     * hiện tại, thêm phần tử vào đó rồi sẽ copy mảng mới vào nội bộ mảng cũ
     * */
    public int rpush(String key, List<String> elements) {
        listStorage.putIfAbsent(key, new java.util.concurrent.CopyOnWriteArrayList<>());
        List<String> list = listStorage.get(key);
        list.addAll(elements);
        return list.size();
    }

    public List<String> lrange(String key, int start, int stop) {
        List<String> list = listStorage.get(key);

        if (list == null) {
            return Collections.emptyList();
        }

        int size = list.size();

        // --- LOGIC XỬ LÝ INDEX ÂM ---
        // Nếu start < 0, nó sẽ tính từ cuối list ngược lại. VD: -1 là phần tử cuối.
        if (start < 0) start = size + start;
        if (stop < 0) stop = size + stop;

        // --- RÀNG BUỘC GIỚI HẠN (CLAMPING) ---
        // Nếu sau khi tính toán start vẫn âm thì đưa về 0
        if (start < 0) start = 0;
        // Nếu stop vượt quá size thì chặn lại ở phần tử cuối cùng
        if (stop >= size) stop = size - 1;

        // Kiểm tra điều kiện dừng sau khi đã xử lý index âm và giới hạn
        if (start >= size || start > stop) {
            return Collections.emptyList();
        }


        /*
         * Tại sao lại là stop + 1?
         * do cơ chế lấy danh sách của redis và java khác nhau
         * Redis(LRANGE): Lấy cả đầu và cuối
         * VD: LRANGE 0 1: lấy cả index 0 và 1
         * Java(sublist(from,to) không lấy điểm cuối
         * sublist(0,1) -> lấy index 0
         * sublist(0,2) -> lấy 0 và 1
         * */
        return list.subList(start, stop + 1);
    }

    public int lpush(String key, List<String> elements) {
        listStorage.putIfAbsent(key, new java.util.concurrent.CopyOnWriteArrayList<>());
        List<String> list = listStorage.get(key);
        for (String element : elements){
            list.add(0,element);
        }
        return list.size();
    }

    public int llen(String key) {
        List<String> list = listStorage.get(key);
        // Nếu list không tồn tại, Redis trả về 0
        if (list == null) {
            return 0;
        }
        return list.size();
    }
}
