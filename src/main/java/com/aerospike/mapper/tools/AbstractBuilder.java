package com.aerospike.mapper.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Log;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.ClassCache.PolicyType;
import com.aerospike.mapper.tools.configuration.ClassConfig;
import com.aerospike.mapper.tools.configuration.Configuration;
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
        GenericTypeMapper typeMapper = new GenericTypeMapper(converter);
        TypeUtils.addTypeMapper(typeMapper.getMappedClass(), typeMapper);

        return this;
    }

    public AbstractBuilder<T> preLoadClasses(Class<?>... clazzes) {
        if (classesToPreload == null) {
            classesToPreload = new ArrayList<>();
        }
        classesToPreload.addAll(Arrays.asList(clazzes));
        return this;
    }

    public String getPackageName(Class<?> clazz) {
        Class<?> c;
        if (clazz.isArray()) {
            c = clazz.getComponentType();
        } else {
            c = clazz;
        }
        String pn;
        if (c.isPrimitive()) {
            pn = "java.lang";
        } else {
            String cn = c.getName();
            int dot = cn.lastIndexOf('.');
            pn = (dot != -1) ? cn.substring(0, dot).intern() : "";
        }
        return pn;
    }

    public AbstractBuilder<T> preLoadClassesFromPackage(Class<?> classInPackage) {
        return preLoadClassesFromPackage(getPackageName(classInPackage));
    }

    public AbstractBuilder<T> preLoadClassesFromPackage(String thePackage) {
        Set<Class<?>> clazzes = findAllClassesUsingClassLoader(thePackage);
        for (Class<?> thisClazz : clazzes) {
            // Only add classes with the AerospikeRecord annotation.
            if (thisClazz.getAnnotation(AerospikeRecord.class) != null) {
                this.preLoadClass(thisClazz);
            }
        }
        return this;
    }

    // See https://www.baeldung.com/java-find-all-classes-in-package
    private Set<Class<?>> findAllClassesUsingClassLoader(String packageName) {
        InputStream stream = ClassLoader.getSystemClassLoader()
                .getResourceAsStream(packageName.replaceAll("[.]", "/"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        return reader.lines().filter(line -> line.endsWith(".class")).map(line -> getClass(line, packageName))
                .collect(Collectors.toSet());
    }

    private Class<?> getClass(String className, String packageName) {
        try {
            return Class.forName(packageName + "." + className.substring(0, className.lastIndexOf('.')));
        } catch (ClassNotFoundException ignored) {
        }
        return null;
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
    
    public AbstractBuilder<T> withClassConfigurations(ClassConfig classConfig, ClassConfig ...classConfigs) {
        Configuration configuration = new Configuration();
        configuration.add(classConfig);
        for (ClassConfig thisConfig : classConfigs) {
            configuration.add(thisConfig);
        }
        ClassCache.getInstance().addConfiguration(configuration);
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
        return new AeroPolicyMapper<>(this, PolicyType.READ, policy);
    }

    public AeroPolicyMapper<T> withWritePolicy(Policy policy) {
        return new AeroPolicyMapper<>(this, PolicyType.WRITE, policy);
    }

    public AeroPolicyMapper<T> withBatchPolicy(BatchPolicy policy) {
        return new AeroPolicyMapper<>(this, PolicyType.BATCH, policy);
    }

    public AeroPolicyMapper<T> withScanPolicy(ScanPolicy policy) {
        return new AeroPolicyMapper<>(this, PolicyType.SCAN, policy);
    }

    public AeroPolicyMapper<T> withQueryPolicy(QueryPolicy policy) {
        return new AeroPolicyMapper<>(this, PolicyType.QUERY, policy);
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
