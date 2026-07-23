package org.molgenis.emx2.io.emx2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.io.emx2.MigrationPlan.ColumnRef;
import org.molgenis.emx2.io.emx2.MigrationPlan.ColumnRename;

class ModelDiffTest {

  private static final String SCHEMA = "ModelDiffTest";
  private static final String PERSON = "Person";
  private static final String SURNAME = "surname";
  private static final String LAST_NAME = "last_name";
  private static final String FAMILY_NAME = "familyName";
  private static final String MIDDLE_NAME = "middleName";

  private static SchemaMetadata schemaWithPerson(String... columnNames) {
    SchemaMetadata schema = new SchemaMetadata(SCHEMA);
    TableMetadata person = new TableMetadata(PERSON);
    schema.create(person);
    person.add(column("id").setKey(1));
    for (String columnName : columnNames) {
      person.add(column(columnName));
    }
    return schema;
  }

  @Test
  void renameChainInference() {
    // live schema is two versions old: still carries the oldest name 'surname'
    SchemaMetadata live = schemaWithPerson(SURNAME);

    // desired schema renamed surname -> last_name -> familyName across two versions
    SchemaMetadata desired = schemaWithPerson(FAMILY_NAME);
    Emx2YamlBundle bundle =
        new Emx2YamlBundle(
            desired,
            1,
            "3.0.0",
            Map.of(),
            Map.of(PERSON, Map.of(FAMILY_NAME, List.of(LAST_NAME, SURNAME))));

    MigrationPlan plan = ModelDiff.diff(bundle, live);

    // matched the two-versions-old live name -> RENAME
    assertTrue(
        plan.columnRenames().contains(new ColumnRename(PERSON, SURNAME, FAMILY_NAME)),
        "expected RENAME surname->familyName, got renames=" + plan.columnRenames());
    // not DROP+ADD
    assertFalse(
        plan.columnAdds().contains(new ColumnRef(PERSON, FAMILY_NAME)),
        "familyName must not be an ADD, got adds=" + plan.columnAdds());
    assertFalse(
        plan.columnDrops().contains(new ColumnRef(PERSON, SURNAME)),
        "surname must not be a DROP, got drops=" + plan.columnDrops());
    assertTrue(plan.errors().isEmpty(), "expected no errors, got " + plan.errors());
  }

  @Test
  void renameCollision() {
    // 'surname' is still live AND still desired (a distinct column), while
    // familyName claims 'surname' as a previous name -> ambiguous, must error
    SchemaMetadata live = schemaWithPerson(SURNAME);

    SchemaMetadata desired = schemaWithPerson(SURNAME, FAMILY_NAME);
    Emx2YamlBundle bundle =
        new Emx2YamlBundle(
            desired, 1, "3.0.0", Map.of(), Map.of(PERSON, Map.of(FAMILY_NAME, List.of(SURNAME))));

    MigrationPlan plan = ModelDiff.diff(bundle, live);

    assertEquals(1, plan.errors().size(), "expected one collision error, got " + plan.errors());
    String error = plan.errors().get(0);
    assertTrue(error.contains(FAMILY_NAME), error);
    assertTrue(error.contains(SURNAME), error);
    assertTrue(error.contains(PERSON), error);
    // no mis-rename
    assertTrue(
        plan.columnRenames().isEmpty(),
        "collision must not produce a rename, got " + plan.columnRenames());
    // no silent drop of the still-live/still-desired name
    assertFalse(
        plan.columnDrops().contains(new ColumnRef(PERSON, SURNAME)),
        "surname must not be silently dropped, got " + plan.columnDrops());
  }

  @Test
  void additiveAbsenceKeepsColumn() {
    // live carries a column the desired document does not mention at all
    SchemaMetadata live = schemaWithPerson(SURNAME);
    SchemaMetadata desired = schemaWithPerson();
    Emx2YamlBundle bundle = new Emx2YamlBundle(desired, 1, "1.0.0");

    MigrationPlan plan = ModelDiff.diff(bundle, live);

    // absence never deletes: the unmentioned live column survives, no drop, no error
    assertFalse(
        plan.columnDrops().contains(new ColumnRef(PERSON, SURNAME)),
        "an unmentioned live column must not be dropped, got " + plan.columnDrops());
    assertTrue(plan.errors().isEmpty(), "expected no errors, got " + plan.errors());
  }

  @Test
  void dropMarkerDrops() {
    // both surname and middleName are absent from desired, but only surname is drop-marked
    SchemaMetadata live = schemaWithPerson(SURNAME, MIDDLE_NAME);
    SchemaMetadata desired = schemaWithPerson();
    ModelDrops drops = new ModelDrops(Set.of(), Map.of(PERSON, List.of(SURNAME)));
    Emx2YamlBundle bundle = new Emx2YamlBundle(desired, 1, "1.0.0", Map.of(), Map.of(), drops);

    MigrationPlan plan = ModelDiff.diff(bundle, live);

    // the marker drives the drop
    assertTrue(
        plan.columnDrops().contains(new ColumnRef(PERSON, SURNAME)),
        "a drop-marked column must be dropped, got " + plan.columnDrops());
    // the unmarked absent column is left alone (additive)
    assertFalse(
        plan.columnDrops().contains(new ColumnRef(PERSON, MIDDLE_NAME)),
        "an unmarked absent column must not be dropped, got " + plan.columnDrops());
  }
}
