package com.aerospike.mapper.tools;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Record;
import com.aerospike.client.cdt.MapOrder;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeExclude;
import com.aerospike.mapper.annotations.AerospikeGetter;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeOrdinal;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeSetter;
import com.aerospike.mapper.tools.TypeUtils.AnnotatedType;
import com.aerospike.mapper.tools.configuration.BinConfig;
import com.aerospike.mapper.tools.configuration.ClassConfig;
import com.aerospike.mapper.tools.configuration.KeyConfig;

public class ClassCacheEntry {
	
	public static final String VERSION_PREFIX = "@V";
	
	private String namespace;
	private String setName;
	private int ttl = 0;
	private boolean mapAll = true;
	private boolean sendKey = false;
	private boolean durableDelete = false;
	private int version = 1;

	private final Class<?> clazz;
	private ValueType key;
	private String keyName = null;
	private final TreeMap<String, ValueType> values = new TreeMap<>();
	private final ClassCacheEntry superClazz;
	private final int binCount;
	private final AeroMapper mapper;
	private Map<Integer, String> ordinals = null;
	private Set<String> fieldsWithOrdinals = null;
	private final ClassConfig classConfig;
	private final Policy readPolicy;
	private final WritePolicy writePolicy;
	
	// package visibility only.
	ClassCacheEntry(@NotNull Class<?> clazz, AeroMapper mapper, ClassConfig config, @NotNull Policy readPolicy, @NotNull WritePolicy writePolicy) {
		this.clazz = clazz;
		this.mapper = mapper;
		this.classConfig = config;
		this.readPolicy = readPolicy;
		this.writePolicy = writePolicy;

		AerospikeRecord recordDescription = clazz.getAnnotation(AerospikeRecord.class);
		if (recordDescription == null && config == null) {
			throw new IllegalArgumentException("Class " + clazz.getName() + " is not augmented by the @AerospikeRecord annotation");
		}
		else if (recordDescription != null) {
			this.namespace = ParserUtils.getInstance().get(recordDescription.namespace());
			this.setName = ParserUtils.getInstance().get(recordDescription.set());
			this.ttl = recordDescription.ttl();
			this.mapAll = recordDescription.mapAll();
			this.version = recordDescription.version();
			this.sendKey = recordDescription.sendKey();
			this.durableDelete = recordDescription.durableDelete();
		}
		
		if (config != null) {
			config.validate();
			this.overrideSettings(config);
		}
		
		this.loadFieldsFromClass(clazz, this.mapAll, config);
		this.loadPropertiesFromClass(clazz, config);
		this.superClazz = ClassCache.getInstance().loadClass(this.clazz.getSuperclass(), this.mapper);
		this.binCount = this.values.size() + (superClazz != null ? superClazz.binCount : 0);
		if (this.binCount == 0) {
			throw new AerospikeException("Class " + clazz.getSimpleName() + " has no values defined to be stored in the database");
		}
		this.formOrdinalsFromValues();
	}
	
	public Policy getReadPolicy() {
		return readPolicy;
	}
	public WritePolicy getWritePolicy() {
		return writePolicy;
	}
	
	public Class<?> getUnderlyingClass() {
		return this.clazz;
	}
	
	public ClassConfig getClassConfig() {
		return this.classConfig;
	}
	
	private void overrideSettings(ClassConfig config) {
		if (!StringUtils.isBlank(config.getNamespace())) {
			this.namespace = config.getNamespace();
		}
		if (!StringUtils.isBlank(config.getSet())) {
			this.setName = config.getSet();
		}
		if (config.getTtl() != null) {
			this.ttl = config.getTtl();
		}
		if (config.getVersion() != null) {
			this.version = config.getVersion();
		}
		if (config.getDurableDelete() != null) {
			this.durableDelete = config.getDurableDelete();
		}
		if (config.getMapAll() != null) {
			this.mapAll = config.getMapAll();
		}
		if (config.getSendKey() != null) {
			this.mapAll = config.getSendKey();
		}
	}
	
	private BinConfig getBinFromName(String name) {
		if (this.classConfig == null || this.classConfig.getBins() == null) {
			return null;
		}
		for (BinConfig thisBin: this.classConfig.getBins()) {
			if (thisBin.getDerivedName().equals(name)) {
				return thisBin;
			}
		}
		return null;
	}
	
