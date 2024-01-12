package org.molgenis.emx2;

import java.sql.Timestamp;

public record Change(
    char operation,
    Timestamp stamp,
    String userId,
    String tableName,
    String oldRowData,
    String newRowData) {}
