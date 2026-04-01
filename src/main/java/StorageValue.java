class StorageValue {
    String value;
    Long expiryTime;

    StorageValue(String value, Long expiryTime) {
        this.value = value;
        this.expiryTime = expiryTime;
    }

    boolean isExpired() {
        if (expiryTime == null) return false;
        return System.currentTimeMillis() > expiryTime;
    }
}
