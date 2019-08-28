package org.molgenis.emx2.graphql;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaPrinter;
import org.junit.Test;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.utils.MolgenisException;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.Type.STRING;

public class EmxToGraphQLSchemaTest {

  @Test
  public void test1() throws MolgenisException {
    SchemaMetadata m = new SchemaMetadata("test");

    TableMetadata t2 = m.createTableIfNotExists("Family");
    t2.addColumn("Name", STRING);
    t2.addUnique("Name");

    TableMetadata t = m.createTableIfNotExists("Person");
    t.addColumn("FirstName", STRING);
    t.addColumn("LastName", STRING);
    t.addRef("family", t2.getTableName(), "Name");

    GraphQLSchema s = new GrahpqlEndpoint().getSchema(m);
    assertEquals(1, s.getType("Family").getChildren().size());
    assertEquals(3, s.getType("Person").getChildren().size());

    System.out.println(new SchemaPrinter().print(s));
  }
}
