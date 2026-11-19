package org.molgenis.emx2.web;

import static org.molgenis.emx2.graphql.GraphqlTableFieldFactory.convertMapToFilterArray;
import static org.molgenis.emx2.web.Constants.COLUMNS;
import static org.molgenis.emx2.web.Constants.INCLUDE_SYSTEM_COLUMNS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.graphql.GraphqlConstants;

public final class DownloadApiUtils {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private DownloadApiUtils() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  static boolean includeSystemColumns(Context ctx) {
    return "true".equalsIgnoreCase(String.valueOf(ctx.queryParam(INCLUDE_SYSTEM_COLUMNS)));
  }

  /**
   * Columns passed as {@code ?columns=a,b} and/or repeated {@code ?columns=a&columns=b}; empty
   * means all columns.
   */
  static List<String> requestedColumns(Context ctx) {
    return ctx.queryParams(COLUMNS).stream()
        .flatMap(value -> Arrays.stream(value.split(",")))
        .map(String::trim)
        .filter(name -> !name.isEmpty())
        .toList();
  }

  static Query requestedTableQuery(Context ctx, Table table) throws JsonProcessingException {
    Query query = table.query();
    if (ctx.queryParam(GraphqlConstants.FILTER_ARGUMENT) != null) {
      query.where(
          convertMapToFilterArray(
              table.getMetadata(),
              MAPPER.readValue(ctx.queryParam(GraphqlConstants.FILTER_ARGUMENT), Map.class)));
    }
    return query;
  }
}
