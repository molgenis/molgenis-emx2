package org.molgenis.emx2.json;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.io.IOException;
import java.io.StringWriter;
import org.jooq.DSLContext;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.sql.SqlDatabase;

public class JsonUtil {

  private JsonUtil() {
    // hide constructor
  }

  private static ObjectWriter writer;
  private static ObjectWriter yamlWriter;

  public static org.molgenis.emx2.Column jsonToColumn(String json) throws IOException {
    Column column = new ObjectMapper().readValue(json, Column.class);
    return column.getColumnMetadata(null);
  }

  public static SchemaMetadata yamlToSchema(String yaml) throws IOException {
    Schema s = new ObjectMapper(new YAMLFactory()).readValue(yaml, Schema.class);
    return s.getSchemaMetadata();
  }

  public static SchemaMetadata jsonToSchema(String json) throws IOException {
    Schema s = new ObjectMapper().readValue(json, Schema.class);
    return s.getSchemaMetadata();
  }

  public static String schemaToJson(SchemaMetadata schema) throws IOException {
    return schemaToJson(schema, true);
  }

  public static String schemaToYaml(SchemaMetadata schema, boolean minimal) throws IOException {
    Schema s = new Schema(schema, minimal);
    StringWriter out = new StringWriter();
    getYamlWriter().writeValue(out, s);
    return out.toString();
  }

  public static String schemaToJson(SchemaMetadata schema, boolean minimal) throws IOException {
    Schema s = new Schema(schema, minimal);
    StringWriter out = new StringWriter();
    getWriter().writeValue(out, s);
    return out.toString();
  }

  public static ObjectWriter getYamlWriter() {

    if (yamlWriter == null) {
      DefaultPrettyPrinter printer =
          new DefaultPrettyPrinter()
              .withArrayIndenter(new DefaultIndenter("  ", "\n"))
              .withObjectIndenter(new DefaultIndenter("  ", "\n"));
      yamlWriter =
          new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
              .addMixIn(SchemaMetadata.class, MixinForJsonIgnore.class)
              .addMixIn(TableMetadata.class, MixinForJsonIgnore.class)
              .addMixIn(Column.class, MixinForJsonIgnore.class)
              .addMixIn(SqlDatabase.class, MixinForJsonIgnore.class)
              .addMixIn(DSLContext.class, MixinForJsonIgnore.class)
              .setSerializationInclusion(JsonInclude.Include.NON_NULL)
              .setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
              .writer(printer);
    }
    return yamlWriter;
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
