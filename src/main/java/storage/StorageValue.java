package storage;

public class StorageValue {
    String value;
    Long expiryTime;

    public StorageValue(String value, Long expiryTime) {
        this.value = value;
        this.expiryTime = expiryTime;
    }

    public boolean isExpired() {
        if (expiryTime == null) return false;
        return System.currentTimeMillis() > expiryTime;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Long getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(Long expiryTime) {
        this.expiryTime = expiryTime;
    }
}
