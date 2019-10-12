package org.molgenis.emx2.web.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.jooq.DSLContext;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.sql.SqlDatabase;

import java.io.IOException;
import java.io.StringWriter;

public class JsonSchemaMapper {

  private static ObjectWriter writer;

  private JsonSchemaMapper() {
    // hide constructor
  }

  public static String schemaToJson(SchemaMetadata schema) throws IOException {
    Schema s = new Schema(schema);
    StringWriter out = new StringWriter();
    getWriter().writeValue(out, s);
    return out.toString();
  }

  public static SchemaMetadata jsonToSchema(String json) throws IOException {
    Schema s = new ObjectMapper().readValue(json, Schema.class);
    return s.getSchemaMetadata();
  }

  // todo, deduplicate this method
  public static ObjectWriter getWriter() {

    if (writer == null) {
      DefaultPrettyPrinter printer =
          new DefaultPrettyPrinter()
              .withArrayIndenter(new DefaultIndenter("  ", "\n"))
              .withObjectIndenter(new DefaultIndenter("  ", "\n"));
      writer =
          new ObjectMapper()
              .addMixIn(SchemaMetadata.class, JsonMapper.MixinForJsonIgnore.class)
              .addMixIn(TableMetadata.class, JsonMapper.MixinForJsonIgnore.class)
              .addMixIn(Column.class, JsonMapper.MixinForJsonIgnore.class)
              .addMixIn(SqlDatabase.class, JsonMapper.MixinForJsonIgnore.class)
              .addMixIn(DSLContext.class, JsonMapper.MixinForJsonIgnore.class)
              .setSerializationInclusion(JsonInclude.Include.NON_NULL)
              .setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
              .writer(printer);
    }
    return writer;
  }
}
