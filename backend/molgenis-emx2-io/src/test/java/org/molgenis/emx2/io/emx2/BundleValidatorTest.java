package org.molgenis.emx2.io.emx2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.SchemaMetadata;

class BundleValidatorTest {

  private static MolgenisException parseError(Map<String, String> files) {
    return assertThrows(MolgenisException.class, () -> Emx2Yaml.fromBundleFiles(files));
  }

  @Test
  void referenceScoping() {
    SchemaMetadata shadowed =
        Emx2Yaml.fromBundleFiles(
                Map.of(
                    "molgenis.yaml",
                    "imports: [shared/wide.yaml]\ntables:\n- file: tables/T.yaml\n",
                    "shared/wide.yaml",
                    "columns:\n- name: email\n  type: string\n",
                    "shared/narrow.yaml",
                    "columns:\n- name: email\n  type: email\n",
                    "tables/T.yaml",
                    "name: T\nimports: [shared/narrow.yaml]\ncolumns:\n- name: id\n  key: 1\n- email\n"))
            .schema();
    assertEquals(
        ColumnType.EMAIL,
        shadowed.getTableMetadata("T").getColumn("email").getColumnType(),
        "table-level import must shadow bundle-level for the same name");

    MolgenisException ambiguous =
        parseError(
            Map.of(
                "molgenis.yaml",
                "imports: [shared/a.yaml, shared/b.yaml]\ntables:\n- file: tables/T.yaml\n",
                "shared/a.yaml",
                "columns:\n- name: email\n  type: email\n",
                "shared/b.yaml",
                "columns:\n- name: email\n  type: string\n",
                "tables/T.yaml",
                "name: T\ncolumns:\n- name: id\n  key: 1\n- email\n"));
    assertTrue(ambiguous.getMessage().contains("email"), ambiguous.getMessage());
    assertTrue(ambiguous.getMessage().toLowerCase().contains("ambiguous"), ambiguous.getMessage());

    MolgenisException unresolvable =
        parseError(
            Map.of(
                "molgenis.yaml",
                "imports: [shared/common.yaml]\ntables:\n- file: tables/T.yaml\n",
                "shared/common.yaml",
                "columns:\n- name: email\n  type: email\n",
                "tables/T.yaml",
                "name: T\ncolumns:\n- name: id\n  key: 1\n- nope\n"));
    assertTrue(unresolvable.getMessage().contains("nope"), unresolvable.getMessage());
    assertTrue(unresolvable.getMessage().contains("tables/T.yaml"), unresolvable.getMessage());
    assertTrue(unresolvable.getMessage().contains("line"), unresolvable.getMessage());
    assertTrue(unresolvable.getMessage().contains("column"), unresolvable.getMessage());

    MolgenisException collision =
        parseError(
            Map.of(
                "molgenis.yaml",
                "imports: [shared/common.yaml]\ntables:\n- file: tables/T.yaml\n",
                "shared/common.yaml",
                "columns:\n- name: email\n  type: email\n",
                "tables/T.yaml",
                "name: T\ncolumns:\n- name: id\n  key: 1\n- name: email\n  type: string\n"));
    assertTrue(collision.getMessage().contains("email"), collision.getMessage());

    MolgenisException duplicate =
        parseError(
            Map.of(
                "molgenis.yaml",
                "imports: [shared/common.yaml]\ntables:\n- file: tables/T.yaml\n",
                "shared/common.yaml",
                "columns:\n- name: email\n  type: email\n",
                "tables/T.yaml",
                "name: T\ncolumns:\n- name: id\n  key: 1\n- email\n- email\n"));
    assertTrue(duplicate.getMessage().contains("email"), duplicate.getMessage());
    assertTrue(duplicate.getMessage().contains("tables/T.yaml"), duplicate.getMessage());
    assertTrue(duplicate.getMessage().contains("line"), duplicate.getMessage());
    assertTrue(duplicate.getMessage().contains("column"), duplicate.getMessage());
  }

  @Test
  void membersRejected() {
    SchemaMetadata roleDefaults =
        BundleValidator.validate(
                Map.of(
                    "molgenis.yaml",
                    "tables:\n- file: tables/T.yaml\nschemas:\n  Shared:\n"
                        + "    bundle: shared/molgenis.yaml\n    permissions:\n      view: anonymous\n",
                    "tables/T.yaml",
                    "name: T\ncolumns:\n- name: id\n  key: 1\n"))
            .schema();
    assertEquals(
        ColumnType.STRING, roleDefaults.getTableMetadata("T").getColumn("id").getColumnType());

    MolgenisException memberBlock =
        assertThrows(
            MolgenisException.class,
            () ->
                BundleValidator.validate(
                    Map.of(
                        "molgenis.yaml",
                        "tables:\n- file: tables/T.yaml\nschemas:\n  Shared:\n"
                            + "    bundle: shared/molgenis.yaml\n    members:\n"
                            + "    - alice@example.com\n",
                        "tables/T.yaml",
                        "name: T\ncolumns:\n- name: id\n  key: 1\n")));
    assertTrue(memberBlock.getMessage().contains("member"), memberBlock.getMessage());
    assertTrue(memberBlock.getMessage().contains("Shared"), memberBlock.getMessage());
    assertTrue(memberBlock.getMessage().contains("line"), memberBlock.getMessage());

    MolgenisException emailGrantee =
        assertThrows(
            MolgenisException.class,
            () ->
                BundleValidator.validate(
                    Map.of(
                        "molgenis.yaml",
                        "tables:\n- file: tables/T.yaml\nschemas:\n  Shared:\n"
                            + "    bundle: shared/molgenis.yaml\n    permissions:\n"
                            + "      edit: bob@example.com\n",
                        "tables/T.yaml",
                        "name: T\ncolumns:\n- name: id\n  key: 1\n")));
    assertTrue(emailGrantee.getMessage().contains("bob@example.com"), emailGrantee.getMessage());
    assertTrue(emailGrantee.getMessage().contains("member"), emailGrantee.getMessage());
  }

  @Test
  void companionCycleDetected() {
    MolgenisException cycle =
        assertThrows(
            MolgenisException.class,
            () ->
                BundleValidator.validate(
                    Map.of(
                        "molgenis.yaml",
                        "tables:\n- file: tables/T.yaml\nschemas:\n  B:\n"
                            + "    bundle: b/molgenis.yaml\n",
                        "tables/T.yaml",
                        "name: T\ncolumns:\n- name: id\n  key: 1\n",
                        "b/molgenis.yaml",
                        "schemas:\n  A:\n    bundle: ../molgenis.yaml\n")));
    assertTrue(cycle.getMessage().toLowerCase().contains("cycle"), cycle.getMessage());
  }

  @Test
  void reuseOrDefine() {
    MolgenisException refinement =
        parseError(
            Map.of(
                "molgenis.yaml",
                "imports: [shared/common.yaml]\ntables:\n- file: tables/T.yaml\n",
                "shared/common.yaml",
                "columns:\n- heading: contactDetails\n  visible: hasContact\n- name: email\n  type: email\n",
                "tables/T.yaml",
                "name: T\ncolumns:\n- name: id\n  key: 1\n- name: contactDetails\n  visible: always\n"));
    assertTrue(refinement.getMessage().contains("contactDetails"), refinement.getMessage());
  }
}
