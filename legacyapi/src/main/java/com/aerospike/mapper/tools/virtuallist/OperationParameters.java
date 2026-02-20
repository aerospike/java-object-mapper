package com.aerospike.mapper.tools.virtuallist;

import lombok.Getter;
import lombok.Setter;

public class OperationParameters {
    @Getter @Setter
    private ReturnType needsResultOfType = ReturnType.NONE;

    public OperationParameters() {
    }

    public OperationParameters(ReturnType needsResultOfType) {
        super();
        this.needsResultOfType = needsResultOfType;
    }
}
