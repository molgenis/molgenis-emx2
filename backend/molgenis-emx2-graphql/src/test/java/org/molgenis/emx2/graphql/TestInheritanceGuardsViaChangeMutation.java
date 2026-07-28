package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestInheritanceGuardsViaChangeMutation {

  private static Database db;

  @BeforeAll
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
    db.becomeAdmin();
  }

  private static final String CANNOT_CHANGE =
      "': inheritance cannot be changed after the table is created.";

  private static void change(String schemaName, String mutation) {
    JsonNode node;
    try {
      String result =
          convertExecutionResultToJson(
              new GraphqlExecutor(db.getSchema(schemaName)).executeWithoutSession(mutation));
      node = new ObjectMapper().readTree(result);
    } catch (Exception e) {
      throw new MolgenisException("could not run graphql mutation", e);
    }
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
  }

  private static void assertRefused(String schemaName, String mutation, String expectedMessage) {
    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> change(schemaName, mutation));
    assertTrue(exception.getMessage().contains(expectedMessage), exception.getMessage());
  }

  @Test
  public void everyInheritanceChangeIsRefusedViaChangeMutation() {
    // 1 change tableExtends
    db.dropSchemaIfExists("GqlGuardChange");
    Schema s1 = db.createSchema("GqlGuardChange");
    s1.create(table("Shape").add(column("name").setPkey()));
    s1.create(table("Other").add(column("oname").setPkey()));
    s1.create(table("MyShape").setInheritName("Shape").add(column("size")));
    assertRefused(
        "GqlGuardChange",
        """
        mutation{change(tables:[{name:"MyShape",inheritName:"Other"}]){message}}""",
        "Cannot change tableExtends of table 'GqlGuardChange.MyShape" + CANNOT_CHANGE);

    // 2 add tableExtends to a plain existing table
    db.dropSchemaIfExists("GqlGuardAdd");
    Schema s2 = db.createSchema("GqlGuardAdd");
    s2.create(table("Shape").add(column("name").setPkey()));
    s2.create(table("MyShape").add(column("myid").setPkey()));
    assertRefused(
        "GqlGuardAdd",
        """
        mutation{change(tables:[{name:"MyShape",inheritName:"Shape"}]){message}}""",
        "Cannot set tableExtends of table 'GqlGuardAdd.MyShape" + CANNOT_CHANGE);

    // 3 change refSchema to another schema holding a same-named table
    db.dropSchemaIfExists("GqlGuardChild");
    db.dropSchemaIfExists("GqlGuardParentA");
    db.dropSchemaIfExists("GqlGuardParentB");
    db.createSchema("GqlGuardParentA").create(table("Shape").add(column("name").setPkey()));
    db.createSchema("GqlGuardParentB").create(table("Shape").add(column("name").setPkey()));
    Schema s3 = db.createSchema("GqlGuardChild");
    s3.create(
        table("MyShape")
            .setInheritName("Shape")
            .setImportSchema("GqlGuardParentA")
            .add(column("size")));
    assertRefused(
        "GqlGuardChild",
        """
        mutation{change(tables:[
          {name:"MyShape",inheritName:"Shape",inheritSchemaName:"GqlGuardParentB"}
        ]){message}}""",
        "Cannot change refSchema of table 'GqlGuardChild.MyShape" + CANNOT_CHANGE);

    // 4 remove tableExtends
    db.dropSchemaIfExists("GqlGuardRemove");
    Schema s4 = db.createSchema("GqlGuardRemove");
    s4.create(table("Shape").add(column("name").setPkey()));
    s4.create(table("MyShape").setInheritName("Shape").add(column("size")));
    assertRefused(
        "GqlGuardRemove",
        """
        mutation{change(tables:[{name:"MyShape",columns:[{name:"size"}]}]){message}}""",
        "Cannot remove tableExtends of table 'GqlGuardRemove.MyShape" + CANNOT_CHANGE);

    // 5 add refSchema to a locally inheriting child
    db.dropSchemaIfExists("GqlGuardLocal");
    Schema s5 = db.createSchema("GqlGuardLocal");
    s5.create(table("Shape").add(column("name").setPkey()));
    s5.create(table("MyShape").setInheritName("Shape").add(column("size")));
    assertRefused(
        "GqlGuardLocal",
        """
        mutation{change(tables:[
          {name:"MyShape",inheritName:"Shape",inheritSchemaName:"GqlGuardParentA"}
        ]){message}}""",
        "Cannot change refSchema of table 'GqlGuardLocal.MyShape" + CANNOT_CHANGE);

    // 6 rename a parent that has an inheriting child, via oldName
    db.dropSchemaIfExists("GqlGuardRename");
    Schema s6 = db.createSchema("GqlGuardRename");
    s6.create(table("Shape").add(column("name").setPkey()));
    s6.create(table("MyShape").setInheritName("Shape").add(column("size")));
    assertRefused(
        "GqlGuardRename",
        """
        mutation{change(tables:[{name:"Renamed",oldName:"Shape"}]){message}}""",
        "Cannot rename table 'GqlGuardRename.Shape': table 'GqlGuardRename.MyShape' inherits from it.");
  }
}
