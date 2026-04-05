package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

class SubsetActivatorProjectionTest {

  @Test
  void alwaysOnColumnsIncludedWithNoActiveSubsets() {
    SchemaMetadata full = new SchemaMetadata();
    full.create(
        table("Animals")
            .add(column("id").setType(ColumnType.INT).setPkey())
            .add(column("name").setType(ColumnType.STRING)));

    SchemaMetadata projected = SubsetActivator.projectSchemaMetadataToActiveSubsets(full, Set.of());

    TableMetadata animals = projected.getTableMetadata("Animals");
    assertNotNull(animals);
    assertNotNull(animals.getColumn("id"));
    assertNotNull(animals.getColumn("name"));
  }

  @Test
  void subsetTaggedColumnExcludedWhenSubsetInactive() {
    SchemaMetadata full = new SchemaMetadata();
    full.create(
        table("Animals")
            .add(column("id").setType(ColumnType.INT).setPkey())
            .add(column("weight").setType(ColumnType.DECIMAL).setSubsets("wgs")));

    SchemaMetadata projected = SubsetActivator.projectSchemaMetadataToActiveSubsets(full, Set.of());

    TableMetadata animals = projected.getTableMetadata("Animals");
    assertNotNull(animals);
    assertNotNull(animals.getColumn("id"), "always-on column should be present");
    assertNull(animals.getColumn("weight"), "subset-tagged column should be absent");
  }

  @Test
  void subsetTaggedColumnIncludedWhenSubsetActive() {
    SchemaMetadata full = new SchemaMetadata();
    full.create(
        table("Animals")
            .add(column("id").setType(ColumnType.INT).setPkey())
            .add(column("weight").setType(ColumnType.DECIMAL).setSubsets("wgs")));

    SchemaMetadata projected =
        SubsetActivator.projectSchemaMetadataToActiveSubsets(full, Set.of("wgs"));

    TableMetadata animals = projected.getTableMetadata("Animals");
    assertNotNull(animals);
    assertNotNull(animals.getColumn("weight"), "subset-tagged column should be present");
  }

  @Test
  void subsetTaggedTableExcludedWhenSubsetInactive() {
    SchemaMetadata full = new SchemaMetadata();
    full.create(table("Experiments").add(column("id").setType(ColumnType.INT).setPkey()));
    full.create(
        table("Sequencing").setSubsets("seq").add(column("id").setType(ColumnType.INT).setPkey()));

    SchemaMetadata projected = SubsetActivator.projectSchemaMetadataToActiveSubsets(full, Set.of());

    assertNotNull(projected.getTableMetadata("Experiments"), "always-on table should be present");
    assertNull(projected.getTableMetadata("Sequencing"), "subset-tagged table should be absent");
  }

  @Test
  void subsetTaggedTableIncludedWhenSubsetActive() {
    SchemaMetadata full = new SchemaMetadata();
    full.create(table("Experiments").add(column("id").setType(ColumnType.INT).setPkey()));
    full.create(
        table("Sequencing").setSubsets("seq").add(column("id").setType(ColumnType.INT).setPkey()));

    SchemaMetadata projected =
        SubsetActivator.projectSchemaMetadataToActiveSubsets(full, Set.of("seq"));

    assertNotNull(
        projected.getTableMetadata("Sequencing"), "subset-tagged table should be present");
  }

  @Test
  void multipleSubsetsColumnIncludedWhenAnyActive() {
    SchemaMetadata full = new SchemaMetadata();
    Column col = column("coverage").setType(ColumnType.INT).setSubsets("wgs", "long_read");
    full.create(table("Animals").add(column("id").setType(ColumnType.INT).setPkey()).add(col));

    SchemaMetadata projectedWgs =
        SubsetActivator.projectSchemaMetadataToActiveSubsets(full, Set.of("wgs"));
    assertNotNull(
        projectedWgs.getTableMetadata("Animals").getColumn("coverage"),
        "column with wgs tag should appear when wgs is active");

    SchemaMetadata projectedNone =
        SubsetActivator.projectSchemaMetadataToActiveSubsets(full, Set.of("other"));
    assertNull(
        projectedNone.getTableMetadata("Animals").getColumn("coverage"),
        "column should be absent when neither wgs nor long_read is active");
  }
}
