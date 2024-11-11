package org.molgenis.emx2;

import java.sql.Timestamp;

public record LastUpdate(
    char operation, Timestamp stamp, String userId, String tableName, String schemaName) {}