	private BinConfig getBinFromField(Field field) {
		if (this.classConfig == null || this.classConfig.getBins() == null) {
			return null;
		}
		for (BinConfig thisBin: this.classConfig.getBins()) {
			if (thisBin.getField() != null && thisBin.getField().equals(field.getName())) {
				return thisBin;
			}
		}
		return null;
	}
	
	private BinConfig getBinFromGetter(String name) {
		if (this.classConfig == null || this.classConfig.getBins() == null) {
			return null;
		}
		for (BinConfig thisBin: this.classConfig.getBins()) {
			if (thisBin.getGetter() != null && thisBin.getGetter().equals(name)) {
				return thisBin;
			}
		}
		return null;
	}
	
	private BinConfig getBinFromSetter(String name) {
		if (this.classConfig == null || this.classConfig.getBins() == null) {
			return null;
		}
		for (BinConfig thisBin: this.classConfig.getBins()) {
			if (thisBin.getSetter() != null && thisBin.getSetter().equals(name)) {
				return thisBin;
			}
		}
		return null;
	}
	
	
	private void formOrdinalsFromValues() {
		for (String thisValueName : this.values.keySet()) {
			ValueType thisValue = this.values.get(thisValueName);
			
			BinConfig binConfig = getBinFromName(thisValueName);
			Integer ordinal = binConfig == null ? null : binConfig.getOrdinal();

			if (ordinal == null) {
				for (Annotation thisAnnotation : thisValue.getAnnotations()) {
					if (thisAnnotation instanceof AerospikeOrdinal) {
						ordinal = ((AerospikeOrdinal)thisAnnotation).value();
					}
					break;
				}
			}
			if (ordinal != null) {
				if (ordinals == null) {
					ordinals = new HashMap<>();
					fieldsWithOrdinals = new HashSet<>();
				}
				if (ordinals.containsKey(ordinal)) {
					throw new AerospikeException(String.format("Class %s has multiple values with the ordinal of %d", clazz.getSimpleName(), ordinal));
				}
				ordinals.put(ordinal, thisValueName);
				fieldsWithOrdinals.add(thisValueName);
			}
		}
		
		if (ordinals != null) {
			// The ordinals need to be valued from 1..<numOrdinals>
			for (int i = 1; i <= ordinals.size(); i++) {
				if (!ordinals.containsKey(i)) {
					throw new AerospikeException(String.format("Class %s has %d values specifying ordinals. These should be 1..%d, but %d is missing",
							clazz.getSimpleName(), ordinals.size(), ordinals.size(), i));
				}
			}
		}
	}
	
	private PropertyDefinition getOrCreateProperty(String name, Map<String, PropertyDefinition> properties) {
		PropertyDefinition thisProperty = properties.get(name);
		if (thisProperty == null) {
			thisProperty = new PropertyDefinition(name, mapper);
			properties.put(name,thisProperty);
		}
		return thisProperty;
	}
	
