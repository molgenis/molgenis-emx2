package org.molgenis.emx2.web.json;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.jooq.DSLContext;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.sql.SqlDatabase;

import java.io.IOException;
import java.io.StringWriter;

public class JsonUtil {
  private static ObjectWriter writer;

  public static org.molgenis.emx2.Column jsonToColumn(String json) throws IOException {
    org.molgenis.emx2.web.json.Column column =
        new ObjectMapper().readValue(json, org.molgenis.emx2.web.json.Column.class);
    return column.getColumnMetadata(null);
  }

  public static SchemaMetadata jsonToSchema(String json) throws IOException {
    org.molgenis.emx2.web.json.Schema s =
        new ObjectMapper().readValue(json, org.molgenis.emx2.web.json.Schema.class);
    return s.getSchemaMetadata();
  }

  public static String schemaToJson(SchemaMetadata schema) throws IOException {
    org.molgenis.emx2.web.json.Schema s = new org.molgenis.emx2.web.json.Schema(schema);
    StringWriter out = new StringWriter();
    getWriter().writeValue(out, s);
    return out.toString();
  }

  public static ObjectWriter getWriter() {

    if (writer == null) {
      DefaultPrettyPrinter printer =
          new DefaultPrettyPrinter()
              .withArrayIndenter(new DefaultIndenter("  ", "\n"))
              .withObjectIndenter(new DefaultIndenter("  ", "\n"));
      writer =
          new ObjectMapper()
              .addMixIn(SchemaMetadata.class, MixinForJsonIgnore.class)
              .addMixIn(TableMetadata.class, MixinForJsonIgnore.class)
              .addMixIn(Column.class, MixinForJsonIgnore.class)
              .addMixIn(SqlDatabase.class, MixinForJsonIgnore.class)
              .addMixIn(DSLContext.class, MixinForJsonIgnore.class)
              .setSerializationInclusion(JsonInclude.Include.NON_NULL)
              .setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
              .writer(printer);
    }
    return writer;
  }

  @JsonIgnoreType
  private static class MixinForJsonIgnore {}
}
