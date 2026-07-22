package org.molgenis.emx2.io.emx2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

class Emx2YamlTest {

  private Path minimalBundleDir() throws Exception {
    return Path.of(getClass().getResource("/yamlbundle/minimal/molgenis.yaml").toURI()).getParent();
  }

  private Path bundleDir(String name) throws Exception {
    return Path.of(getClass().getResource("/yamlbundle/" + name + "/molgenis.yaml").toURI())
        .getParent();
  }

  private static List<String> nonSystemNames(List<Column> columns) {
    return columns.stream()
        .filter(column -> !column.isSystemColumn())
        .map(Column::getName)
        .toList();
  }

  @Test
  void minimalBundleRoundTripsByteIdentical() throws Exception {
    Emx2YamlBundle parsed = Emx2Yaml.fromBundle(minimalBundleDir());

    SchemaMetadata schema = parsed.schema();
    assertEquals(2, schema.getTables().size());
    assertEquals("1.0.0", parsed.version());
    assertEquals("main", schema.getSettings().get("menu"));
    TableMetadata pet = schema.getTableMetadata("Pet");
    assertNotNull(pet);
    assertEquals("Pets", pet.getLabels().get("en"));
    assertEquals("card", pet.getSettings().get("row_style"));
    assertEquals(1, pet.getColumn("name").getKey());
    assertEquals("true", pet.getColumn("active").getRequired());

    Map<String, String> firstExport = Emx2Yaml.toBundleFiles(parsed);
    Map<String, String> secondExport =
        Emx2Yaml.toBundleFiles(Emx2Yaml.fromBundleFiles(firstExport));

    assertEquals(firstExport, secondExport);
  }

  @Test
  void unknownKeyError() {
    Map<String, String> files =
        Map.of(
            "molgenis.yaml", "tables:\n- file: tables/Bad.yaml\n",
            "tables/Bad.yaml",
                "name: Bad\ncolumns:\n- name: id\n  key: 1\n- name: broken\n  refTabel: Something\n");

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> Emx2Yaml.fromBundleFiles(files));

