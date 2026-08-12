package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestGraphQLRefFilterSemantics {

  private static GraphqlExecutor graphql;
  private static Database database;
  private static final String SCHEMA_NAME = TestGraphQLRefFilterSemantics.class.getSimpleName();

  @BeforeAll
  public static void setup() throws IOException {
    database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(SCHEMA_NAME);
    graphql = new GraphqlExecutor(schema);

    execute(
        """
        mutation {change(tables:[
          {name:"Tag", columns:[{name:"code", key:1}]}
          {name:"Category", columns:[{name:"name", key:1}]}
          {name:"Item", columns:[
            {name:"id", key:1}
            {name:"refCategory", columnType:"REF", refTableName:"Category"}
            {name:"radioCategory", columnType:"RADIO", refTableName:"Category"}
            {name:"tagsRefArray", columnType:"REF_ARRAY", refTableName:"Tag"}
            {name:"tagsCheckbox", columnType:"CHECKBOX", refTableName:"Tag"}
            {name:"tagsMultiselect", columnType:"MULTISELECT", refTableName:"Tag"}
          ]}
          {name:"Part", columns:[
            {name:"item", columnType:"REF", refTableName:"Item", key:1, required:"true"}
            {name:"name", key:1}
          ]}
        ]){message}}
        """);

    execute(
        """
        mutation {change(columns:[
          {table:"Item", name:"parts", columnType:"PARTS", refTableName:"Part", refBackName:"item"}
          {table:"Item", name:"partsAsRefback", columnType:"REFBACK", refTableName:"Part", refBackName:"item"}
        ]){message}}
        """);

    graphql = new GraphqlExecutor(database.getSchema(SCHEMA_NAME));

    execute(
        """
        mutation {insert(
          Tag:[{code:"X"},{code:"Y"},{code:"Z"}]
          Category:[{name:"cat"},{name:"dog"},{name:"fish"}]
        ){message}}
        """);
    execute(
        """
        mutation {insert(Item:[
          {id:"i1", refCategory:{name:"cat"}, radioCategory:{name:"cat"},
           tagsRefArray:[{code:"X"}], tagsCheckbox:[{code:"X"}], tagsMultiselect:[{code:"X"}]}
          {id:"i2", refCategory:{name:"dog"}, radioCategory:{name:"dog"},
           tagsRefArray:[{code:"Y"}], tagsCheckbox:[{code:"Y"}], tagsMultiselect:[{code:"Y"}]}
          {id:"i3", refCategory:{name:"fish"}, radioCategory:{name:"fish"},
           tagsRefArray:[{code:"Z"}], tagsCheckbox:[{code:"Z"}], tagsMultiselect:[{code:"Z"}]}
          {id:"i4", refCategory:{name:"cat"}, radioCategory:{name:"cat"},
           tagsRefArray:[{code:"X"},{code:"Y"}], tagsCheckbox:[{code:"X"},{code:"Y"}],
           tagsMultiselect:[{code:"X"},{code:"Y"}]}
        ]){message}}
        """);
    execute(
        """
        mutation {insert(Part:[
          {item:{id:"i1"}, name:"p1"}
          {item:{id:"i2"}, name:"p2"}
          {item:{id:"i3"}, name:"p3"}
        ]){message}}
        """);
  }

  @Test
  public void multipleValuesOnOneRefColumnAreOred() throws IOException {
    for (String column : List.of("refCategory", "radioCategory")) {
      assertEquals(
          List.of("i1", "i2", "i4"),
          itemIds(
              "{Item(orderby:{id:ASC},filter:{"
                  + column
                  + ":{name:{equals:[\"cat\",\"dog\"]}}}){id}}"),
          column + " equals list must be OR");
    }
    for (String column : List.of("tagsRefArray", "tagsCheckbox", "tagsMultiselect")) {
      assertEquals(
          List.of("i1", "i2", "i4"),
          itemIds(
              "{Item(orderby:{id:ASC},filter:{" + column + ":{code:{equals:[\"X\",\"Y\"]}}}){id}}"),
          column + " equals list must be OR");
    }
    for (String column : List.of("parts", "partsAsRefback")) {
      assertEquals(
          List.of("i1", "i2"),
          itemIds(
              "{Item(orderby:{id:ASC},filter:{"
                  + column
                  + ":{name:{equals:[\"p1\",\"p2\"]}}}){id}}"),
          column + " equals list must be OR");
    }
  }

  @Test
  public void equalsListMatchesOrOfEquals() throws IOException {
    for (String column : List.of("refCategory", "radioCategory")) {
      assertEquals(
          itemIds(
              "{Item(orderby:{id:ASC},filter:{"
                  + column
                  + ":{_or:[{name:{equals:\"cat\"}},{name:{equals:\"dog\"}}]}}){id}}"),
          itemIds(
              "{Item(orderby:{id:ASC},filter:{"
                  + column
                  + ":{name:{equals:[\"cat\",\"dog\"]}}}){id}}"),
          column + " equals list must select the same rows as an _or of single equals");
    }
    for (String column : List.of("tagsRefArray", "tagsCheckbox", "tagsMultiselect")) {
      assertEquals(
          itemIds(
              "{Item(orderby:{id:ASC},filter:{"
                  + column
                  + ":{_or:[{code:{equals:\"X\"}},{code:{equals:\"Y\"}}]}}){id}}"),
          itemIds(
              "{Item(orderby:{id:ASC},filter:{" + column + ":{code:{equals:[\"X\",\"Y\"]}}}){id}}"),
          column + " equals list must select the same rows as an _or of single equals");
    }
  }

  @Test
  public void partsFilterBehavesLikeRefback() throws IOException {
    assertEquals(
        itemIds(
            "{Item(orderby:{id:ASC},filter:{partsAsRefback:{name:{equals:[\"p1\",\"p3\"]}}}){id}}"),
        itemIds("{Item(orderby:{id:ASC},filter:{parts:{name:{equals:[\"p1\",\"p3\"]}}}){id}}"),
        "PARTS must filter identically to REFBACK");
    assertEquals(
        itemIds(
            "{Item(orderby:{id:ASC},filter:{partsAsRefback:{equals:[{name:\"p1\"},{name:\"p3\"}]}}){id}}"),
        itemIds(
            "{Item(orderby:{id:ASC},filter:{parts:{equals:[{name:\"p1\"},{name:\"p3\"}]}}){id}}"),
        "PARTS key-object equals must filter identically to REFBACK");
    assertEquals(
        List.of("i1", "i3"),
        itemIds(
            "{Item(orderby:{id:ASC},filter:{parts:{equals:[{name:\"p1\"},{name:\"p3\"}]}}){id}}"),
        "PARTS key-object equals list must be OR");
  }

  @Test
  public void filtersOnDifferentColumnsAreAnded() throws IOException {
    assertEquals(
        List.of("i2", "i4"),
        itemIds(
            "{Item(orderby:{id:ASC},filter:{refCategory:{name:{equals:[\"cat\",\"dog\"]}},tagsCheckbox:{code:{equals:[\"Y\"]}}}){id}}"),
        "two column filters must be AND while each stays OR internally");
    assertEquals(
        List.of("i4"),
        itemIds(
            "{Item(orderby:{id:ASC},filter:{refCategory:{name:{equals:[\"cat\"]}},tagsCheckbox:{code:{equals:[\"Y\"]}}}){id}}"),
        "two column filters must be AND");
    assertEquals(
        List.of(),
        itemIds(
            "{Item(orderby:{id:ASC},filter:{refCategory:{name:{equals:[\"dog\"]}},tagsCheckbox:{code:{equals:[\"Z\"]}}}){id}}"),
        "two column filters must be AND");
  }

  private List<String> itemIds(String query) throws IOException {
    JsonNode result = execute(query);
    List<String> ids = new ArrayList<>();
    result.at("/Item").forEach(item -> ids.add(item.get("id").asText()));
    return ids;
  }

  private static JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(graphql.executeWithoutSession(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return node.get("data");
  }
}
