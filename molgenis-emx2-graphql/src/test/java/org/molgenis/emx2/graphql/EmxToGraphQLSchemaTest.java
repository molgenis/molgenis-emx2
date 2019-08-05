package org.molgenis.emx2.graphql;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaPrinter;
import org.molgenis.MolgenisException;
import org.molgenis.Schema;
import org.molgenis.Table;
import org.molgenis.beans.SchemaBean;

import static org.molgenis.Type.STRING;

public class EmxToGraphQLSchemaTest {

  // @Test DISABLED BECAUSE SLOW
  public void test1() throws MolgenisException {
    Schema m = new SchemaBean("test");

    Table t2 = m.createTableIfNotExists("Family");
    t2.addColumn("Name", STRING);

    Table t = m.createTableIfNotExists("Person");
    t.addColumn("FirstName", STRING);
    t.addColumn("LastName", STRING);
    t.addRef("family", t2.getName());

    GraphQLSchema s = new GrahpqlEndpoint().getSchema(m);

    System.out.println(new SchemaPrinter().print(s));
  }
}
