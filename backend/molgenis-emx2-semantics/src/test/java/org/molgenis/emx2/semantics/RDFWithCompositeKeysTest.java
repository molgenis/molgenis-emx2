package org.molgenis.emx2.semantics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class RDFWithCompositeKeysTest {
  private static Schema schema;

  @BeforeAll
  public static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(RDFWithCompositeKeysTest.class.getSimpleName());
    schema.create(
        table("Patients", column("firstName").setPkey(), column("lastName").setPkey()),
        table(
            "Samples",
            column("patient").setType(ColumnType.REF).setRefTable("Patients").setPkey(),
            column("id").setPkey(),
            column("someNonKeyRef").setType(ColumnType.REF_ARRAY).setRefTable("Samples")));
    schema.getTable("Patients").insert(row("firstName", "Donald", "lastName", "Duck"));
    schema
        .getTable("Samples")
        .insert(
            row("patient.firstName", "Donald", "patient.lastName", "Duck", "id", "sample1"),
            row(
                "patient.firstName",
                "Donald",
                "patient.lastName",
                "Duck",
                "id",
                "sample2",
                "someNonKeyRef.patient.firstName",
                "Donald",
                "someNonKeyRef.patient.lastName",
                "Duck",
                "someNonKeyRef.id",
                "sample1"));
  }

  @Test
  void testRDFwithCompositeKeys() {
    RDFService rdf = new RDFService("http://localhost:8080" + RDFTest.RDF_API_LOCATION);
    OutputStream outputStream = new ByteArrayOutputStream();
    rdf.describeAsRDF(
        outputStream,
        RDFTest.RDF_API_LOCATION,
        null,
        null,
        null,
        List.of(schema).toArray(new Schema[1]));
    String result = outputStream.toString();
    assertTrue(result.contains(RDFWithCompositeKeysTest.class.getSimpleName()));
    String rowId =
        "http://localhost:8080/RDFWithCompositeKeysTest/api/rdf/Samples/patient.firstName=Donald&patient.lastName=Duck&id=sample1";
    String rowId2 =
        "http://localhost:8080/RDFWithCompositeKeysTest/api/rdf/Samples/patient.firstName=Donald&patient.lastName=Duck&id=sample2";
    String check_composite_key_element =
        "  <http://localhost:8080/RDFWithCompositeKeysTest/api/rdf/Patients/column/firstName>\n"
            + "    \"Donald\";";
    String check_composite_ref =
        "  <http://localhost:8080/RDFWithCompositeKeysTest/api/rdf/Samples/column/patient> <http://localhost:8080/RDFWithCompositeKeysTest/api/rdf/Patients/patient.firstName=Donald&patient.lastName=Duck>;";
    assertTrue(result.contains(rowId));
    assertTrue(result.contains(rowId2));
    assertTrue(result.contains(check_composite_key_element));
    assertTrue(result.contains(check_composite_ref));

    // should it also be possible to resolve these URIs?
    // but first rewrite the code because very confusing with passing of request/response
    outputStream = new ByteArrayOutputStream();
    rdf.describeAsRDF(
        outputStream,
        RDFTest.RDF_API_LOCATION,
        schema.getTable("Samples"),
        "patient.firstName=Donald&patient.lastName=Duck&id=sample2",
        null,
        List.of(schema).toArray(new Schema[1]));
    result = outputStream.toString();
    assertFalse(result.contains(rowId));
    assertTrue(result.contains(rowId2));
  }
}