	private void loadPropertiesFromClass(@NotNull Class<?> clazz, ClassConfig config) {
		Map<String, PropertyDefinition> properties = new HashMap<>();
		PropertyDefinition keyProperty = null;
		KeyConfig keyConfig = config != null ? config.getKey() : null;
		for (Method thisMethod : clazz.getDeclaredMethods()) {
			
			String methodName = thisMethod.getName();
			BinConfig getterConfig = getBinFromGetter(methodName);
			BinConfig setterConfig = getBinFromSetter(methodName);
			
			boolean isKey = false;
			boolean isKeyViaConfig = keyConfig != null && (keyConfig.isGetter(methodName) || keyConfig.isSetter(methodName));
			if (thisMethod.isAnnotationPresent(AerospikeKey.class) || isKeyViaConfig) {

				if (keyProperty == null) {
					keyProperty = new PropertyDefinition("_key_", mapper);
				}
				if (isKeyViaConfig) {
					if (keyConfig.isGetter(methodName)) {
						keyProperty.setGetter(thisMethod);
					}
					else {
						keyProperty.setSetter(thisMethod);
					}
				}
				else {
					AerospikeKey key = thisMethod.getAnnotation(AerospikeKey.class);
					if (key.setter()) {
						keyProperty.setSetter(thisMethod);
					}
					else {
						keyProperty.setGetter(thisMethod);
					}
				}
				isKey = true;
			}
			
			if (thisMethod.isAnnotationPresent(AerospikeGetter.class) || getterConfig != null) {
				String getterName = (getterConfig != null) ? getterConfig.getGetter() : thisMethod.getAnnotation(AerospikeGetter.class).name();

				String name = ParserUtils.getInstance().get(ParserUtils.getInstance().get(getterName));
				PropertyDefinition thisProperty = getOrCreateProperty(name, properties);
				thisProperty.setGetter(thisMethod);
				if (isKey) {
					keyName = name;
				}
			}
			
			if (thisMethod.isAnnotationPresent(AerospikeSetter.class) || setterConfig != null) {
				String setterName = (setterConfig != null) ? setterConfig.getSetter() : thisMethod.getAnnotation(AerospikeSetter.class).name();
				PropertyDefinition thisProperty = getOrCreateProperty(ParserUtils.getInstance().get(ParserUtils.getInstance().get(setterName)), properties);
				thisProperty.setSetter(thisMethod);
			}
		}
		
		if (keyProperty != null) {
			keyProperty.validate(clazz.getName(), config, true);
			if (key != null) {
				throw new AerospikeException("Class " + clazz.getName() + " cannot have a more than one key");
			}
			TypeMapper typeMapper = TypeUtils.getMapper(keyProperty.getType(), new AnnotatedType(config, keyProperty.getGetter()), this.mapper);
			this.key = new ValueType.MethodValue(keyProperty, typeMapper);
		}
		for (String thisPropertyName : properties.keySet()) {
			PropertyDefinition thisProperty = properties.get(thisPropertyName);
			thisProperty.validate(clazz.getName(), config, false);
			if (this.values.get(thisPropertyName) != null) {
				throw new AerospikeException("Class " + clazz.getName() + " cannot define the mapped name " + thisPropertyName + " more than once");
			}
			TypeMapper typeMapper = TypeUtils.getMapper(thisProperty.getType(), new AnnotatedType(config, thisProperty.getGetter()), this.mapper);
			ValueType value = new ValueType.MethodValue(thisProperty, typeMapper);
			values.put(thisPropertyName, value);
		}
	}

	private void loadFieldsFromClass(Class<?> clazz, boolean mapAll, ClassConfig config) {
		KeyConfig keyConfig = config != null ? config.getKey() : null;
		String keyField = keyConfig == null ? null : keyConfig.getField();
		for (Field thisField : clazz.getDeclaredFields()) {
			boolean isKey = false;
			BinConfig thisBin = getBinFromField(thisField);
			if (thisField.isAnnotationPresent(AerospikeKey.class) || (!StringUtils.isBlank(keyField) && keyField.equals(thisField.getName()))) {
				if (thisField.isAnnotationPresent(AerospikeExclude.class) || (thisBin != null && thisBin.isExcluded())) {
					throw new AerospikeException("Class " + clazz.getName() + " cannot have a field which is both a key and excluded.");
				}
				if (key != null) {
					throw new AerospikeException("Class " + clazz.getName() + " cannot have a more than one key");
				}
				TypeMapper typeMapper = TypeUtils.getMapper(thisField.getType(), new AnnotatedType(config, thisField), this.mapper);
				this.key = new ValueType.FieldValue(thisField, typeMapper);
				isKey = true;
			}

			if (thisField.isAnnotationPresent(AerospikeExclude.class) || (thisBin != null && thisBin.isExcluded() != null && thisBin.isExcluded().booleanValue())) {
				// This field should be excluded from being stored in the database. Even keys must be stored
				continue;
			}
			
			if (this.mapAll || thisField.isAnnotationPresent(AerospikeBin.class) || thisBin != null) {
				// This field needs to be mapped
				thisField.setAccessible(true);
				AerospikeBin bin = thisField.getAnnotation(AerospikeBin.class);
				String binName = bin == null ? null : ParserUtils.getInstance().get(bin.name());
				if (thisBin != null && !StringUtils.isBlank(thisBin.getDerivedName())) {
					binName = thisBin.getDerivedName();
				}
				String name;
				if (StringUtils.isBlank(binName)) {
					name = thisField.getName();
				}
				else {
					name = binName;
				}
				if (isKey) {
					this.keyName = name;
				}
				
				if (this.values.get(name) != null) {
					throw new AerospikeException("Class " + clazz.getName() + " cannot define the mapped name " + name + " more than once");
				}
				TypeMapper typeMapper = TypeUtils.getMapper(thisField.getType(), new AnnotatedType(config, thisField), this.mapper);
				ValueType valueType = new ValueType.FieldValue(thisField, typeMapper);
				values.put(name, valueType);
			}
		}
	}
	
