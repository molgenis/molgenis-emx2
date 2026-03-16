package org.molgenis.emx2;

public record TablePermission(
    String table,
    Boolean select,
    Boolean insert,
    Boolean update,
    Boolean delete,
    Boolean isRowLevel) {}
