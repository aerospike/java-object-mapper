package com.aerospike.mapper.tools.fluent;

import com.aerospike.client.fluent.DataSet;
import com.aerospike.client.fluent.Key;
import com.aerospike.client.fluent.Record;
import com.aerospike.client.fluent.RecordStream;
import com.aerospike.client.fluent.Session;
import com.aerospike.client.fluent.Value;
import com.aerospike.mapper.exceptions.AerospikeMapperException;
import com.aerospike.mapper.tools.RecordKey;
import com.aerospike.mapper.tools.RecordLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements {@link RecordLoader} using the fluent client's {@link Session}.
 */
public class FluentRecordLoader implements RecordLoader {

    private final Session session;

    public FluentRecordLoader(Session session) {
        this.session = session;
    }

    @Override
    public Map<String, Object> getRecord(String namespace, String setName, Object keyValue) {
        Key key = DataSet.of(namespace, setName).idForObject(keyValue);
        try (RecordStream rs = session.query(key).executeSync()) {
            Record record = rs.getFirstRecord();
            return record == null ? null : new HashMap<>(record.bins);
        }
    }

    @Override
    public Map<String, Object> getRecordByDigest(String namespace, String setName, byte[] digest) {
        Key key = DataSet.of(namespace, setName).idFromDigest(digest);
        try (RecordStream rs = session.query(key).executeSync()) {
            Record record = rs.getFirstRecord();
            return record == null ? null : new HashMap<>(record.bins);
        }
    }

    @Override
    public List<Map<String, Object>> getBatchRecords(List<RecordKey> keys) {
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<Key> fluentKeys = new ArrayList<>(keys.size());
        for (RecordKey rk : keys) {
            fluentKeys.add(rk.digest != null
                ? DataSet.of(rk.namespace, rk.setName).idFromDigest(rk.digest)
                : DataSet.of(rk.namespace, rk.setName).idForObject(rk.keyValue));
        }

        // Index-based ordering: map each Key identity to its position
        Map<Key, Integer> keyIndexMap = new HashMap<>(fluentKeys.size());
        for (int i = 0; i < fluentKeys.size(); i++) {
            keyIndexMap.put(fluentKeys.get(i), i);
        }

        List<Map<String, Object>> results = new ArrayList<>(Collections.nCopies(keys.size(), null));

        try (RecordStream rs = session.query(fluentKeys).executeSync()) {
            while (rs.hasNext()) {
                var result = rs.next();
                Integer idx = keyIndexMap.get(result.key());
                if (idx != null && result.isOk() && result.recordOrNull() != null) {
                    results.set(idx, new HashMap<>(result.recordOrNull().bins));
                }
            }
        } catch (AerospikeMapperException e) {
            throw e;
        } catch (Exception e) {
            throw new AerospikeMapperException("Batch record fetch failed", e);
        }

        return results;
    }

    @Override
    public byte[] computeDigest(String setName, Object userKey) {
        return Key.computeDigest(setName, Value.get(userKey));
    }
}
