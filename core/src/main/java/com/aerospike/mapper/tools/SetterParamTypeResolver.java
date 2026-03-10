package com.aerospike.mapper.tools;

import com.aerospike.mapper.tools.PropertyDefinition.SetterParamType;

/**
 * Resolves the {@link SetterParamType} for a setter's second parameter based on the parameter's fully-qualified type
 * name. This decouples core from specific client libraries.
 */
@FunctionalInterface
public interface SetterParamTypeResolver {

    /**
     * Determines if the given parameter type name corresponds to a Key or Value type in the client library used by this
     * mapper.
     *
     * @param paramTypeName fully-qualified class name of the setter's second parameter
     * @return the resolved {@link SetterParamType}, or {@link SetterParamType#NONE} if unrecognized
     */
    SetterParamType resolve(String paramTypeName);

    /**
     * Default resolver that does not recognize any client-specific parameter types.
     */
    SetterParamTypeResolver DEFAULT = paramTypeName -> SetterParamType.NONE;
}
