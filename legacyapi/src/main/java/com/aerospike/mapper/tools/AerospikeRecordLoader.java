package com.aerospike.mapper.tools;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.policy.BatchPolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements {@link RecordLoader} using the legacy {@link IAerospikeClient}.
 */
public class AerospikeRecordLoader implements RecordLoader {

    private final IAerospikeClient client;

    public AerospikeRecordLoader(IAerospikeClient client) {
        this.client = client;
    }

    @Override
    public Map<String, Object> getRecord(String namespace, String setName, Object keyValue) {
        Key key = new Key(namespace, setName, Value.get(keyValue));
        Record record = client.get(client.getReadPolicyDefault(), key);
        return record == null ? null : new HashMap<>(record.bins);
    }

    @Override
    public Map<String, Object> getRecordByDigest(String namespace, String setName, byte[] digest) {
        Key key = new Key(namespace, digest, setName, null);
        Record record = client.get(client.getReadPolicyDefault(), key);
        return record == null ? null : new HashMap<>(record.bins);
    }

    @Override
    public List<Map<String, Object>> getBatchRecords(List<RecordKey> keys) {
        Key[] aerospikeKeys = new Key[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            RecordKey rk = keys.get(i);
            if (rk.digest != null) {
                aerospikeKeys[i] = new Key(rk.namespace, rk.digest, rk.setName, null);
            } else {
                aerospikeKeys[i] = new Key(rk.namespace, rk.setName, Value.get(rk.keyValue));
            }
        }

        BatchPolicy batchPolicy = client.getBatchPolicyDefault();
        if (keys.size() <= 2) {
            batchPolicy = new BatchPolicy(batchPolicy);
            batchPolicy.maxConcurrentThreads = 1;
        }

        Record[] records = client.get(batchPolicy, aerospikeKeys);

        List<Map<String, Object>> result = new ArrayList<>(records.length);
        for (Record r : records) {
            result.add(r == null ? null : new HashMap<>(r.bins));
        }
        return result;
    }

    @Override
    public byte[] computeDigest(String setName, Object userKey) {
        return com.aerospike.client.util.Crypto.computeDigest(setName, Value.get(userKey));
    }
}
