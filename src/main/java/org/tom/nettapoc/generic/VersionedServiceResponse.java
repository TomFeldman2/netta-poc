package org.tom.nettapoc.generic;

import java.util.List;

public record VersionedServiceResponse<E extends VersionedEntity<V>, V extends Comparable<V>>(
        List<E> data,
        List<String> deleted,
        V nextDataVersion
) {}