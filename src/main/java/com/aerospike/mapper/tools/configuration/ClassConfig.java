package com.aerospike.mapper.tools.configuration;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.aerospike.client.AerospikeException;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeReference;
import com.aerospike.mapper.tools.ConfigurationUtils;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClassConfig {
    @JsonProperty(value = "class")
    private String className;
    private String namespace;
    private String set;
    private Integer ttl;
    private Integer version;
    private Boolean sendKey;
    private Boolean mapAll;
    private Boolean durableDelete;
    private KeyConfig key;
    private String shortName;
    private String factoryClass;
    private String factoryMethod;
    private final List<BinConfig> bins;

    public ClassConfig() {
        bins = new ArrayList<>();
    }

    public String getClassName() {
        return className;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getSet() {
        return set;
    }

    public Integer getTtl() {
        return ttl;
    }

    public Integer getVersion() {
        return version;
    }

    public Boolean getSendKey() {
        return sendKey;
    }

    public Boolean getMapAll() {
        return mapAll;
    }

    public Boolean getDurableDelete() {
        return durableDelete;
    }

    public String getShortName() {
        return shortName;
    }

    public KeyConfig getKey() {
        return key;
    }

    public List<BinConfig> getBins() {
        return bins;
    }

    public String getFactoryClass() {
        return factoryClass;
    }

    public void setFactoryClass(String factoryClass) {
        this.factoryClass = factoryClass;
    }

    public String getFactoryMethod() {
        return factoryMethod;
    }

    public void setFactoryMethod(String factoryMethod) {
        this.factoryMethod = factoryMethod;
    }

    public BinConfig getBinByName(@NotNull String name) {
        for (BinConfig thisBin : bins) {
            if (name.equals(thisBin.getName())) {
                return thisBin;
            }
        }
        return null;
    }

    public BinConfig getBinByGetterName(@NotNull String getterName) {
        for (BinConfig thisBin : bins) {
            if (getterName.equals(thisBin.getGetter())) {
                return thisBin;
            }
        }
        return null;
    }

    public BinConfig getBinByFieldName(@NotNull String fieldName) {
        for (BinConfig thisBin : bins) {
            if (fieldName.equals(thisBin.getField())) {
                return thisBin;
            }
        }
        return null;
    }

    public void validate() {
        for (BinConfig thisBin : bins) {
            thisBin.validate(this.className);
        }
    }

    private void setClassName(String className) {
        this.className = className;
    }

    private void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    private void setSet(String set) {
        this.set = set;
    }

    private void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    private void setVersion(Integer version) {
        this.version = version;
    }

    private void setSendKey(Boolean sendKey) {
        this.sendKey = sendKey;
    }

    private void setMapAll(Boolean mapAll) {
        this.mapAll = mapAll;
    }

    private void setDurableDelete(Boolean durableDelete) {
        this.durableDelete = durableDelete;
    }

    private void setKey(KeyConfig key) {
        this.key = key;
    }

    private void setShortName(String shortName) {
        this.shortName = shortName;
    }
    
    public static class Builder {
        private final Class<?> clazz;
        private final ClassConfig classConfig;
        public Builder(final Class<?> clazz) {
            this.clazz = clazz;
            this.classConfig = new ClassConfig();
            this.classConfig.setClassName(clazz.getName());
        }
        
        private void validateFieldExists(String fieldName) {
            if (!ConfigurationUtils.validateFieldOnClass(this.clazz, fieldName)) {
                throw new AerospikeException(String.format("Field %s does not exist on class %s or its superclasses", fieldName, this.clazz));
            }
        }

        public Builder withNamespace(String namespace) {
            this.classConfig.setNamespace(namespace);
            return this;
        }
        public Builder withShortName(String shortName) {
            this.classConfig.setShortName(shortName);
            return this;
        }
        public Builder withSet(String setName) {
            this.classConfig.setSet(setName);
            return this;
        }
        public Builder withTtl(int ttl) {
            this.classConfig.setTtl(ttl);
            return this;
        }
        public Builder withVersion(int version) {
            this.classConfig.setVersion(version);
            return this;
        }
        public Builder withSendKey(boolean sendKey) {
            this.classConfig.setSendKey(sendKey);
            return this;
        }
        public Builder withMapAll(boolean mapAll) {
            this.classConfig.setMapAll(mapAll);
            return this;
        }
        public Builder withDurableDelete(boolean durableDelete) {
            this.classConfig.setDurableDelete(durableDelete);
            return this;
        }
        public Builder withShortName(boolean sendKey) {
            this.classConfig.setSendKey(sendKey);
            return this;
        }
        
        public Builder withFactoryClassAndMethod(@NotNull Class<?> factoryClass, @NotNull String factoryMethod) {
            this.classConfig.setFactoryClass(factoryClass.getName());
            this.classConfig.setFactoryMethod(factoryMethod);
            return this;
        }
        
        public Builder withKeyField(String fieldName) {
            if (this.classConfig.getKey() == null) {
                this.classConfig.setKey(new KeyConfig());
            }
            this.validateFieldExists(fieldName);
            this.classConfig.getKey().setField(fieldName);
            return this;
        }
        
        public Builder withKeyFieldAndStoreAsBin(String fieldName, boolean storeAsBin) {
            if (this.classConfig.getKey() == null) {
                this.classConfig.setKey(new KeyConfig());
            }
            this.validateFieldExists(fieldName);
            this.classConfig.getKey().setField(fieldName);
            this.classConfig.getKey().setStoreAsBin(storeAsBin);
            return this;
        }
        
        public Builder withKeyGetterAndSetterOf(String getterName, String setterName) {
            if (this.classConfig.getKey() == null) {
                this.classConfig.setKey(new KeyConfig());
            }
            // TODO: Do we need to validate the method names?
            this.classConfig.getKey().setGetter(getterName);
            this.classConfig.getKey().setSetter(setterName);
            return this;
        }
        
        public AeroBinConfig withFieldNamed(String fieldName) {
            validateFieldExists(fieldName);
            return new AeroBinConfig(this, fieldName);
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

        public ClassConfig build() {
            return this.classConfig;
        }
    }
    
    public static class AeroBinConfig {
        private final Builder builder;
        private final BinConfig binConfig;
        
        public AeroBinConfig(Builder builder, String fieldName) {
            super();
            this.builder = builder;
            this.binConfig = new BinConfig();
            this.binConfig.setField(fieldName);
        }
        
        public Builder mappingToBin(String name) {
            this.binConfig.setName(name);
            return this.end();
        }
        
        public Builder beingReferencedBy(AerospikeReference.ReferenceType type) {
            this.binConfig.setReference(new ReferenceConfig(type, false));
            return this.end();
        }
        
        public Builder beingLazilyReferencedBy(AerospikeReference.ReferenceType type) {
            this.binConfig.setReference(new ReferenceConfig(type, true));
            return this.end();
        }
        
        public Builder beingEmbeddedAs(AerospikeEmbed.EmbedType type) {
            EmbedConfig embedConfig = new EmbedConfig();
            embedConfig.setType(type);
            this.binConfig.setEmbed(embedConfig);
            return this.end();
        }
        public Builder beingEmbeddedAs(AerospikeEmbed.EmbedType type, AerospikeEmbed.EmbedType elementType) {
            EmbedConfig embedConfig = new EmbedConfig();
            embedConfig.setType(type);
            embedConfig.setElementType(elementType);
            this.binConfig.setEmbed(embedConfig);
            return this.end();
        }
        public Builder beingEmbeddedAs(AerospikeEmbed.EmbedType type, AerospikeEmbed.EmbedType elementType, boolean saveKey) {
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
        public Builder beingExcluded() {
            this.binConfig.setExclude(true);
            return this.end();
        }
        
        private Builder end() {
            this.builder.mergeBinConfig(binConfig);
            return this.builder;
        }
    }

}