	public Object translateKeyToAerospikeKey(Object key) {
		return this.key.getTypeMapper().toAerospikeFormat(key);
	}
	
	private Object _getKey(Object object) throws ReflectiveOperationException {
		if (this.key != null) {
			return this.translateKeyToAerospikeKey(this.key.get(object));
		}
		else if (superClazz != null) {
			return this.superClazz._getKey(object);
		}
		return null;
	}
	
	public Object getKey(Object object) {
		try {
			Object key = this._getKey(object);
			if (key == null) {
	    		throw new AerospikeException("Null key from annotated object of class " + this.clazz.getSimpleName() + ". Did you forget an @AerospikeKey annotation?");
			}
			return key;
		}
		catch (ReflectiveOperationException re) {
			throw new AerospikeException(re);
		}
	}
	
	private void _setKey(Object object, Object value) throws ReflectiveOperationException {
		if (this.key != null) {
			this.key.set(object, this.key.getTypeMapper().fromAerospikeFormat(value));
		}
		else if (superClazz != null) {
			this.superClazz._setKey(object, value);
		}
	}
	
	public void setKey(Object object, Object value) {
		try {
			this._setKey(object, value);
		}
		catch (ReflectiveOperationException re) {
			throw new AerospikeException(re);
		}
	}
	
	public String getNamespace() {
		return namespace;
	}
	
	public String getSetName() {
		return setName;
	}
	
	public int getTtl() {
		return ttl;
	}
	
	public boolean getSendKey() {
		return sendKey;
	}
	
	public boolean getDurableDelete() {
		return durableDelete;
	}
	
