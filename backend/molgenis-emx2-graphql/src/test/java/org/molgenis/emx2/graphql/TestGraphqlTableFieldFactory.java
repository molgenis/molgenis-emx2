package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.TableMetadata.table;

import graphql.schema.GraphQLFieldDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestGraphqlTableFieldFactory {

  private static final String schemaName = TestGraphqlTableFieldFactory.class.getSimpleName();
  private static final String SAMPLES = "Samples";
  private static final String TYPE = "Type";
  private static final String NAME = "Name";
  private static final String GENDER = "Gender";

  private static Database database;

  private static Schema schema;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(schemaName);
  }

  @Test
  public void tableGroupByField() {

    schema.create(table(TYPE).add(column(NAME).setPkey()));
    schema.create(table(GENDER).add(column(NAME).setPkey()));

    final Table samplesTable =
        schema.create(
            table(SAMPLES)
                .add(column("N").setType(INT))
                .add(column(TYPE).setType(REF).setRefTable(TYPE).setPkey())
                .add(column(GENDER).setType(REF).setRefTable(GENDER).setPkey()));
    final TableMetadata samplesTableMetadata = samplesTable.getMetadata();

    GraphqlTableFieldFactory factory = new GraphqlTableFieldFactory(schema);

    final GraphQLFieldDefinition fieldDefinition = factory.tableGroupByField(samplesTableMetadata);
    fieldDefinition.getChildrenWithTypeReferences().getChildren();
  }
}
