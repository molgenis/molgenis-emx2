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
    // a table file's own import resolves inside that file
    SchemaMetadata resolved =
        Emx2Yaml.fromBundleFiles(
                Map.of(
                    "molgenis.yaml",
                    "tables:\n- tables/T.yaml\n",
                    "shared/narrow.yaml",
                    "columns:\n- name: email\n  type: email\n",
                    "tables/T.yaml",
                    "name: T\nimports: [shared/narrow.yaml]\ncolumns:\n- name: id\n  key: 1\n- email\n"))
            .schema();
    assertEquals(
        ColumnType.EMAIL,
        resolved.getTableMetadata("T").getColumn("email").getColumnType(),
        "a table file's own import must resolve inside that file");

    // a bundle-level import does NOT resolve inside a table file (per-file import locality)
    MolgenisException notLocal =
        parseError(
            Map.of(
                "molgenis.yaml",
                "imports: [shared/narrow.yaml]\ntables:\n- tables/T.yaml\n",
                "shared/narrow.yaml",
                "columns:\n- name: email\n  type: email\n",
                "tables/T.yaml",
                "name: T\ncolumns:\n- name: id\n  key: 1\n- email\n"));
    assertTrue(notLocal.getMessage().contains("email"), notLocal.getMessage());
    assertTrue(notLocal.getMessage().contains("tables/T.yaml"), notLocal.getMessage());
    assertTrue(notLocal.getMessage().contains("line"), notLocal.getMessage());
    assertTrue(notLocal.getMessage().contains("column"), notLocal.getMessage());

    // two of the table's own imports defining the same name are ambiguous
    MolgenisException ambiguous =
        parseError(
            Map.of(
                "molgenis.yaml",
                "tables:\n- tables/T.yaml\n",
                "shared/a.yaml",
                "columns:\n- name: email\n  type: email\n",
                "shared/b.yaml",
                "columns:\n- name: email\n  type: string\n",
                "tables/T.yaml",
                "name: T\nimports: [shared/a.yaml, shared/b.yaml]\ncolumns:\n- name: id\n  key: 1\n- email\n"));
    assertTrue(ambiguous.getMessage().contains("email"), ambiguous.getMessage());
    assertTrue(ambiguous.getMessage().toLowerCase().contains("ambiguous"), ambiguous.getMessage());

    // an unresolvable reference names its file, line and column
    MolgenisException unresolvable =
        parseError(
            Map.of(
                "molgenis.yaml",
                "tables:\n- tables/T.yaml\n",
                "shared/common.yaml",
                "columns:\n- name: email\n  type: email\n",
                "tables/T.yaml",
                "name: T\nimports: [shared/common.yaml]\ncolumns:\n- name: id\n  key: 1\n- nope\n"));
    assertTrue(unresolvable.getMessage().contains("nope"), unresolvable.getMessage());
    assertTrue(unresolvable.getMessage().contains("tables/T.yaml"), unresolvable.getMessage());
    assertTrue(unresolvable.getMessage().contains("line"), unresolvable.getMessage());
    assertTrue(unresolvable.getMessage().contains("column"), unresolvable.getMessage());

    MolgenisException collision =
        parseError(
            Map.of(
                "molgenis.yaml",
                "tables:\n- tables/T.yaml\n",
                "shared/common.yaml",
                "columns:\n- name: email\n  type: email\n",
                "tables/T.yaml",
                "name: T\nimports: [shared/common.yaml]\ncolumns:\n- name: id\n  key: 1\n- name: email\n  type: string\n"));
    assertTrue(collision.getMessage().contains("email"), collision.getMessage());

    MolgenisException duplicate =
        parseError(
            Map.of(
                "molgenis.yaml",
                "tables:\n- tables/T.yaml\n",
                "shared/common.yaml",
                "columns:\n- name: email\n  type: email\n",
                "tables/T.yaml",
                "name: T\nimports: [shared/common.yaml]\ncolumns:\n- name: id\n  key: 1\n- email\n- email\n"));
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
                    "tables:\n- tables/T.yaml\nadditionalSchemas:\n  Shared:\n"
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
                        "tables:\n- tables/T.yaml\nadditionalSchemas:\n  Shared:\n"
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
                        "tables:\n- tables/T.yaml\nadditionalSchemas:\n  Shared:\n"
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
                        "tables:\n- tables/T.yaml\nadditionalSchemas:\n  B:\n"
                            + "    bundle: b/molgenis.yaml\n",
                        "tables/T.yaml",
                        "name: T\ncolumns:\n- name: id\n  key: 1\n",
                        "b/molgenis.yaml",
                        "additionalSchemas:\n  A:\n    bundle: ../molgenis.yaml\n")));
    assertTrue(cycle.getMessage().toLowerCase().contains("cycle"), cycle.getMessage());
  }

  @Test
  void ontologyRefTargetMustBeOntology() {
    // 'kind' is an ontology column but points at a data table -> loud error, path + position
    MolgenisException error =
        parseError(
            Map.of(
                "molgenis.yaml",
                "tables:\n"
                    + "- name: Person\n  columns:\n  - name: id\n    key: 1\n"
                    + "- name: Event\n  columns:\n  - name: id\n    key: 1\n"
                    + "  - name: kind\n    type: ontology\n    refTable: Person\n"));
    assertTrue(error.getMessage().contains("kind"), error.getMessage());
    assertTrue(error.getMessage().contains("Person"), error.getMessage());
    assertTrue(error.getMessage().toLowerCase().contains("ontology"), error.getMessage());
    assertTrue(error.getMessage().contains("line"), error.getMessage());
    assertTrue(error.getMessage().contains("column"), error.getMessage());
  }

  @Test
  void disjointRootsRejected() {
    // Child extends two independent roots (both empty-extends ancestors) -> loud error
    MolgenisException error =
        parseError(
            Map.of(
                "molgenis.yaml",
                "tables:\n"
                    + "- name: RootA\n  columns:\n  - name: id\n    key: 1\n"
                    + "- name: RootB\n  columns:\n  - name: code\n    key: 1\n"
                    + "- name: Child\n  extends: [RootA, RootB]\n  columns:\n  - name: x\n"));
    assertTrue(error.getMessage().contains("Child"), error.getMessage());
    assertTrue(error.getMessage().contains("RootA"), error.getMessage());
    assertTrue(error.getMessage().contains("RootB"), error.getMessage());
  }

  @Test
  void reuseOrDefine() {
    MolgenisException refinement =
        parseError(
            Map.of(
                "molgenis.yaml",
                "tables:\n- tables/T.yaml\n",
                "shared/common.yaml",
                "columns:\n- heading: contactDetails\n  visible: hasContact\n- name: email\n  type: email\n",
                "tables/T.yaml",
                "name: T\nimports: [shared/common.yaml]\ncolumns:\n- name: id\n  key: 1\n- name: contactDetails\n  visible: always\n"));
    assertTrue(refinement.getMessage().contains("contactDetails"), refinement.getMessage());
  }
}
