package storage;

import models.StreamEntry;

import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class RedisDatabase {
    private final Map<String, StorageValue> stringStorage = new ConcurrentHashMap<>();
    private final Map<String, BlockingDeque<String>> listStorage = new ConcurrentHashMap<>();
    //key ở đây là định danh cho ấu trúc d liệu
    private final Map<String, List<StreamEntry>> streamStorage = new ConcurrentHashMap<>();

    public void set(String key, String value, Long expiryTime) {
        stringStorage.put(key, new StorageValue(value, expiryTime));
    }

    public String get(String key) {
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

    private BlockingDeque<String> getOrInitList(String key) {
        return listStorage.computeIfAbsent(key, k -> new LinkedBlockingDeque<>());
    }

    public int rpush(String key, List<String> elements) {
        BlockingDeque<String> deque = getOrInitList(key);
        for (String e : elements) {
            deque.addLast(e);
        }
        return deque.size();
    }

    public List<String> lrange(String key, int start, int stop) {
        BlockingDeque<String> deque = listStorage.get(key);
        if (deque == null) return Collections.emptyList();
        List<String> list = new ArrayList<>(deque); // deque không hỗ trợ truy cập index trực tiếp
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
        BlockingDeque<String> deque = getOrInitList(key);
        for (String e : elements) {
            deque.addFirst(e);
        }
        return deque.size();
    }

    public int llen(String key) {
        BlockingDeque<String> deque = listStorage.get(key);
        return (deque == null) ? 0 : deque.size();
    }

    public String lpop(String key) {
        BlockingDeque<String> deque = listStorage.get(key);
        if (deque == null || deque.isEmpty()) return null;
        return deque.pollFirst(); // remove first element
    }


    public List<String> lpop(String key, int count) {
        BlockingDeque<String> deque = listStorage.get(key);
        if (deque == null || deque.isEmpty()) return null;
        List<String> poppedElements = new ArrayList<>();
        int actualPopCount = Math.min(count, deque.size());
        for (int i = 0; i < actualPopCount; i++) {
            poppedElements.add(deque.pollFirst());
        }
        return poppedElements;
    }

    public String blpop(String key, long timeoutInSeconds) throws InterruptedException {
        BlockingDeque<String> deque = getOrInitList(key);
        if (timeoutInSeconds == 0) {
            //BLPOP list_key 0 (dành cho đợi vô hạn)
            return deque.takeFirst();
        } else {
            //BLPOP list_key 5 (dành cho đợi có thời hạn)
            return deque.pollFirst(timeoutInSeconds, TimeUnit.MILLISECONDS);
        }
    }

    public String getType(String key) {
        if (stringStorage.containsKey(key)) {
            StorageValue entry = stringStorage.get(key);
            if (entry != null && entry.isExpired()) {
                stringStorage.remove(key);
                return "none";
            }
            return "string";
        }

        if (listStorage.containsKey(key)) {
            return "list";
        }
        if (streamStorage.containsKey(key)) return "stream";
        return "none";
    }

    public String xadd(String key, String id, Map<String, String> fields) throws IllegalArgumentException {
        if ("0-0".equals(id)){
            throw new IllegalArgumentException("ERR The ID specified in XADD must be greater than 0-0");
        }

        List<StreamEntry> entries = streamStorage.computeIfAbsent(key, k -> new ArrayList<>());
        synchronized (entries) {
            if (!entries.isEmpty()){
                StreamEntry lastEntry = entries.get(entries.size()-1);
                if (!isValidNewId(id, lastEntry.getId())) {
                    throw new IllegalArgumentException("ERR The ID specified in XADD is equal or smaller than the target stream top item");                }
            }
            entries.add(new StreamEntry(id, fields));
        }
        return id;
    }

    private boolean isValidNewId(String newId, String lastId) {
        String[] newParts = newId.split("-");
        String[] lastParts = lastId.split("-");

        long newMs = Long.parseLong(newParts[0]);
        long newSeq = Long.parseLong(newParts[1]);
        long lastMs = Long.parseLong(lastParts[0]);
        long lastSeq = Long.parseLong(lastParts[1]);
        // ms: compare by milisecond
        if (newMs > lastMs) return true;
        if (newMs == lastMs) return newSeq > lastSeq;
        return false;
    }
}
