# Copilot Instructions for Aerospike Java Object Mapper

## Running Java commands
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
PATH=/usr/lib/jvm/java-17-openjdk-arm64/bin:$PATH

## Build & Test

```bash
# Compile (skip tests)
mvn compile -B

# Run all tests (requires a running Aerospike server on localhost:3000)
mvn clean test -B -U

# Run a single test class
mvn test -Dtest=AeroMapperTest -B

# Run a single test method
mvn test -Dtest=AeroMapperTest#testSimpleSave -B

# Run tests against a different Aerospike host
mvn test -Dtest.host=myhost:3000 -B
```

Java 8 source/target compatibility. No separate lint step.

## Architecture

This is an annotation-driven ORM that maps Java POJOs to Aerospike database records. 
It has two parallel APIs: synchronous (`AeroMapper`) and reactive (`ReactiveAeroMapper`), 
both built through the same `AbstractBuilder<T>` pattern.

### Core data flow (sync)

```
Write path (Use-case → Aerospike):

  save(obj) → ClassCacheEntry.getBins(obj) → IAerospikeClient.put(bins)

   1. JavaMapperApplication → Entry point, calls mapper.save(customer)
   2. AeroMapper → API layer, creates WritePolicy, extracts key, converts to bins
   3. ClassCacheEntry<Customer> → Reflection engine, iterates fields, extracts values
   4. TypeMapper → Converts Java types (Date, List) to Aerospike format (Long, Map)
   5. AerospikeClient → SDK handles network I/O
   6. Aerospike Server → Persists record in namespace "test", set "customer"

Read path (Aerospike → Use-case):

  read(Class<T>, key) → IAerospikeClient.get() → MappingConverter → ClassCacheEntry.hydrateFromRecord()

   1. JavaMapperApplication → Calls mapper.read(Customer.class, "cust1")
   2. AeroMapper → Constructs Key, checks cache, calls client.get()
   3. AerospikeClient → Retrieves Record from server
   4. Aerospike Server → Returns Record with bin data
   5. MappingConverter → Orchestrates conversion
   6. ClassCacheEntry<Customer> → Constructs Customer, iterates bins, populates fields (hydrateFromRecord())
   7. TypeMapper → Converts Aerospike format back to Java types
   8. Customer Object → Fully hydrated and returned

VirtualList append path (e.g. appending an Item to a Container's embedded list):

  mapper.asBackedList(container, "items", Item.class).append(new Item(500, new Date(), "Item5"))

   1. AeroMapper.asBackedList(container, "items", Item.class) → Constructs VirtualList, resolves ClassCacheEntry for Container + Item, extracts ListMapper from the @AerospikeEmbed bin "items"
   2. VirtualList.append(item) → Delegates to VirtualListInteractors
   3. VirtualListInteractors.getAppendOperation() → ListMapper.toAerospikeInstanceFormat(item) converts Item to Aerospike-native format (Map/List depending on EmbedType)
   4. VirtualListInteractors → Builds CDT operation: MapOperation.put(binName, key, value) for MAP embed, or ListOperation.append(binName, value) for LIST embed
   5. AerospikeClient.operate(writePolicy, key, operation) → Sends CDT operation to server, atomically appends to the bin without reading the full list
   6. Aerospike Server → Appends element server-side, returns updated bin size
   7. VirtualList → Returns size (long); the in-memory container object is NOT updated

VirtualList query path (e.g. getByKeyRange):

  list.getByKeyRange(100, 450)

   1. VirtualList.getByKeyRange(start, end) → Sets return type, delegates to VirtualListInteractors
   2. VirtualListInteractors → Creates Interactor wrapping a DeferredOperation
   3. DeferredOperation.getOperation() → Translates keys via ClassCacheEntry.translateKeyToAerospikeKey(), builds MapOperation.getByKeyRange(binName, startValue, endValue, returnType)
   4. AerospikeClient.operate() → Sends CDT query to server
   5. Aerospike Server → Evaluates range server-side, returns matching entries
   6. Interactor.getResult() → Chains ResultsUnpackers (ArrayUnpacker iterates results, calls ListMapper.fromAerospikeInstanceFormat() per element)
   7. MappingConverter.resolveDependencies() → Resolves any nested @AerospikeReference objects
   8. VirtualList → Returns List<Item> of matched elements
```

### Key classes and their roles

- **`AeroMapper` / `ReactiveAeroMapper`** — Public API entry points (sync returns objects, reactive returns `Mono`/`Flux`). Both are instantiated via their inner `Builder` class, never directly.
- **`AbstractBuilder<T>`** — Shared builder logic: register custom type converters, preload classes, set policies, load YAML configuration.
- **`ClassCacheEntry<T>`** — Parses annotations on a mapped class and caches the metadata (namespace, set, key field, bin names, policies). Lazily constructed. Used by the mapper to serialize/deserialize objects.
- **`ClassCache`** — Singleton cache of `ClassCacheEntry` instances.
- **`MappingConverter`** — Orchestrates type conversion during serialization/deserialization using registered `TypeMapper` instances.
- **`TypeMapper`** — Abstract base for custom type converters. Override `toAerospikeFormat()` and `fromAerospikeFormat()`. Register via `builder.addConverter()`.

### Annotation system (`com.aerospike.mapper.annotations`)

- `@AerospikeRecord` — Marks a class as mappable; defines namespace, set, TTL.
- `@AerospikeKey` — Designates the primary key field.
- `@AerospikeBin` — Customizes bin (column) name for a field.
- `@AerospikeEmbed` / `@AerospikeReference` — Relationship mapping (embedded vs. referenced sub-objects).
- `@AerospikeExclude` — Excludes a field from mapping.
- `@ToAerospike` / `@FromAerospike` — Custom per-field conversion methods.
- `@AerospikeVersion` / `@AerospikeGeneration` — Optimistic concurrency control.
- `@AerospikeConstructor` / `@ParamFrom` — Controls object construction from records.

### Type mappers (`tools/mappers/`)

Built-in mappers for Java primitives, `Date`, `Instant`, `LocalDate`, `LocalDateTime`, `BigDecimal`, `BigInteger`, 
enums, arrays, lists, and maps. Custom mappers extend `TypeMapper`.

### Virtual Lists (`tools/virtuallist/`)

Aerospike CDT-backed lists that support lazy loading and server-side operations without retrieving entire collections. 
Both sync (`VirtualList`) and reactive (`ReactiveVirtualList`) variants exist.

### Configuration (`tools/configuration/`)

Alternative to annotations: YAML-based configuration for class mappings via `builder.withConfiguration()`.

## Conventions

- **Package root**: `com.aerospike.mapper` — annotations, exceptions, and `tools` sub-packages.
- **Lombok**: Used in production code (provided scope). Ensure IDE/build has Lombok support.
- **Test base classes**: Sync tests extend `AeroMapperBaseTest`; reactive tests extend `ReactiveAeroMapperBaseTest`. Both handle client lifecycle and provide a `compare()` helper that uses Jackson for JSON-based object comparison.
- **Test setup**: Each test method gets a fresh `AeroMapper` via `Builder` and clears `ClassCache` in `@BeforeEach`. Tables are truncated before tests to ensure isolation.
