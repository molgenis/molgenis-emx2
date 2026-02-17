package org.molgenis.emx2;

import com.fasterxml.jackson.annotation.JsonAlias;

public record Permission(
    String tableId,
    String tableSchema,
    @JsonAlias("isRowLevel") boolean isRowLevel,
    boolean hasSelect,
    boolean hasInsert,
    boolean hasUpdate,
    boolean hasDelete,
    boolean hasAdmin) {}
