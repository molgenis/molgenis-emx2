package org.molgenis.emx2.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.utils.TableSort.sortTableByDependency;
import static org.molgenis.emx2.utils.TableSort.sortTableByImportDependency;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

class TestTableSort {

  private static SchemaMetadata createHospitalSchema() {
    SchemaMetadata schema = new SchemaMetadata("TestTableSort");
    schema.create(
        table("Hospital", column("name").setPkey(), column("city")),
        table(
            "Doctor",
            column("id").setPkey(),
            column("name"),
            column("hospital").setType(REF).setRefTable("Hospital")),
        table(
            "Patient",
            column("id").setPkey(),
            column("name"),
            column("doctor").setType(REF).setRefTable("Doctor")));
    return schema;
  }

  private static List<String> importOrder(SchemaMetadata schema) {
    List<TableMetadata> tables = new ArrayList<>(schema.getTables());
    sortTableByImportDependency(tables);
    return tables.stream().map(TableMetadata::getTableName).toList();
  }

  private static List<String> referenceOrder(SchemaMetadata schema) {
    List<TableMetadata> tables = new ArrayList<>(schema.getTables());
    sortTableByDependency(tables);
    return tables.stream().map(TableMetadata::getTableName).toList();
  }

  private static void assertImportedBefore(String first, String second, List<String> order) {
    assertTrue(
        order.indexOf(first) < order.indexOf(second), first + " before " + second + " in " + order);
  }

  @Test
  void keepsOrderWhenNoExpressionUsesARef() {
    SchemaMetadata schema = createHospitalSchema();
    schema.getTableMetadata("Doctor").add(column("display name").setComputed("name"));

    assertEquals(referenceOrder(schema), importOrder(schema));
  }

  @Test
  void importsRefTableBeforeTableWhoseExpressionUsesTheRef() {
    SchemaMetadata schema = createHospitalSchema();
    schema.getTableMetadata("Doctor").add(column("hospital city").setComputed("hospital.city"));

    assertImportedBefore("Hospital", "Doctor", importOrder(schema));
  }

  @Test
  void importsRefTableBeforeTableWhoseVisibleExpressionUsesTheRef() {
    SchemaMetadata schema = createHospitalSchema();
    schema
        .getTableMetadata("Doctor")
        .add(column("parking permit").setVisible("hospital.city == 'Groningen'"));

    assertImportedBefore("Hospital", "Doctor", importOrder(schema));
  }

  @Test
  void breaksCircularExpressionDependencies() {
    SchemaMetadata schema = createHospitalSchema();
    schema.getTableMetadata("Doctor").add(column("hospital city").setComputed("hospital.city"));
    schema
        .getTableMetadata("Hospital")
        .add(
            column("doctors").setType(REFBACK).setRefTable("Doctor").setRefBack("hospital"),
            column("number of doctors").setType(INT).setComputed("doctors.length"));
    schema.getTableMetadata("Patient").add(column("doctor name").setComputed("doctor.name"));

    List<String> order = importOrder(schema);
    assertEquals(3, order.size());
    assertImportedBefore("Doctor", "Patient", order);
  }

  @Test
  void keepsPrimaryKeyDependencyOverExpressionDependency() {
    SchemaMetadata schema = createHospitalSchema();
    schema.create(
        table(
            "Visit",
            column("patient").setType(REF).setRefTable("Patient").setKey(1),
            column("date").setType(DATE).setKey(1)));
    schema
        .getTableMetadata("Patient")
        .add(
            column("visits").setType(REFBACK).setRefTable("Visit").setRefBack("patient"),
            column("number of visits").setType(INT).setComputed("visits.length"));

    assertImportedBefore("Patient", "Visit", importOrder(schema));
  }

  @Test
  void ignoresRefsToTablesInOtherSchemas() {
    SchemaMetadata schema = createHospitalSchema();
    schema
        .getTableMetadata("Doctor")
        .add(
            column("hospital in other schema")
                .setType(REF)
                .setRefSchemaName("OtherSchema")
                .setRefTable("Hospital"),
            column("other city").setComputed("hospitalInOtherSchema.city"));

    assertEquals(referenceOrder(schema), importOrder(schema));
  }
}