	private boolean contains(String[] names, String thisName) {
		if (names == null || names.length == 0) {
			return true;
		}
		if (thisName == null) {
			return false;
		}
		for (String aName : names) {
			if (thisName.equals(aName)) {
				return true;
			}
		}
		return false;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Bin[] getBins(Object instance, boolean allowNullBins, String[] binNames) {
		try {
			Bin[] bins = new Bin[this.binCount];
			int index = 0;
			ClassCacheEntry thisClass = this;
			while (thisClass != null) {
				for (String name : this.values.keySet()) {
					if (contains(binNames, name)) {
						ValueType value = this.values.get(name);
						Object javaValue = value.get(instance);
						Object aerospikeValue = value.getTypeMapper().toAerospikeFormat(javaValue);
						if (aerospikeValue != null || allowNullBins) {
							if (aerospikeValue != null && aerospikeValue instanceof TreeMap<?, ?>) {
								TreeMap<?,?> treeMap = (TreeMap<?,?>)aerospikeValue;
								bins[index++] = new Bin(name, new ArrayList(treeMap.entrySet()), MapOrder.KEY_ORDERED);
							}
							else {
								bins[index++] = new Bin(name, aerospikeValue);
							}
						}
					}
				}
				thisClass = thisClass.superClazz;
			}
			if (index != this.binCount) {
				bins = Arrays.copyOf(bins, index);
			}
			return bins;
		}
		catch (ReflectiveOperationException ref) {
			throw new AerospikeException(ref);
		}
	}
	
	public Map<String, Object> getMap(Object instance) {
		try {
			Map<String, Object> results = new HashMap<>();
			ClassCacheEntry thisClass = this;
			while (thisClass != null) {
				for (String name : this.values.keySet()) {
					ValueType value = this.values.get(name);
					Object javaValue = value.get(instance);
					Object aerospikeValue = value.getTypeMapper().toAerospikeFormat(javaValue);
					results.put(name, aerospikeValue);
				}
				thisClass = thisClass.superClazz;
			}
			return results;
		}
		catch (ReflectiveOperationException ref) {
			throw new AerospikeException(ref);
		}
	}
	
	private void addDataFromValueName(String name, Object instance, ClassCacheEntry thisClass, List<Object> results) throws ReflectiveOperationException {
		ValueType value = this.values.get(name);
		if (value.getMinimumVersion() <= thisClass.version && thisClass.version <= value.getMaximumVersion()) {
			Object javaValue = value.get(instance);
			Object aerospikeValue = value.getTypeMapper().toAerospikeFormat(javaValue);
			results.add(aerospikeValue);
		}
	}
	
	private boolean isKeyField(String name) {
		return name != null && keyName != null && keyName.equals(name);
	}
	
	public List<Object> getList(Object instance, boolean skipKey) {
		try {
			List<Object> results = new ArrayList<>();
			List<Object> versionsToAdd = new ArrayList<>();
			ClassCacheEntry thisClass = this;
			while (thisClass != null) {
				if (thisClass.version > 1) {
					versionsToAdd.add(0, VERSION_PREFIX + thisClass.version);
				}
				if (ordinals != null) {
					for (int i = 1; i <= ordinals.size(); i++) {
						String name = ordinals.get(i);
						if (!skipKey || !isKeyField(name)) {
							addDataFromValueName(name, instance, thisClass, results);
						}
					}
				}
				for (String name : this.values.keySet()) {
					if (fieldsWithOrdinals == null || !fieldsWithOrdinals.contains(name)) {
						if (!skipKey || !isKeyField(name)) {
							addDataFromValueName(name, instance, thisClass, results);
						}
					}
				}
				thisClass = thisClass.superClazz;
			}
			results.addAll(versionsToAdd);
			return results;
		}
		catch (ReflectiveOperationException ref) {
			throw new AerospikeException(ref);
		}
	}
	
	public void hydrateFromRecord(Record record, Object instance) {
		this.hydrateFromRecordOrMap(record, null, instance);
	}

	public void hydrateFromMap(Map<String, Object> map, Object instance) {
		this.hydrateFromRecordOrMap(null, map, instance);
	}
	
	private void hydrateFromRecordOrMap(Record record, Map<String, Object> map, Object instance) {
		try {
			ClassCacheEntry thisClass = this;
			while (thisClass != null) {
				for (String name : this.values.keySet()) {
					ValueType value = this.values.get(name);
					Object aerospikeValue = record == null? map.get(name) : record.getValue(name);
					value.set(instance, value.getTypeMapper().fromAerospikeFormat(aerospikeValue));
				}
				thisClass = thisClass.superClazz;
			}
		}
		catch (ReflectiveOperationException ref) {
			throw new AerospikeException(ref);
		}
	}
	
	private int setValueByField(String name, int objectVersion, int recordVersion, Object instance, int index, List<Object> list) throws ReflectiveOperationException {
		ValueType value = this.values.get(name);
		TypeMapper typeMapper = value.getTypeMapper();
		// If the version of this value does not exist on this object, simply skip it. For example, V1 contains {a,b,c} but V2 contains {a,c}, skip field B
		if (!(value.getMinimumVersion() <= objectVersion && objectVersion <= value.getMaximumVersion())) {
			// If the version of this record in the database also contained this value, skip over the value as well as the field
			if (value.getMinimumVersion() <= recordVersion && recordVersion <= value.getMaximumVersion()) {
				index++;
			}
			return index;
		}
		// Otherwise only map the value if it should exist on the record in the database. 
		if (value.getMinimumVersion() <= recordVersion && recordVersion <= value.getMaximumVersion() && index < list.size()) {
			Object aerospikeValue = list.get(index++);
			Object javaValue = aerospikeValue == null ? null : typeMapper.fromAerospikeFormat(aerospikeValue);
			value.set(instance, javaValue);
		}
		return index;
	}
	
	public void hydrateFromList(List<Object> list, Object instance) {
		this.hydrateFromList(list, instance, false);
	}
	
	public void hydrateFromList(List<Object> list, Object instance, boolean skipKey) {
		try {
			int index = 0;
			int endIndex = list.size();
			ClassCacheEntry thisClass = this;
			while (thisClass != null) {
				if (index < endIndex) {
					Object lastValue = list.get(endIndex-1);
					int recordVersion = 1;
					if ((lastValue instanceof String) && (((String)lastValue).startsWith(VERSION_PREFIX))) {
						recordVersion = Integer.valueOf(((String)lastValue).substring(2));
						endIndex--;
					}
					int objectVersion = thisClass.version;
					if (ordinals != null) {
						for (int i = 1; i <= ordinals.size(); i++) {
							String name = ordinals.get(i);
							if (!skipKey || !isKeyField(name)) {
								index = setValueByField(name, objectVersion, recordVersion, instance, index, list);
							}
						}
					}
					for (String name : this.values.keySet()) {
						if (this.fieldsWithOrdinals == null || !thisClass.fieldsWithOrdinals.contains(name)) {
							if (!skipKey || !isKeyField(name)) {
								index = setValueByField(name, objectVersion, recordVersion, instance, index, list);
							}
						}
					}
					thisClass = thisClass.superClazz;
				}
			}
		}
		catch (ReflectiveOperationException ref) {
			throw new AerospikeException(ref);
		}
	}

}
