# Metadata Caching Tests

This directory contains tests that verify the metadata caching behavior of the Java Object Mapper.

**Total: 7 tests** (reduced from initial 15 by eliminating duplicates)

## Test Files

### 1. ClassCacheUnitTest.java (5 tests)
**Purpose:** Unit tests that verify ClassCache behavior without requiring a database connection.

**Key Tests:**
- `testSameInstanceReturnedOnRepeatedLoads()` - **primary test**, verifies that multiple calls to 
`ClassCache.getInstance().loadClass()` return the **exact same ClassCacheEntry instance**, 
proving that reflection happens only once. Includes stress test with 100 iterations.
- `testHasClassTracksLoadedClasses()` - Verifies cache state tracking with `hasClass()`
- `testMultipleClassesCachedSimultaneously()` - Tests cache with multiple types loaded and reloaded
- `testClearRemovesAllEntries()` - Verifies that clearing cache forces new reflection
- `testCacheSharedAcrossMapperInstances()` - Verifies singleton behavior across mapper instances

**Running:** These tests use a mocked `IAerospikeClient` and run without a live Aerospike server.

### 2. MetadataCachingTest.java (2 tests)
**Purpose:** Integration tests that verify caching behavior during actual database read/write operations.

**Key Tests:**
- `testMetadataCachedAcrossMixedOperations()` - Comprehensive test verifying cache consistency 
- with mixed `save()` and `read()` operations
- `testCacheWithHighVolumeOperations()` - Stress test with 100 save operations plus reads

**Requirements:** These tests require a running Aerospike server.

## What These Tests Prove

### ✅ Reflection Happens Only Once
The tests use `assertSame(entry1, entry2)` to verify that multiple loads return the **exact same object instance** 
(by reference comparison), not just equivalent objects. This proves that:
1. The first `loadClass()` call performs expensive reflection and creates a ClassCacheEntry
2. Subsequent `loadClass()` calls return the cached instance without re-running reflection
3. No redundant annotation processing or field discovery occurs

### ✅ Same Instance Across Operations
The integration tests verify that:
- Multiple `mapper.save()` calls use the same cached ClassCacheEntry
- Multiple `mapper.read()` calls use the same cached ClassCacheEntry
- Mixed read/write operations share the same cached metadata

This proves that **metadata is computed once and reused for all subsequent database operations**.

### ✅ Cache Lifecycle
The tests verify:
- Cache starts empty (`hasClass()` returns false initially)
- First use populates the cache
- Cache persists across operations
- `clear()` removes entries and forces new reflection
- After clear, a different ClassCacheEntry instance is created

## Running the Tests

### With Aerospike Server Running

```bash
# Run all caching tests
mvn test -Dtest=MetadataCachingTest,ClassCacheUnitTest

# Run specific test
mvn test -Dtest=ClassCacheUnitTest#testSameInstanceReturnedOnRepeatedLoads
```

### Without Aerospike Server

The unit tests (`ClassCacheUnitTests`) use Mockito to mock the Aerospike client and run fully without a server.
Only the integration tests (`MetadataCachingTests`) require a running server.

## Example Test Output

```
[INFO] Running com.aerospike.mapper.ClassCacheUnitTests
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

When a test passes, it confirms:
- `assertSame()` passed → Same instance returned → No redundant reflection
- Cache state matches expectations → Proper caching behavior

## Key Assertions

```java
// Verify same instance (proves no redundant reflection)
ClassCacheEntry<Person> entry1 = ClassCache.getInstance().loadClass(Person.class, mapper);
ClassCacheEntry<Person> entry2 = ClassCache.getInstance().loadClass(Person.class, mapper);
assertSame(entry1, entry2); // Same object reference = cached instance reused

// Verify cache state
assertFalse(ClassCache.getInstance().hasClass(TestClass.class)); // Before load
ClassCache.getInstance().loadClass(TestClass.class, mapper);
assertTrue(ClassCache.getInstance().hasClass(TestClass.class)); // After load

// Verify clear forces new reflection
ClassCacheEntry<TestClass> before = ClassCache.getInstance().loadClass(TestClass.class, mapper);
ClassCache.getInstance().clear();
ClassCacheEntry<TestClass> after = ClassCache.getInstance().loadClass(TestClass.class, mapper);
assertNotSame(before, after); // Different instance = new reflection occurred
```

## Coverage Summary

| Aspect | Verified By | Test Method |
|--------|-------------|-------------|
| **Same instance on repeated loads** | ClassCacheUnitTest | testSameInstanceReturnedOnRepeatedLoads (includes 100-iteration stress test) |
| **Cache state tracking** | ClassCacheUnitTest | testHasClassTracksLoadedClasses |
| **Multiple classes isolated** | ClassCacheUnitTest | testMultipleClassesCachedSimultaneously |
| **Clear forces new reflection** | ClassCacheUnitTest | testClearRemovesAllEntries |
| **Singleton behavior** | ClassCacheUnitTest | testCacheSharedAcrossMapperInstances |
| **Cache across mixed operations** | MetadataCachingTest | testMetadataCachedAcrossMixedOperations |
| **High volume stress test** | MetadataCachingTest | testCacheWithHighVolumeOperations (100 operations) |

## Refactoring History

**Original:** 15 tests (7 unit + 8 integration)
**Refactored:** 7 tests (5 unit + 2 integration)
**Reduction:** 53%

**Removed duplicates:**
- Merged stress tests into primary tests
- Removed redundant same-instance checks
- Eliminated unit test concepts duplicated in integration tests
- Consolidated write-only and read-only tests into mixed operations test

## Technical Details

### What Gets Cached
- Field metadata (which fields map to which bins)
- Annotation processing results (@AerospikeKey, @AerospikeBin, etc.)
- Type converters (TypeMapper instances)
- Constructor and Method references
- Policies (read/write/batch/query/scan)

### What Doesn't Get Cached
- Object instances (each read creates new POJOs)
- Database records (each operation hits the database)
- Thread-local state (LoadedObjectResolver is per-operation)

### Performance Impact
By caching metadata, the mapper avoids:
- Java reflection API calls on every operation
- Annotation processing overhead
- Field discovery and type analysis
- Constructor/method lookup

Instead, it performs a simple HashMap lookup to get the cached ClassCacheEntry, 
then uses cached Method references and TypeMapper instances for data conversion.
