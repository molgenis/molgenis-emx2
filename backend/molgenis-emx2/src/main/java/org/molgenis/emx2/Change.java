package org.molgenis.emx2;

import java.sql.Timestamp;
import org.jooq.JSONB;

public record Change(
    char operation, Timestamp stamp, String userId, JSONB oldRowData, JSONB newRowData) {}
