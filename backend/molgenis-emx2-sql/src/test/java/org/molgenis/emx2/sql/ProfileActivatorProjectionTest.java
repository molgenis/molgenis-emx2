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

class ProfileActivatorProjectionTest {

  @Test
  void alwaysOnColumnsIncludedWithNoActiveProfiles() {
    SchemaMetadata full = new SchemaMetadata();
    full.create(
        table("Animals")
            .add(column("id").setType(ColumnType.INT).setPkey())
            .add(column("name").setType(ColumnType.STRING)));

    SchemaMetadata projected =
        ProfileActivator.projectSchemaMetadataToActiveProfiles(full, Set.of());

    TableMetadata animals = projected.getTableMetadata("Animals");
    assertNotNull(animals);
    assertNotNull(animals.getColumn("id"));
    assertNotNull(animals.getColumn("name"));
  }

  @Test
  void profileTaggedColumnExcludedWhenProfileInactive() {
    SchemaMetadata full = new SchemaMetadata();
    full.create(
        table("Animals")
            .add(column("id").setType(ColumnType.INT).setPkey())
            .add(column("weight").setType(ColumnType.DECIMAL).setProfiles("wgs")));

    SchemaMetadata projected =
        ProfileActivator.projectSchemaMetadataToActiveProfiles(full, Set.of());

    TableMetadata animals = projected.getTableMetadata("Animals");
    assertNotNull(animals);
    assertNotNull(animals.getColumn("id"), "always-on column should be present");
    assertNull(animals.getColumn("weight"), "profile-tagged column should be absent");
  }

  @Test
  void profileTaggedColumnIncludedWhenProfileActive() {
    SchemaMetadata full = new SchemaMetadata();
    full.create(
        table("Animals")
            .add(column("id").setType(ColumnType.INT).setPkey())
            .add(column("weight").setType(ColumnType.DECIMAL).setProfiles("wgs")));

    SchemaMetadata projected =
        ProfileActivator.projectSchemaMetadataToActiveProfiles(full, Set.of("wgs"));

    TableMetadata animals = projected.getTableMetadata("Animals");
    assertNotNull(animals);
    assertNotNull(animals.getColumn("weight"), "profile-tagged column should be present");
  }

  @Test
  void profileTaggedTableExcludedWhenProfileInactive() {
    SchemaMetadata full = new SchemaMetadata();
    full.create(table("Experiments").add(column("id").setType(ColumnType.INT).setPkey()));
    full.create(
        table("Sequencing").setProfiles("seq").add(column("id").setType(ColumnType.INT).setPkey()));

    SchemaMetadata projected =
        ProfileActivator.projectSchemaMetadataToActiveProfiles(full, Set.of());

    assertNotNull(projected.getTableMetadata("Experiments"), "always-on table should be present");
    assertNull(projected.getTableMetadata("Sequencing"), "profile-tagged table should be absent");
  }

  @Test
  void profileTaggedTableIncludedWhenProfileActive() {
    SchemaMetadata full = new SchemaMetadata();
    full.create(table("Experiments").add(column("id").setType(ColumnType.INT).setPkey()));
    full.create(
        table("Sequencing").setProfiles("seq").add(column("id").setType(ColumnType.INT).setPkey()));

    SchemaMetadata projected =
        ProfileActivator.projectSchemaMetadataToActiveProfiles(full, Set.of("seq"));

    assertNotNull(
        projected.getTableMetadata("Sequencing"), "profile-tagged table should be present");
  }

  @Test
  void multipleProfilesColumnIncludedWhenAnyActive() {
    SchemaMetadata full = new SchemaMetadata();
    Column col = column("coverage").setType(ColumnType.INT).setProfiles("wgs", "long_read");
    full.create(table("Animals").add(column("id").setType(ColumnType.INT).setPkey()).add(col));

    SchemaMetadata projectedWgs =
        ProfileActivator.projectSchemaMetadataToActiveProfiles(full, Set.of("wgs"));
    assertNotNull(
        projectedWgs.getTableMetadata("Animals").getColumn("coverage"),
        "column with wgs tag should appear when wgs is active");

    SchemaMetadata projectedNone =
        ProfileActivator.projectSchemaMetadataToActiveProfiles(full, Set.of("other"));
    assertNull(
        projectedNone.getTableMetadata("Animals").getColumn("coverage"),
        "column should be absent when neither wgs nor long_read is active");
  }
}
