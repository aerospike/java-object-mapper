package com.aerospike.mapper.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Log;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeReference;
import com.aerospike.mapper.tools.ClassCache.PolicyType;
import com.aerospike.mapper.tools.configuration.BinConfig;
import com.aerospike.mapper.tools.configuration.ClassConfig;
import com.aerospike.mapper.tools.configuration.Configuration;
import com.aerospike.mapper.tools.configuration.EmbedConfig;
import com.aerospike.mapper.tools.configuration.KeyConfig;
import com.aerospike.mapper.tools.configuration.ReferenceConfig;
import com.aerospike.mapper.tools.utils.TypeUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public abstract class AbstractBuilder<T extends IBaseAeroMapper> {
    private final T mapper;
    private List<Class<?>> classesToPreload = null;

    protected AbstractBuilder(T mapper) {
        this.mapper = mapper;
    }
    /**
     * Add in a custom type converter. The converter must have methods which implement the ToAerospike and FromAerospike annotation.
     *
     * @param converter The custom converter
     * @return this object
     */
    public AbstractBuilder<T> addConverter(Object converter) {
        GenericTypeMapper mapper = new GenericTypeMapper(converter);
        TypeUtils.addTypeMapper(mapper.getMappedClass(), mapper);

        return this;
    }

    public AbstractBuilder<T> preLoadClass(Class<?> clazz) {
        if (classesToPreload == null) {
            classesToPreload = new ArrayList<>();
        }
        classesToPreload.add(clazz);
        return this;
    }

    public AbstractBuilder<T> withConfigurationFile(File file) throws IOException {
        return this.withConfigurationFile(file, false);
    }

    public AbstractBuilder<T> withConfigurationFile(File file, boolean allowsInvalid) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        Configuration configuration = objectMapper.readValue(file, Configuration.class);
        this.loadConfiguration(configuration, allowsInvalid);
        return this;
    }

    public AbstractBuilder<T> withConfigurationFile(InputStream ios) throws IOException {
        return this.withConfigurationFile(ios, false);
    }

    public AbstractBuilder<T> withConfigurationFile(InputStream ios, boolean allowsInvalid) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        Configuration configuration = objectMapper.readValue(ios, Configuration.class);
        this.loadConfiguration(configuration, allowsInvalid);
        return this;
    }

    public AbstractBuilder<T> withConfiguration(String configurationYaml) throws JsonProcessingException {
        return this.withConfiguration(configurationYaml, false);
    }

    public AbstractBuilder<T> withConfiguration(String configurationYaml, boolean allowsInvalid) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        Configuration configuration = objectMapper.readValue(configurationYaml, Configuration.class);
        this.loadConfiguration(configuration, allowsInvalid);
        return this;
    }

    private void loadConfiguration(@NotNull Configuration configuration, boolean allowsInvalid) {
        for (ClassConfig config : configuration.getClasses()) {
            try {
                String name = config.getClassName();
                if (StringUtils.isBlank(name)) {
                    throw new AerospikeException("Class with blank name in configuration file");
                } else {
                    try {
                        Class.forName(config.getClassName());
                    } catch (ClassNotFoundException e) {
                        throw new AerospikeException("Cannot find a class with name " + name);
                    }
                }
            } catch (RuntimeException re) {
                if (allowsInvalid) {
                    Log.warn("Ignoring issue with configuration: " + re.getMessage());
                } else {
                    throw re;
                }
            }
        }
        ClassCache.getInstance().addConfiguration(configuration);
    }

    public AeroConfigMapper<T> withConfigurationForClass(Class<?> clazz) {
        return new AeroConfigMapper<T>(this, clazz);
    }
    
    public static class AeroConfigMapper<T extends IBaseAeroMapper> {
        private final AbstractBuilder<T> builder;
        private final ClassConfig classConfig;
        private final Class<?> clazz;
        
        public AeroConfigMapper(AbstractBuilder<T> builder, Class<?> clazz) {
            this.builder = builder;
            this.clazz = clazz;
            this.classConfig = new ClassConfig();
            this.classConfig.setClassName(clazz.getName());
        }
        
        public AeroConfigMapper<T> withNamespace(String namespace) {
            this.classConfig.setNamespace(namespace);
            return this;
        }
        public AeroConfigMapper<T> withSet(String setName) {
            this.classConfig.setSet(setName);
            return this;
        }
        public AeroConfigMapper<T> withTtl(int ttl) {
            this.classConfig.setTtl(ttl);
            return this;
        }
        public AeroConfigMapper<T> withVersion(int version) {
            this.classConfig.setVersion(version);
            return this;
        }
        public AeroConfigMapper<T> withSendKey(boolean sendKey) {
            this.classConfig.setSendKey(sendKey);
            return this;
        }
        public AeroConfigMapper<T> withMapAll(boolean mapAll) {
            this.classConfig.setMapAll(mapAll);
            return this;
        }
        public AeroConfigMapper<T> withDurableDelete(boolean durableDelete) {
            this.classConfig.setDurableDelete(durableDelete);
            return this;
        }
        public AeroConfigMapper<T> withShortName(boolean sendKey) {
            this.classConfig.setSendKey(sendKey);
            return this;
        }
        
        public AeroConfigMapper<T> withFactoryClassAndMethod(@NotNull Class<?> factoryClass, @NotNull String factoryMethod) {
            this.classConfig.setFactoryClass(factoryClass.getName());
            this.classConfig.setFactoryMethod(factoryMethod);
            return this;
        }
        
        public AeroConfigMapper<T> withKeyField(String fieldName) {
            if (this.classConfig.getKey() == null) {
                this.classConfig.setKey(new KeyConfig());
            }
            if (!ConfigurationUtils.validateFieldOnClass(this.clazz, fieldName)) {
                throw new AerospikeException(String.format("Field %s does not exist on class %s or its superclasses", fieldName, this.clazz));
            }
            this.classConfig.getKey().setField(fieldName);
            return this;
        }
        
        public AeroConfigMapper<T> withKeyGetterAndSetterOf(String getterName, String setterName) {
            if (this.classConfig.getKey() == null) {
                this.classConfig.setKey(new KeyConfig());
            }
            // TODO: Do we need to validate the method names?
            this.classConfig.getKey().setGetter(getterName);
            this.classConfig.getKey().setSetter(setterName);
            return this;
        }
        
        public AeroBinConfig<T> withFieldNamed(String fieldName) {
            if (!ConfigurationUtils.validateFieldOnClass(this.clazz, fieldName)) {
                throw new AerospikeException(String.format("Field %s does not exist on class %s or its superclasses", fieldName, this.clazz));
            }
            return new AeroBinConfig<T>(this, fieldName);
        }
        
        private void mergeBinConfig(BinConfig config) {
            List<BinConfig> bins = this.classConfig.getBins();
            for (BinConfig thisBin : bins) {
                if (config.getField().equals(thisBin.getField())) {
                    thisBin.merge(config);
                    return;
                }
            }
            this.classConfig.getBins().add(config);
        }
        
        public AbstractBuilder<T> end() {
            Configuration configuration = new Configuration();
            configuration.add(classConfig);
            ClassCache.getInstance().addConfiguration(configuration);
            return this.builder;
        }
    }
    
    public static class AeroBinConfig<T extends IBaseAeroMapper> {
        private final AeroConfigMapper<T> mapper;
        private final BinConfig binConfig;
        
        public AeroBinConfig(AeroConfigMapper<T> mapper, String fieldName) {
            super();
            this.mapper = mapper;
            this.binConfig = new BinConfig();
            this.binConfig.setField(fieldName);
        }
        
        public AeroConfigMapper<T> mappingToBin(String name) {
            this.binConfig.setName(name);
            return this.end();
        }
        
        public AeroConfigMapper<T> beingReferencedBy(AerospikeReference.ReferenceType type) {
            this.binConfig.setReference(new ReferenceConfig(type, false));
            return this.end();
        }
        
        public AeroConfigMapper<T> beingLazilyReferencedBy(AerospikeReference.ReferenceType type) {
            this.binConfig.setReference(new ReferenceConfig(type, true));
            return this.end();
        }
        
        public AeroConfigMapper<T> beingEmbeddedAs(AerospikeEmbed.EmbedType type) {
            EmbedConfig embedConfig = new EmbedConfig();
            embedConfig.setType(type);
            this.binConfig.setEmbed(embedConfig);
            return this.end();
        }
        public AeroConfigMapper<T> beingEmbeddedAs(AerospikeEmbed.EmbedType type, AerospikeEmbed.EmbedType elementType) {
            EmbedConfig embedConfig = new EmbedConfig();
            embedConfig.setType(type);
            embedConfig.setElementType(elementType);
            this.binConfig.setEmbed(embedConfig);
            return this.end();
        }
        public AeroConfigMapper<T> beingEmbeddedAs(AerospikeEmbed.EmbedType type, AerospikeEmbed.EmbedType elementType, boolean saveKey) {
            EmbedConfig embedConfig = new EmbedConfig();
            embedConfig.setType(type);
            embedConfig.setElementType(elementType);
            embedConfig.setSaveKey(saveKey);
            this.binConfig.setEmbed(embedConfig);
            return this.end();
        }
        /**
         * Exclude the field. An excluded field doesn't need any other config, so return the parent.
         * This allows for more natural syntax like:
         * <code>
         * .withConfigurationForClass(B.class)
         *     .withFieldName("ignoreMe").beingExcluded()
         * .end()
         * </code>
         * @return
         */
        public AeroConfigMapper<T> beingExcluded() {
            this.binConfig.setExclude(true);
            return this.end();
        }
        
        private AeroConfigMapper<T> end() {
            this.mapper.mergeBinConfig(binConfig);
            return this.mapper;
        }
    }
    
    public static class AeroPolicyMapper<T extends IBaseAeroMapper> {
        private final AbstractBuilder<T> builder;
        private final Policy policy;
        private final PolicyType policyType;

        public AeroPolicyMapper(AbstractBuilder<T> builder, PolicyType policyType, Policy policy) {
            this.builder = builder;
            this.policyType = policyType;
            this.policy = policy;
        }

        public AbstractBuilder<T> forClasses(Class<?>... classes) {
            for (Class<?> thisClass : classes) {
                ClassCache.getInstance().setSpecificPolicy(policyType, thisClass, policy);
            }
            return builder;
        }

        public AbstractBuilder<T> forThisOrChildrenOf(Class<?> clazz) {
            ClassCache.getInstance().setChildrenPolicy(this.policyType, clazz, this.policy);
            return builder;
        }

        public AbstractBuilder<T> forAll() {
            ClassCache.getInstance().setDefaultPolicy(policyType, policy);
            return builder;
        }
    }

    public AeroPolicyMapper<T> withReadPolicy(Policy policy) {
        return new AeroPolicyMapper<T>(this, PolicyType.READ, policy);
    }

    public AeroPolicyMapper<T> withWritePolicy(Policy policy) {
        return new AeroPolicyMapper<T>(this, PolicyType.WRITE, policy);
    }

    public AeroPolicyMapper<T> withBatchPolicy(BatchPolicy policy) {
        return new AeroPolicyMapper<T>(this, PolicyType.BATCH, policy);
    }

    public AeroPolicyMapper<T> withScanPolicy(ScanPolicy policy) {
        return new AeroPolicyMapper<T>(this, PolicyType.SCAN, policy);
    }

    public AeroPolicyMapper<T> withQueryPolicy(QueryPolicy policy) {
        return new AeroPolicyMapper<T>(this, PolicyType.QUERY, policy);
    }

    public T build() {
        if (classesToPreload != null) {
            for (Class<?> clazz : classesToPreload) {
                ClassCache.getInstance().loadClass(clazz, this.mapper);
            }
        }
        return this.mapper;
    }
}
