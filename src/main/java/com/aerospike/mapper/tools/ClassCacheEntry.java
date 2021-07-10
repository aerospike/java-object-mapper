package com.aerospike.mapper.tools;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Record;
import com.aerospike.client.cdt.MapOrder;
import com.aerospike.client.policy.*;
import com.aerospike.mapper.annotations.*;
import com.aerospike.mapper.tools.utils.ParserUtils;
import com.aerospike.mapper.tools.utils.TypeUtils;
import com.aerospike.mapper.tools.utils.TypeUtils.AnnotatedType;
import com.aerospike.mapper.tools.configuration.BinConfig;
import com.aerospike.mapper.tools.configuration.ClassConfig;
import com.aerospike.mapper.tools.configuration.KeyConfig;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class ClassCacheEntry<T> {
	
	public static final String VERSION_PREFIX = "@V";
	public static final String TYPE_PREFIX = "@T:";
	public static final String TYPE_NAME = ".type";
	
	private String namespace;
	private String setName;
	private Integer ttl = null;
	private boolean mapAll = true;
	private Boolean sendKey = null;
	private Boolean durableDelete = null;
	private int version = 1;

	private final Class<T> clazz;
	private ValueType key;
	private String keyName = null;
	private final TreeMap<String, ValueType> values = new TreeMap<>();
	private ClassCacheEntry<?> superClazz;
	private int binCount;
	private final IBaseAeroMapper mapper;
	private Map<Integer, String> ordinals = null;
	private Set<String> fieldsWithOrdinals = null;
	private final ClassConfig classConfig;
	private final Policy readPolicy;
	private final WritePolicy writePolicy;
	private final BatchPolicy batchPolicy;
	private final QueryPolicy queryPolicy;
	private final ScanPolicy scanPolicy;
	private String[] constructorParamBins;
	private Object[] constructorParamDefaults;
	private Constructor<T> constructor;
	private final ClassConfig config;
	
	/** 
	 * When there are subclasses, we need to store the type information to be able to re-create an instance of the same type. As the
	 * class name can be verbose, we provide the ability to set a string representing the class name. This string must be unique for all classes.
	 */
	private String shortenedClassName;
	private boolean isChildClass = false;
	
	
	// package visibility only.
	ClassCacheEntry(@NotNull Class<T> clazz, IBaseAeroMapper mapper, ClassConfig config,
			@NotNull Policy readPolicy, @NotNull WritePolicy writePolicy,
			@NotNull BatchPolicy batchPolicy, @NotNull QueryPolicy queryPolicy,
			@NotNull ScanPolicy scanPolicy) {
		this.clazz = clazz;
		this.mapper = mapper;
		this.classConfig = config;
		this.readPolicy = readPolicy;
		this.writePolicy = writePolicy;
		this.batchPolicy = batchPolicy;
		this.scanPolicy = scanPolicy;
		this.queryPolicy = queryPolicy;

		AerospikeRecord recordDescription = clazz.getAnnotation(AerospikeRecord.class);
		if (recordDescription == null && config == null) {
			throw new AerospikeException("Class " + clazz.getName() + " is not augmented by the @AerospikeRecord annotation");
		}
		else if (recordDescription != null) {
			this.namespace = ParserUtils.getInstance().get(recordDescription.namespace());
			this.setName = ParserUtils.getInstance().get(recordDescription.set());
			this.ttl = recordDescription.ttl();
			this.mapAll = recordDescription.mapAll();
			this.version = recordDescription.version();
			this.sendKey = recordDescription.sendKey();
			this.durableDelete = recordDescription.durableDelete();
			this.shortenedClassName = recordDescription.shortName();
		}
		this.config = config;
	}
	
	public ClassCacheEntry<T> construct() {
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
		this.findConstructor();
		if (StringUtils.isBlank(this.shortenedClassName)) {
			this.shortenedClassName = clazz.getSimpleName();
		}
		ClassCache.getInstance().setStoredName(this, this.shortenedClassName);
		
		this.checkRecordSettingsAgainstSuperClasses();
		return this;
	}
	public Policy getReadPolicy() {
		return readPolicy;
	}
	public WritePolicy getWritePolicy() {
		return writePolicy;
	}
	public BatchPolicy getBatchPolicy() {
		return batchPolicy;
	}
	public QueryPolicy getQueryPolicy() {
		return queryPolicy;
	}
	public ScanPolicy getScanPolicy() {
		return scanPolicy;
	}
	
	public Class<?> getUnderlyingClass() {
		return this.clazz;
	}
	
	public ClassConfig getClassConfig() {
		return this.classConfig;
	}
	
	public String getShortenedClassName() {
		return this.shortenedClassName;
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
			this.sendKey = config.getSendKey();
		}
		if (config.getShortName() != null) {
			this.shortenedClassName = config.getShortName();
		}
	}
	
	public boolean isChildClass() {
		return isChildClass;
	}
	
	private void checkRecordSettingsAgainstSuperClasses() {
		if (!StringUtils.isBlank(this.namespace) && !StringUtils.isBlank(this.setName)) {
			// This class defines it's own namespace + set, it is only a child class if it's closest named superclass is the same as ours.
			this.isChildClass = false;
			ClassCacheEntry<?> thisEntry = this.superClazz;
			while (thisEntry != null) {
				if ((!StringUtils.isBlank(thisEntry.getNamespace())) && (!StringUtils.isBlank(thisEntry.getSetName()))) {
					if (this.namespace.equals(thisEntry.getNamespace()) && this.setName.equals(thisEntry.getSetName())) {
						this.isChildClass = true;
					}
					break;
				}
				thisEntry = thisEntry.superClazz;
			}
		}
		else {
			// Otherwise this is a child class, find the set and namespace from the closest highest class
			this.isChildClass = true;
			ClassCacheEntry<?> thisEntry = this.superClazz;
			while (thisEntry != null) {
				if ((!StringUtils.isBlank(thisEntry.getNamespace())) && (!StringUtils.isBlank(thisEntry.getSetName()))) {
					this.namespace = thisEntry.getNamespace();
					this.setName = thisEntry.getSetName();
					break;
				}
				thisEntry = thisEntry.superClazz;
			}
		}
		ClassCacheEntry<?> thisEntry = this.superClazz;
		while (thisEntry != null) {
			if (this.durableDelete == null && thisEntry.getDurableDelete() != null) {
				this.durableDelete = thisEntry.getDurableDelete();
			}
			if (this.ttl == null && thisEntry.getTtl() != null) {
				this.ttl = thisEntry.getTtl();
			}
			if (this.sendKey == null && thisEntry.getSendKey() != null) {
				this.sendKey = thisEntry.getSendKey();
			}
			if (this.key == null && thisEntry.key != null) {
				this.key = thisEntry.key;
			}
			thisEntry = thisEntry.superClazz;
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
	
	private void findConstructor() {
		Constructor<?>[] constructors = clazz.getDeclaredConstructors();
		if (constructors.length == 0) {
			throw new AerospikeException("Class " + clazz.getSimpleName() + " has no constructors and hence cannot be mapped to Aerospike");
		}
		Constructor<?> desiredConstructor = null;
		Constructor<?> noArgConstructor = null;
		if (constructors.length == 1) {
			desiredConstructor = constructors[0];
		}
		else {
			for (Constructor<?> thisConstructor : constructors) {
				if (thisConstructor.getParameters().length == 0) {
					noArgConstructor = thisConstructor;
				}
				AerospikeConstructor aerospikeConstructor = thisConstructor.getAnnotation(AerospikeConstructor.class);
				if (aerospikeConstructor != null) {
					if (desiredConstructor != null) {
						throw new AerospikeException("Class " + clazz.getSimpleName() + " has multiple constructors annotated with @AerospikeConstructor. Only one constructor can be so annotated.");
					}
					else {
						desiredConstructor = thisConstructor;
					}
				}
			}
		}
		if (desiredConstructor == null && noArgConstructor != null) {
			constructorParamBins = new String[0];
			desiredConstructor = noArgConstructor;
		}
		
		if (desiredConstructor == null) {
			throw new AerospikeException("Class " + clazz.getSimpleName() + " has neither a no-arg constructor, nor a constructor annotated with @AerospikeConstructor so cannot be mapped to Aerospike.");
		}
		
		Parameter[] params = desiredConstructor.getParameters();
		this.constructorParamBins = new String[params.length];
		this.constructorParamDefaults = new Object[params.length];
			
		Map<String, ValueType> allValues = new HashMap<>();
		ClassCacheEntry<?> current = this;
		while (current != null) {
			allValues.putAll(current.values);
			current = current.superClazz;
		}
		int count = 0;
		
		// Parameters can be either specified by their name (which requires the use of the javac -parameters flag), or through an @ParamFrom annotation.  
		for (Parameter thisParam : params) {
			count++;
			boolean isFromAnnotation = false;
			String binName = thisParam.getName();
			ParamFrom parameterDetails = thisParam.getAnnotation(ParamFrom.class);
			
			if (parameterDetails != null) {
				binName = parameterDetails.value();
				isFromAnnotation = true;
			}
			
			// Validate that we have such a value
			if (!allValues.containsKey(binName)) {
				String valueList = String.join(",", values.keySet());
				String message = String.format("Class %s has a preferred constructor of %s. However, parameter %d is mapped to bin \"%s\" %s which is not one of the values on the class, which are: %s%s", 
						clazz.getSimpleName(), desiredConstructor.toString(), count, binName,
						isFromAnnotation ? "via the @ParamFrom annotation" : "via the argument name",
						valueList,
						(!isFromAnnotation && binName.startsWith("arg")) ? ". Did you forget to specify '-parameters' to javac when building?" : "");
				throw new AerospikeException(message);
			}
			Class<?> type = thisParam.getType();
			if (!type.isAssignableFrom(allValues.get(binName).getType())) {
				throw new AerospikeException("Class " + clazz.getSimpleName() + " has a preferred constructor of " + desiredConstructor.toString()+ ". However, parameter " + count + 
						" is of type " + type + " but assigned from bin \"" + binName + "\" of type " +values.get(binName).getType()+ ". These types are incompatible.");
			}
			constructorParamBins[count-1] = binName;
			constructorParamDefaults[count-1] = PrimitiveDefaults.getDefaultValue(thisParam.getType());
		}
		this.constructor = (Constructor<T>) desiredConstructor;
		this.constructor.setAccessible(true);
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
			AnnotatedType annotatedType = new AnnotatedType(config, keyProperty.getGetter());
			TypeMapper typeMapper = TypeUtils.getMapper(keyProperty.getType(), annotatedType, this.mapper);
			this.key = new ValueType.MethodValue(keyProperty, typeMapper, annotatedType);
		}
		for (String thisPropertyName : properties.keySet()) {
			PropertyDefinition thisProperty = properties.get(thisPropertyName);
			thisProperty.validate(clazz.getName(), config, false);
			if (this.values.get(thisPropertyName) != null) {
				throw new AerospikeException("Class " + clazz.getName() + " cannot define the mapped name " + thisPropertyName + " more than once");
			}
			AnnotatedType annotatedType = new AnnotatedType(config, thisProperty.getGetter());
			TypeMapper typeMapper = TypeUtils.getMapper(thisProperty.getType(), annotatedType, this.mapper);
			ValueType value = new ValueType.MethodValue(thisProperty, typeMapper, annotatedType);
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
				AnnotatedType annotatedType = new AnnotatedType(config, thisField);
				TypeMapper typeMapper = TypeUtils.getMapper(thisField.getType(), annotatedType, this.mapper);
				this.key = new ValueType.FieldValue(thisField, typeMapper, annotatedType);
				isKey = true;
			}

			if (thisField.isAnnotationPresent(AerospikeExclude.class) || (thisBin != null && thisBin.isExcluded() != null && thisBin.isExcluded())) {
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
				AnnotatedType annotatedType = new AnnotatedType(config, thisField);
				TypeMapper typeMapper = TypeUtils.getMapper(thisField.getType(), annotatedType, this.mapper);
				ValueType valueType = new ValueType.FieldValue(thisField, typeMapper, annotatedType);
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
	
	public Integer getTtl() {
		return ttl;
	}
	
	public Boolean getSendKey() {
		return sendKey;
	}
	
	public Boolean getDurableDelete() {
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
				Set<String> keys = thisClass.values.keySet();
				for (String name : keys) {
					if (contains(binNames, name)) {
						ValueType value = (ValueType) thisClass.values.get(name);
						Object javaValue = value.get(instance);
						Object aerospikeValue = value.getTypeMapper().toAerospikeFormat(javaValue);
						if (aerospikeValue != null || allowNullBins) {
							if (aerospikeValue instanceof TreeMap<?, ?>) {
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
	
	public Map<String, Object> getMap(Object instance, boolean needsType) {
		try {
			Map<String, Object> results = new HashMap<>();
			ClassCacheEntry<?> thisClass = this;
			if (needsType) {
				results.put(TYPE_NAME, this.getShortenedClassName());
			}
			while (thisClass != null) {
				for (String name : thisClass.values.keySet()) {
					ValueType value = thisClass.values.get(name);
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
	
	private void addDataFromValueName(String name, Object instance, ClassCacheEntry<?> thisClass, List<Object> results) throws ReflectiveOperationException {
		ValueType value = thisClass.values.get(name);
		if (value.getMinimumVersion() <= thisClass.version && thisClass.version <= value.getMaximumVersion()) {
			Object javaValue = value.get(instance);
			Object aerospikeValue = value.getTypeMapper().toAerospikeFormat(javaValue);
			results.add(aerospikeValue);
		}
	}
	
	private boolean isKeyField(String name) {
		return keyName != null && keyName.equals(name);
	}
	
	public List<Object> getList(Object instance, boolean skipKey, boolean needsType) {
		try {
			List<Object> results = new ArrayList<>();
			List<Object> versionsToAdd = new ArrayList<>();
			ClassCacheEntry<?> thisClass = this;
			while (thisClass != null) {
				if (thisClass.version > 1) {
					versionsToAdd.add(0, VERSION_PREFIX + thisClass.version);
				}
				if (thisClass.ordinals != null) {
					for (int i = 1; i <= thisClass.ordinals.size(); i++) {
						String name = thisClass.ordinals.get(i);
						if (!skipKey || !isKeyField(name)) {
							addDataFromValueName(name, instance, thisClass, results);
						}
					}
				}
				for (String name : thisClass.values.keySet()) {
					if (thisClass.fieldsWithOrdinals == null || !thisClass.fieldsWithOrdinals.contains(name)) {
						if (!skipKey || !isKeyField(name)) {
							addDataFromValueName(name, instance, thisClass, results);
						}
					}
				}
				thisClass = thisClass.superClazz;
			}
			results.addAll(versionsToAdd);
			if (needsType) {
				results.add(TYPE_PREFIX + this.getShortenedClassName());
			}
			return results;
		}
		catch (ReflectiveOperationException ref) {
			throw new AerospikeException(ref);
		}
	}
	
	public T constructAndHydrate(Map<String, Object> map) {
		return constructAndHydrate(null, map);
	}
	public T constructAndHydrate(Record record) {
		return constructAndHydrate(record, null);
	}
	
	private T constructAndHydrate(Record record, Map<String, Object> map) {
		Map<String, Object> valueMap = new HashMap<>();
		try {
			ClassCacheEntry<?> thisClass = this;
			
			// If the object saved in the list was a subclass of the declared type, it must have the type name in the map
			// Note that there is a performance implication of using subclasses.
			String className = map == null ? record.getString(TYPE_NAME) :(String) map.get(TYPE_NAME);
			if (className != null) {
				thisClass = ClassCache.getInstance().getCacheEntryFromStoredName(className);
				if (thisClass == null) {
					Class<?> typeClazz = Class.forName(className);
					thisClass = ClassCache.getInstance().loadClass(typeClazz, this.mapper);
				}
			}
			
			T result = null;
			while (thisClass != null) {
				for (String name : thisClass.values.keySet()) {
					ValueType value = thisClass.values.get(name);
					Object aerospikeValue = record == null? map.get(name) : record.getValue(name);
					valueMap.put(name, value.getTypeMapper().fromAerospikeFormat(aerospikeValue));
				}
				if (result == null) {
					result = (T)thisClass.constructAndHydrateFromJavaMap(valueMap);
				}
				else {
					for (String field : valueMap.keySet()) {
						ValueType value = thisClass.values.get(field);
						value.set(result, valueMap.get(field));
					}
				}
				valueMap.clear();
				thisClass = thisClass.superClazz;
			}
			
			return result;
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
			ClassCacheEntry<?> thisClass = this;
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
	
	private int setValueByField(String name, int objectVersion, int recordVersion, Object instance, int index, List<Object> list, Map<String, Object> map) throws ReflectiveOperationException {
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
			if (instance == null) {
				map.put(name, javaValue);
			}
			else {
				value.set(instance, javaValue);
			}
		}
		return index;
	}
	
	public void hydrateFromList(List<Object> list, Object instance) {
		this.hydrateFromList(list, instance, false);
	}
	
	private T constructAndHydrateFromJavaMap(Map<String, Object> javaValuesMap) throws ReflectiveOperationException {
		// Now form the values which satisfy the constructor
		T result;
		Object[] args = new Object[constructorParamBins.length];
		for (int i = 0; i < constructorParamBins.length; i++) {
			if (javaValuesMap.containsKey(constructorParamBins[i])) {
				args[i] = javaValuesMap.get(constructorParamBins[i]);
			}
			else {
				args[i] = constructorParamDefaults[i];
			}
			javaValuesMap.remove(constructorParamBins[i]);
		}
		result = constructor.newInstance(args);
		// Once the object has been created, we need to store it against the current key so that
		// recursive objects resolve correctly
		LoadedObjectResolver.setObjectForCurrentKey(result);

		for (String field : javaValuesMap.keySet()) {
			ValueType value = this.values.get(field);
			Object object = javaValuesMap.get(field);
			if (object == null && value.getType().isPrimitive()) {
				object = PrimitiveDefaults.getDefaultValue(value.getType());
			}
			value.set(result, object);
		}
		return result;
	}
	
	public T constructAndHydrate(List<Object> list, boolean skipKey) {
		Map<String, Object> valueMap = new HashMap<>();
		try {
			ClassCacheEntry<?> thisClass = this;
			int index = 0;
			int endIndex = list.size();
			if (!list.isEmpty()) {
				// If the object saved in the list was a subclass of the declared type, it must have the type name as the last element of the list.
				// Note that there is a performance implication of using subclasses.
				Object obj = list.get(endIndex-1);
				if ((obj instanceof String) && ((String) obj).startsWith(TYPE_PREFIX)) {
					String className = ((String)obj).substring(TYPE_PREFIX.length());
					thisClass = ClassCache.getInstance().getCacheEntryFromStoredName(className);
					if (thisClass == null) {
						Class<?> typeClazz = Class.forName(className);
						thisClass = ClassCache.getInstance().loadClass(typeClazz, this.mapper);
					}
					endIndex--;
				}
			}

			T result = null;
			while (thisClass != null) {
				if (index < endIndex) {
					Object lastValue = list.get(endIndex-1);
					int recordVersion = 1;
					if ((lastValue instanceof String) && (((String)lastValue).startsWith(VERSION_PREFIX))) {
						recordVersion = Integer.parseInt(((String)lastValue).substring(2));
						endIndex--;
					}
					int objectVersion = thisClass.version;
					if (thisClass.ordinals != null) {
						for (int i = 1; i <= thisClass.ordinals.size(); i++) {
							String name = thisClass.ordinals.get(i);
							if (!skipKey || !isKeyField(name)) {
								index = thisClass.setValueByField(name, objectVersion, recordVersion, null, index, list, valueMap);
							}
						}
					}
					for (String name : thisClass.values.keySet()) {
						if (thisClass.fieldsWithOrdinals == null || !thisClass.fieldsWithOrdinals.contains(name)) {
							if (!skipKey || !isKeyField(name)) {
								index = thisClass.setValueByField(name, objectVersion, recordVersion, null, index, list, valueMap);
							}
						}
					}
					if (result == null) {
						result = (T)thisClass.constructAndHydrateFromJavaMap(valueMap);
					}
					else {
						for (String field : valueMap.keySet()) {
							ValueType value = this.values.get(field);
							value.set(result, valueMap.get(field));
						}
					}
					valueMap.clear();
					thisClass = thisClass.superClazz;
				}
			}
			return result;
		}
		catch (ReflectiveOperationException ref) {
			throw new AerospikeException(ref);
		}
	}

	public void hydrateFromList(List<Object> list, Object instance, boolean skipKey) {
		try {
			int index = 0;
			int endIndex = list.size();
			ClassCacheEntry<?> thisClass = this;
			while (thisClass != null) {
				if (index < endIndex) {
					Object lastValue = list.get(endIndex-1);
					int recordVersion = 1;
					if ((lastValue instanceof String) && (((String)lastValue).startsWith(VERSION_PREFIX))) {
						recordVersion = Integer.parseInt(((String)lastValue).substring(2));
						endIndex--;
					}
					int objectVersion = thisClass.version;
					if (ordinals != null) {
						for (int i = 1; i <= ordinals.size(); i++) {
							String name = ordinals.get(i);
							if (!skipKey || !isKeyField(name)) {
								index = setValueByField(name, objectVersion, recordVersion, instance, index, list, null);
							}
						}
					}
					for (String name : this.values.keySet()) {
						if (this.fieldsWithOrdinals == null || !thisClass.fieldsWithOrdinals.contains(name)) {
							if (!skipKey || !isKeyField(name)) {
								index = setValueByField(name, objectVersion, recordVersion, instance, index, list, null);
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

	public ValueType getValueFromBinName(String name) {
		return this.values.get(name);
	}
	
	@Override
	public String toString() {
		return String.format("ClassCacheEntry<%s> (ns=%s,set=%s,subclass=%b,shortName=%s)", this.getUnderlyingClass().getSimpleName(), this.namespace, this.setName, this.isChildClass, this.shortenedClassName);
	}

}