    String message = exception.getMessage();
    assertTrue(message.contains("refTabel"), message);
    assertTrue(message.contains("tables/Bad.yaml"), message);
    assertTrue(message.contains("columns[1]"), message);
    assertTrue(message.contains("line"), message);
    assertTrue(message.contains("column"), message);
  }

  @Test
  void weavingPositions() throws Exception {
    SchemaMetadata schema = Emx2Yaml.fromBundle(bundleDir("woven")).schema();

    Column draftNote = schema.getTableMetadata("DraftReport").getColumn("draftNote");
    Column attachmentInfo = schema.getTableMetadata("Attachments").getColumn("attachmentInfo");
    Column reportId = schema.getTableMetadata("Report").getColumn("reportId");
    Column publishedDate = schema.getTableMetadata("Report").getColumn("publishedDate");

    // file order reportId, draftNote(subclass), attachmentInfo(module), publishedDate -> positions
    assertEquals(reportId.getPosition() + 1, draftNote.getPosition().intValue());
    assertEquals(draftNote.getPosition() + 1, attachmentInfo.getPosition().intValue());
    assertEquals(attachmentInfo.getPosition() + 1, publishedDate.getPosition().intValue());

    // subclass column sits between the two root columns in the merged form
    assertEquals(
        List.of("reportId", "draftNote", "publishedDate"),
        nonSystemNames(schema.getTableMetadata("DraftReport").getColumns()));

    // module column sits between the two root columns in the root's module-merged form
    assertEquals(
        List.of("reportId", "attachmentInfo", "publishedDate"),
        nonSystemNames(schema.getTableMetadata("Report").getColumnsIncludingModules()));
  }

  @Test
  void diamondMergeOrder() throws Exception {
    List<String> firstParse =
        nonSystemNames(
            Emx2Yaml.fromBundle(bundleDir("diamond"))
                .schema()
                .getTableMetadata("FilledOutlined")
                .getColumns());

    assertEquals(
        List.of("shapeId", "shapeName", "fillColor", "strokeColor", "cornerRadius"), firstParse);

    List<String> secondParse =
        nonSystemNames(
            Emx2Yaml.fromBundle(bundleDir("diamond"))
                .schema()
                .getTableMetadata("FilledOutlined")
                .getColumns());
    assertEquals(firstParse, secondParse);
  }

  @Test
  void multiParentOrder() throws Exception {
    List<String> inheritNames =
        Emx2Yaml.fromBundle(bundleDir("diamond"))
            .schema()
            .getTableMetadata("FilledOutlined")
            .getInheritNames();

    assertEquals(List.of("Filled", "Outlined"), inheritNames);
    assertEquals("Filled", inheritNames.get(0));
  }

  @Test
  void enumAndModuleAxes() throws Exception {
    Emx2YamlBundle parsed = Emx2Yaml.fromBundle(bundleDir("axes"));
    TableMetadata subject = parsed.schema().getTableMetadata("Subject");

    assertEquals(ColumnType.ENUM, subject.getColumn("sex").getColumnType());
    assertEquals(List.of("male", "female", "unknown"), subject.getColumn("sex").getValues());
    assertEquals(ColumnType.MODULE_ARRAY, subject.getColumn("subgroups").getColumnType());
    assertEquals(
        List.of("CockayneSyndrome", "Trichothiodystrophy"),
        subject.getColumn("subgroups").getValues());
    assertEquals(ColumnType.MODULE, subject.getColumn("assay").getColumnType());
    assertTrue(
        subject.getColumn("assay").getValues() == null
            || subject.getColumn("assay").getValues().isEmpty());

    Map<String, String> firstExport = Emx2Yaml.toBundleFiles(parsed);
    SchemaMetadata reparsed = Emx2Yaml.fromBundleFiles(firstExport).schema();
    TableMetadata reparsedSubject = reparsed.getTableMetadata("Subject");
    assertEquals(
        List.of("male", "female", "unknown"), reparsedSubject.getColumn("sex").getValues());
    assertEquals(
        List.of("CockayneSyndrome", "Trichothiodystrophy"),
        reparsedSubject.getColumn("subgroups").getValues());
    assertTrue(
        reparsedSubject.getColumn("assay").getValues() == null
            || reparsedSubject.getColumn("assay").getValues().isEmpty());

    assertEquals(firstExport, Emx2Yaml.toBundleFiles(Emx2Yaml.fromBundleFiles(firstExport)));
  }

  @Test
  void headingSplice() throws Exception {
    Emx2YamlBundle parsed = Emx2Yaml.fromBundle(bundleDir("reuse"));
    TableMetadata resources = parsed.schema().getTableMetadata("Resources");

    assertEquals(
        List.of("id", "familyName", "contactDetails", "email", "phone", "orcid", "sex"),
        nonSystemNames(resources.getColumns()));

    Column heading = resources.getColumn("contactDetails");
    assertTrue(heading.isHeading());
    assertEquals("hasContact", heading.getVisible());

    Column email = resources.getColumn("email");
    assertEquals(ColumnType.EMAIL, email.getColumnType());
    assertNull(
        email.getVisible(), "visible cascade is engine behavior, not materialized on columns");

    assertEquals(heading.getPosition() + 1, resources.getColumn("email").getPosition().intValue());
    assertEquals(
        resources.getColumn("orcid").getPosition() + 1,
        resources.getColumn("sex").getPosition().intValue());

    Map<String, String> firstExport = Emx2Yaml.toBundleFiles(parsed);
    Map<String, String> secondExport =
        Emx2Yaml.toBundleFiles(Emx2Yaml.fromBundleFiles(firstExport));
    assertEquals(firstExport, secondExport);

    TableMetadata reparsed =
        Emx2Yaml.fromBundleFiles(firstExport).schema().getTableMetadata("Resources");
    assertEquals(
        List.of("id", "familyName", "contactDetails", "email", "phone", "orcid", "sex"),
        nonSystemNames(reparsed.getColumns()));
    assertTrue(reparsed.getColumn("contactDetails").isHeading());
    assertEquals("hasContact", reparsed.getColumn("contactDetails").getVisible());
  }

  @Test
  void formatVersionSkew() {
    Map<String, String> files =
        Map.of("molgenis.yaml", "formatVersion: 999\ntables:\n- file: tables/Missing.yaml\n");

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> Emx2Yaml.fromBundleFiles(files));

    String message = exception.getMessage();
    assertTrue(message.contains("999"), message);
    assertTrue(message.toLowerCase().contains("formatversion"), message);
    assertTrue(message.contains("newer"), message);
  }
}
