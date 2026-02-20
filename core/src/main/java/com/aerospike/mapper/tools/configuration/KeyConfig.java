package com.aerospike.mapper.tools.configuration;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
public class KeyConfig {
    private String field;
    private String getter;
    private String setter;
    private Boolean storeAsBin;

    public boolean isGetter(String methodName) {
        return (!StringUtils.isBlank(this.getter)) && this.getter.equals(methodName);
    }

    public boolean isSetter(String methodName) {
        return (!StringUtils.isBlank(this.setter)) && this.setter.equals(methodName);
    }
}
