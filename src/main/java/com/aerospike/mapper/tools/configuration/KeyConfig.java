package com.aerospike.mapper.tools.configuration;

import org.apache.commons.lang3.StringUtils;

public class KeyConfig {
    private String field;
    private String getter;
    private String setter;

    public String getField() {
        return field;
    }

    public String getGetter() {
        return getter;
    }

    public String getSetter() {
        return setter;
    }

    
    public void setField(String field) {
        this.field = field;
    }

    public void setGetter(String getter) {
        this.getter = getter;
    }

    public void setSetter(String setter) {
        this.setter = setter;
    }

    public boolean isGetter(String methodName) {
        return (!StringUtils.isBlank(this.getter)) && this.getter.equals(methodName);
    }

    public boolean isSetter(String methodName) {
        return (!StringUtils.isBlank(this.setter)) && this.setter.equals(methodName);
    }
}
