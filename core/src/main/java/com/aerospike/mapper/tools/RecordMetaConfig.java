package com.aerospike.mapper.tools;

/**
 * Holds annotation-derived per-class Aerospike record settings.
 * Decouples ClassCacheEntry from legacy client policy types.
 */
public class RecordMetaConfig {
    private int ttl = -1;
    private Boolean sendKey = null;
    private Boolean durableDelete = null;
    private Boolean mapAll = null;

    public int getTtl() { return ttl; }
    public void setTtl(int ttl) { this.ttl = ttl; }

    public Boolean getSendKey() { return sendKey; }
    public void setSendKey(Boolean sendKey) { this.sendKey = sendKey; }

    public Boolean getDurableDelete() { return durableDelete; }
    public void setDurableDelete(Boolean durableDelete) { this.durableDelete = durableDelete; }

    public Boolean getMapAll() { return mapAll; }
    public void setMapAll(Boolean mapAll) { this.mapAll = mapAll; }
}
