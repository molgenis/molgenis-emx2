package org.molgenis.emx2.datamodels.test;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.ColumnType.REF_ARRAY;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

public class CrossSchemaReferenceExample {

  public static void create(Schema schema1, Schema schema2) {
    Table parent = schema1.create(table("Parent", column("name").setPkey(), column("hobby")));
    parent.insert(row("name", "parent1", "hobby", "stamps"));

    Table child =
        schema2.create(
            table(
                "Child",
                column("name").setPkey(),
                column("parent")
                    .setType(REF)
                    .setRefSchemaName(schema1.getName())
                    .setRefTable("Parent")));
    child.insert(row("name", "child1", "parent", "parent1"));

    Table pet = schema1.create(table("Pet", column("name").setPkey(), column("species")));
    pet.insert(row("name", "pooky", "species", "cat"));
    pet.insert(row("name", "spike", "species", "dog"));

    Table petLover =
        schema2.create(
            table(
                "PetLover",
                column("name").setPkey(),
                column("pets")
                    .setType(REF_ARRAY)
                    .setRefSchemaName(schema1.getName())
                    .setRefTable("Pet")));
    petLover.insert(row("name", "x", "pets", new String[] {"pooky", "spike"}));

    Table cat =
        schema2.create(table("Mouse").setImportSchema(schema1.getName()).setInheritName("Pet"));
    cat.insert(row("name", "mickey", "species", "mouse"));
  }
}
