package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.*;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestRefBack {

  private static Schema schema;

  @BeforeAll
  public static void setUp() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestRefBack.class.getSimpleName());
  }

  @Test
  public void testRefArrayBack() {
    execute(REF_ARRAY);
  }

  public void execute(ColumnType refArrayOrMref) {

    // Table Parts(partname)
    Table parts = schema.create(table("Parts").add(column("partname").setPkey()));

    // Table Products(productname, parts->ref(Parts))
    Table products =
        schema.create(
            table("Products")
                .add(column("productname").setPkey())
                .add(column("parts").setType(refArrayOrMref).setRefTable("Parts")));

    parts.insert(new Row().set("partname", "smallscreen"));
    parts.insert(new Row().set("partname", "bigscreen"));
    parts.insert(new Row().set("partname", "smallbutton"));
    parts.insert(new Row().set("partname", "battery"));

    // ref_array entry via 'products', business as usual
    products.insert(new Row().set("productname", "bigphone"));
    products.insert(
        new Row()
            .set("productname", "smallphone")
            .set("parts", new String[] {"smallscreen", "smallbutton"}));

    // add refback, Table Parts(partname,products->refback(product))
    parts
        .getMetadata()
        .add(column("products").setType(REFBACK).setRefTable("Products").setRefBack("parts"));

    // if refback is set to null all are remove
    parts.save(new Row().set("partname", "bigscreen").set("products", null));

    // so now bigphone.parts = [] or null
    assertNull(
        products
            .query()
            .where(f("productname", EQUALS, "bigphone"))
            .retrieveRows()
            .get(0)
            .getStringArray("parts"),
        "bigscreen");

    // contains_any
    String result =
        products
            .select(s("productname"), s("parts", s("partname")))
            .where(f("parts", MATCH_ANY, "smallscreen"))
            .retrieveJSON();
    assertTrue(result.contains("smallphone"));
    assertFalse(result.contains("bigphone"));

    result =
        parts
            .select(s("partname"), s("products", s("productname")))
            .where(f("products", MATCH_ANY, "smallphone"))
            .retrieveJSON();
    assertTrue(result.contains("smallscreen"));
    assertTrue(result.contains("smallbutton"));
    assertFalse(result.contains("bigscreen"));

    result =
        parts
            .select(s("partname"), s("products", s("productname")))
            .where(f("products", MATCH_ANY, "bigphone"))
            .retrieveJSON();
    assertTrue(result.contains("null"));

    // now multiple
    parts.update(
        new Row().set("partname", "bigscreen").set("products", "bigphone"),
        new Row()
            .set("partname", "battery")
            .set("products", new String[] {"smallphone", "bigphone"}));

    // via insert, bigphone and smallphone products should now have headphones as part, i.e.
    // bigphone=bigscreen,battery,headphone,
    // smallphone=battery+headphone+smallscreen+smallbutton
    parts.save(new Row().set("partname", "headphones").set("products", "bigphone,smallphone"));

    List<Row> pTest = products.query().orderBy("productname").retrieveRows();

    assertEquals(2, pTest.size());
    assertEquals("bigphone", pTest.get(0).getString("productname"));
    assertEquals("smallphone", pTest.get(1).getString("productname"));
    assertEquals(2, pTest.get(1).getStringArray("parts").length);

    Query query =
        products.select(s("productname"), s("parts", s("partname")), s("parts_agg", s("count")));
    System.out.println(query.retrieveJSON());
    query =
        parts.select(s("partname"), s("products", s("productname")), s("products_agg", s("count")));

    result =
        parts
            .select(s("partname"), s("products", s("productname")))
            .where(f("products", MATCH_ALL, "smallphone"))
            .retrieveJSON();
    assertTrue(result.contains("smallscreen"));
    assertTrue(result.contains("smallbutton"));
    assertFalse(result.contains("battery"));

    System.out.println(query.retrieveJSON());

    query = parts.agg(s("count"));
    assertTrue(query.retrieveJSON().contains("\"count\": 5"));

    query = products.agg(s("count"));
    assertTrue(query.retrieveJSON().contains("\"count\": 2"));

    query = products.select(s("parts_agg", s("count"))).where(f("productname", EQUALS, "bigphone"));

    query = products.select(s("parts", s("partname")));
    System.out.println(query.retrieveJSON());

    // delete
    parts.delete(new Row().set("partname", "headphones"));
    assertEquals(
        2,
        products
            .query()
            .orderBy("productname")
            .retrieveRows()
            .get(1)
            .getStringArray("parts")
            .length);

    // delete
    products.delete(new Row().set("productname", "bigphone"));

    // check
    assertEquals(1, products.query().orderBy("productname").retrieveRows().size());
  }

  @Test
  public void testRefBack() {

    Table users = schema.create(table("User").add(column("username").setPkey()));

    Table posts =
        schema.create(
            table("Posts")
                .add(column("title").setPkey())
                .add(column("tags").setType(ONTOLOGY).setRefTable("Tags"))
                .add(column("user").setType(REF).setRefTable(users.getName())));

    users
        .getMetadata()
        .add(column("posts").setType(REFBACK).setRefTable(posts.getName()).setRefBack("user"));

    users.insert(new Row().set("username", "jack"));
    users.insert(new Row().set("username", "joe"));
    schema.getTable("Tags").insert(new Row().set("name", "green"));
    posts.insert(new Row().set("title", "joes post").set("user", "joe").set("tags", "green"));

    // add another post for jack, should result in 'posts(user=jack,title=jacks post)
    posts.insert(new Row().set("title", "jacks post").set("user", "jack"));

    // check select on posts
    assertEquals(
        1, // expect 'jacks post'
        users
            .query()
            .select(s("username"), s("posts"))
            .where(f("username", EQUALS, "jack"))
            .retrieveRows()
            .get(0)
            .getStringArray("posts")
            .length);

    // check filter on posts
    assertEquals(
        1, users.query().where(f("posts", f("title", EQUALS, "jacks post"))).retrieveRows().size());

    String result =
        users
            .select(s("username"), s("posts", s("title")))
            .where(f("posts", MATCH_ANY, "jacks post"))
            .retrieveJSON();
    assertTrue(result.contains("jacks"));

    result =
        users
            .select(s("username"), s("posts", s("title")))
            .where(f("posts", MATCH_ALL, "jacks post"))
            .retrieveJSON();
    assertTrue(result.contains("jacks"));

    // check graph query
    Query query = users.agg(s("count"));
    assertTrue(query.retrieveJSON().contains("\"count\": 2"));

    query =
        users
            .select(s("username"), s("posts", s("title")))
            .where(f("posts", f("title", EQUALS, "jacks post")));
    assertTrue(query.retrieveJSON().contains("jacks post"));

    query =
        users.select(
            s("username"), s("posts", s("title")).where(f("tags", f("name", EQUALS, "green"))));
    assertTrue(query.retrieveJSON().contains("joes post"));
    assertFalse(query.retrieveJSON().contains("jacks post"));

    query = users.agg(s("count")).where(f("posts", f("title", EQUALS, "jacks post")));
    assertTrue(query.retrieveJSON().contains("\"count\": 1"));

    // delete of user should fail as long as there are posts refering to this user, unless cascading
    // delete
    try {
      // users.delete(new Row().set("username", "jack"));
      // should not fail? fail("delete of user with reference from post should fail");
    } catch (Exception e) {
      // ok
    }

    // delete of a post should also remove it from users.posts
    posts.delete(new Row().set("title", "joes post"));

    // check, should now have one posts in posts array
    assertEquals(
        1,
        users
            .query()
            .select(s("username"), s("posts"))
            .where(f("username", EQUALS, "jack"))
            .retrieveRows()
            .get(0)
            .getStringArray("posts")
            .length);
  }

  @Test
  public void testRefBackUsingSubclasses() {
    schema.create(
        table("subject", column("id").setPkey()),
        table(
            "treatments",
            column("id").setPkey(),
            column("partOfSubject").setType(REF).setRefTable("subject")));
    // inherit
    schema.create(table("treatmentxyz", column("xyz")).setInheritName("treatments"));
    // add the refback
    schema
        .getTable("subject")
        .getMetadata()
        .add(
            column("treatxyz")
                .setType(REFBACK)
                .setRefTable("treatmentxyz")
                .setRefBack("partOfSubject"));
    schema.getTable("subject").insert(row("id", "s1"));
    schema.getTable("treatmentxyz").insert(row("id", "t1", "partOfSubject", "s1"));
    List<Row> subjects = schema.query("subject").retrieveRows();
    assertEquals("t1", subjects.get(0).getString("treatxyz"));
  }

  @Test
  public void testRefbackWithIncorrectRefColumn() {
    schema.create(
        table("subject_incorrect_ref", column("id").setPkey()),
        table(
            "treatments_incorrect_ref",
            column("id").setPkey(),
            column("partOfSubject").setType(REF).setRefTable("subject_incorrect_ref")));
    assertThrows(
        MolgenisException.class,
        () ->
            schema
                .getTable("subject_incorrect_ref")
                .getMetadata()
                .add(
                    column("treatment")
                        .setType(REFBACK)
                        .setRefTable("treatments_incorrect_ref")
                        .setRefBack("id")));
    ;
  }

  @Test
  public void testRefbackWithIncorrectRefTable() {
    schema.create(
        table("subject_incorrect_ref_table", column("id").setPkey()),
        table(
            "treatments_incorrect_ref_table",
            column("id").setPkey(),
            column("partOfDisease").setType(REF).setRefTable("diseases_incorrect_ref_table")),
        table(
            "diseases_incorrect_ref_table",
            column("id").setPkey(),
            column("partOfTreatment").setType(REF).setRefTable("treatments_incorrect_ref_table")));
    assertThrows(
        MolgenisException.class,
        () ->
            schema
                .getTable("subject_incorrect_ref_table")
                .getMetadata()
                .add(
                    column("disease")
                        .setType(REFBACK)
                        .setRefTable("diseases_incorrect_ref_table")
                        .setRefBack("partOfTreatment")));
  }

  @Test
  void partsColumnReturnsChildRowsLikeRefback() {
    Table resources = schema.create(table("PartsResources", column("name").setPkey()));
    Table tables =
        schema.create(
            table(
                "PartsTables",
                column("resource")
                    .setType(REF)
                    .setRefTable("PartsResources")
                    .setRequired(true)
                    .setKey(1),
                column("name").setRequired(true).setKey(1)));

    resources.insert(row("name", "resource1"), row("name", "resource2"));
    tables.insert(
        row("resource", "resource1", "name", "table1"),
        row("resource", "resource1", "name", "table2"),
        row("resource", "resource2", "name", "table3"));

    long foreignKeysBeforeParts = countForeignKeyConstraints("PartsResources");

    resources
        .getMetadata()
        .add(column("tables").setType(PARTS).setRefTable("PartsTables").setRefBack("resource"));

    assertEquals(foreignKeysBeforeParts, countForeignKeyConstraints("PartsResources"));

    String resource1Json =
        resources
            .select(s("name"), s("tables", s("name")), s("tables_agg", s("count")))
            .where(f("name", EQUALS, "resource1"))
            .retrieveJSON();
    assertTrue(resource1Json.contains("table1"));
    assertTrue(resource1Json.contains("table2"));
    assertFalse(resource1Json.contains("table3"));
    assertTrue(resource1Json.contains("\"count\": 2"));

    String resource2Json =
        resources
            .select(s("name"), s("tables", s("name")), s("tables_agg", s("count")))
            .where(f("name", EQUALS, "resource2"))
            .retrieveJSON();
    assertTrue(resource2Json.contains("table3"));
    assertFalse(resource2Json.contains("table1"));
    assertFalse(resource2Json.contains("table2"));
    assertTrue(resource2Json.contains("\"count\": 1"));

    resources
        .getMetadata()
        .add(
            column("tablesAsRefback")
                .setType(REFBACK)
                .setRefTable("PartsTables")
                .setRefBack("resource"));
    String refbackJson =
        resources.select(s("name"), s("tablesAsRefback", s("name"))).orderBy("name").retrieveJSON();
    String partsJson =
        resources.select(s("name"), s("tables", s("name"))).orderBy("name").retrieveJSON();
    assertEquals(refbackJson.replace("tablesAsRefback", "tables"), partsJson);
  }

  @Test
  void partsRejectsSelfReference() {
    schema.create(
        table(
            "PartsSelfReference",
            column("name").setPkey(),
            column("parent").setType(REF).setRefTable("PartsSelfReference")));

    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                schema
                    .getTable("PartsSelfReference")
                    .getMetadata()
                    .add(
                        column("children")
                            .setType(PARTS)
                            .setRefTable("PartsSelfReference")
                            .setRefBack("parent")));
    assertTrue(exception.getMessage().contains("cannot point at its own table"));
    assertTrue(exception.getMessage().contains("a row cannot be part of itself"));
    assertTrue(exception.getMessage().contains("PartsSelfReference"));
  }

  @Test
  void partsRejectsMultiValuedCounterpart() {
    schema.create(
        table("PartsMultiValuedParent", column("name").setPkey()),
        table(
            "PartsMultiValuedChild",
            column("name").setPkey(),
            column("parents").setType(REF_ARRAY).setRefTable("PartsMultiValuedParent")));

    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                schema
                    .getTable("PartsMultiValuedParent")
                    .getMetadata()
                    .add(
                        column("children")
                            .setType(PARTS)
                            .setRefTable("PartsMultiValuedChild")
                            .setRefBack("parents")));
    assertTrue(exception.getMessage().contains("must be a single valued ref"));
    assertTrue(exception.getMessage().contains("PartsMultiValuedChild.parents"));
    assertTrue(
        exception
            .getMessage()
            .contains(
                "so every 'PartsMultiValuedChild' row is part of exactly one 'PartsMultiValuedParent' row"));
  }

  @Test
  void partsAcceptsSelectAndRadioCounterpart() {
    schema.create(
        table("PartsFlavorParent", column("name").setPkey()),
        table(
            "PartsSelectChild",
            column("parent").setType(SELECT).setRefTable("PartsFlavorParent").setKey(1),
            column("name").setKey(1)),
        table(
            "PartsRadioChild",
            column("parent").setType(RADIO).setRefTable("PartsFlavorParent").setKey(1),
            column("name").setKey(1)));

    TableMetadata parent = schema.getTable("PartsFlavorParent").getMetadata();
    parent.add(
        column("selectChildren")
            .setType(PARTS)
            .setRefTable("PartsSelectChild")
            .setRefBack("parent"));
    parent.add(
        column("radioChildren").setType(PARTS).setRefTable("PartsRadioChild").setRefBack("parent"));

    TableMetadata reloaded = schema.getTable("PartsFlavorParent").getMetadata();
    assertEquals(PARTS, reloaded.getColumn("selectChildren").getColumnType());
    assertEquals(PARTS, reloaded.getColumn("radioChildren").getColumnType());
  }

  @Test
  void partsRejectsOptionalCounterpart() {
    schema.create(
        table("PartsOptionalParent", column("name").setPkey()),
        table(
            "PartsOptionalChild",
            column("name").setPkey(),
            column("parent").setType(REF).setRefTable("PartsOptionalParent")));

    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                schema
                    .getTable("PartsOptionalParent")
                    .getMetadata()
                    .add(
                        column("children")
                            .setType(PARTS)
                            .setRefTable("PartsOptionalChild")
                            .setRefBack("parent")));
    assertTrue(exception.getMessage().contains("must be required"));
    assertTrue(exception.getMessage().contains("PartsOptionalChild.parent"));
    assertTrue(
        exception
            .getMessage()
            .contains(
                "so every 'PartsOptionalChild' row belongs to exactly one 'PartsOptionalParent' row"));
  }

  @Test
  void partsRejectsCounterpartOutsideKey() {
    schema.create(
        table("PartsOutsideKeyParent", column("name").setPkey()),
        table(
            "PartsOutsideKeyChild",
            column("name").setPkey(),
            column("parent").setType(REF).setRefTable("PartsOutsideKeyParent").setRequired(true)));

    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                schema
                    .getTable("PartsOutsideKeyParent")
                    .getMetadata()
                    .add(
                        column("children")
                            .setType(PARTS)
                            .setRefTable("PartsOutsideKeyChild")
                            .setRefBack("parent")));
    assertTrue(
        exception
            .getMessage()
            .contains("must be part of the primary key of 'PartsOutsideKeyChild'"));
    assertTrue(exception.getMessage().contains("a key column marked key=1"));
    assertTrue(exception.getMessage().contains("PartsOutsideKeyChild.parent"));

    schema
        .getTable("PartsOutsideKeyChild")
        .getMetadata()
        .alterColumn(
            "parent",
            column("parent")
                .setType(REF)
                .setRefTable("PartsOutsideKeyParent")
                .setRequired(true)
                .setKey(1));
    schema
        .getTable("PartsOutsideKeyParent")
        .getMetadata()
        .add(
            column("children")
                .setType(PARTS)
                .setRefTable("PartsOutsideKeyChild")
                .setRefBack("parent"));

    assertEquals(
        PARTS,
        schema
            .getTable("PartsOutsideKeyParent")
            .getMetadata()
            .getColumn("children")
            .getColumnType());
  }

  @Test
  void partsRejectsCounterpartInSecondaryKeyOnly() {
    schema.create(
        table("PartsSecondaryKeyParent", column("name").setPkey()),
        table(
            "PartsSecondaryKeyChild",
            column("name").setPkey(),
            column("parent")
                .setType(REF)
                .setRefTable("PartsSecondaryKeyParent")
                .setRequired(true)
                .setKey(2)));

    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                schema
                    .getTable("PartsSecondaryKeyParent")
                    .getMetadata()
                    .add(
                        column("children")
                            .setType(PARTS)
                            .setRefTable("PartsSecondaryKeyChild")
                            .setRefBack("parent")));
    assertTrue(
        exception
            .getMessage()
            .contains("must be part of the primary key of 'PartsSecondaryKeyChild'"));
    assertTrue(exception.getMessage().contains("a key column marked key=1"));
    assertTrue(exception.getMessage().contains("PartsSecondaryKeyChild.parent"));
  }

  @Test
  void partsRejectsSecondPartsColumnTargetingSameChild() {
    schema.create(
        table("PartsFirstParent", column("name").setPkey()),
        table("PartsSecondParent", column("name").setPkey()),
        table(
            "PartsSharedChild",
            column("firstParent")
                .setType(REF)
                .setRefTable("PartsFirstParent")
                .setRequired(true)
                .setKey(1),
            column("secondParent")
                .setType(REF)
                .setRefTable("PartsSecondParent")
                .setRequired(true)
                .setKey(1),
            column("name").setRequired(true).setKey(1)));

    schema
        .getTable("PartsFirstParent")
        .getMetadata()
        .add(
            column("children")
                .setType(PARTS)
                .setRefTable("PartsSharedChild")
                .setRefBack("firstParent"));

    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                schema
                    .getTable("PartsSecondParent")
                    .getMetadata()
                    .add(
                        column("children")
                            .setType(PARTS)
                            .setRefTable("PartsSharedChild")
                            .setRefBack("secondParent")));
    assertTrue(exception.getMessage().contains("PartsFirstParent.children"));
    assertTrue(exception.getMessage().contains("PartsSecondParent.children"));
  }

  @Test
  void partsAllowsRepointingItsOwnRefBack() {
    schema.create(
        table("PartsRepointParent", column("name").setPkey()),
        table(
            "PartsRepointChild",
            column("firstLink")
                .setType(REF)
                .setRefTable("PartsRepointParent")
                .setRequired(true)
                .setKey(1),
            column("secondLink")
                .setType(REF)
                .setRefTable("PartsRepointParent")
                .setRequired(true)
                .setKey(1),
            column("name").setRequired(true).setKey(1)));

    TableMetadata parent = schema.getTable("PartsRepointParent").getMetadata();
    parent.add(
        column("children").setType(PARTS).setRefTable("PartsRepointChild").setRefBack("firstLink"));

    parent.alterColumn(
        "children",
        column("children")
            .setType(PARTS)
            .setRefTable("PartsRepointChild")
            .setRefBack("secondLink"));

    assertEquals(
        "secondLink",
        schema.getTable("PartsRepointParent").getMetadata().getColumn("children").getRefBack());
  }

  @Test
  void partsStillRejectsDuplicateAfterRepointing() {
    schema.create(
        table("PartsDuplicateFirstParent", column("name").setPkey()),
        table("PartsDuplicateSecondParent", column("name").setPkey()),
        table(
            "PartsDuplicateChild",
            column("firstLink")
                .setType(REF)
                .setRefTable("PartsDuplicateFirstParent")
                .setRequired(true)
                .setKey(1),
            column("secondLink")
                .setType(REF)
                .setRefTable("PartsDuplicateFirstParent")
                .setRequired(true)
                .setKey(1),
            column("otherParent")
                .setType(REF)
                .setRefTable("PartsDuplicateSecondParent")
                .setRequired(true)
                .setKey(1),
            column("name").setRequired(true).setKey(1)));

    TableMetadata firstParent = schema.getTable("PartsDuplicateFirstParent").getMetadata();
    firstParent.add(
        column("children")
            .setType(PARTS)
            .setRefTable("PartsDuplicateChild")
            .setRefBack("firstLink"));
    firstParent.alterColumn(
        "children",
        column("children")
            .setType(PARTS)
            .setRefTable("PartsDuplicateChild")
            .setRefBack("secondLink"));

    MolgenisException fromOtherTable =
        assertThrows(
            MolgenisException.class,
            () ->
                schema
                    .getTable("PartsDuplicateSecondParent")
                    .getMetadata()
                    .add(
                        column("children")
                            .setType(PARTS)
                            .setRefTable("PartsDuplicateChild")
                            .setRefBack("otherParent")));
    assertTrue(fromOtherTable.getMessage().contains("PartsDuplicateFirstParent.children"));

    MolgenisException fromSameTable =
        assertThrows(
            MolgenisException.class,
            () ->
                schema
                    .getTable("PartsDuplicateFirstParent")
                    .getMetadata()
                    .add(
                        column("moreChildren")
                            .setType(PARTS)
                            .setRefTable("PartsDuplicateChild")
                            .setRefBack("firstLink")));
    assertTrue(fromSameTable.getMessage().contains("PartsDuplicateFirstParent.children"));
  }

  @Test
  void partsRejectsSecondPartsColumnWithSameRefBack() {
    schema.create(
        table("PartsSameRefBackWhole", column("name").setPkey()),
        table(
            "PartsSameRefBackPart",
            column("whole")
                .setType(REF)
                .setRefTable("PartsSameRefBackWhole")
                .setRequired(true)
                .setKey(1),
            column("name").setRequired(true).setKey(1)));

    TableMetadata whole = schema.getTable("PartsSameRefBackWhole").getMetadata();
    whole.add(
        column("children").setType(PARTS).setRefTable("PartsSameRefBackPart").setRefBack("whole"));

    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                whole.add(
                    column("moreChildren")
                        .setType(PARTS)
                        .setRefTable("PartsSameRefBackPart")
                        .setRefBack("whole")));
    assertTrue(exception.getMessage().contains("PartsSameRefBackWhole.children"));
    assertTrue(exception.getMessage().contains("PartsSameRefBackWhole.moreChildren"));
    assertTrue(
        exception
            .getMessage()
            .contains("every 'PartsSameRefBackPart' row must belong to exactly one whole"));
  }

  @Test
  void partsValidationAllowsRenamingAndRepointingButNotDuplicating() {
    schema.create(
        table("PartsRenameWhole", column("name").setPkey()),
        table(
            "PartsRenamePart",
            column("firstLink")
                .setType(REF)
                .setRefTable("PartsRenameWhole")
                .setRequired(true)
                .setKey(1),
            column("secondLink")
                .setType(REF)
                .setRefTable("PartsRenameWhole")
                .setRequired(true)
                .setKey(1),
            column("name").setRequired(true).setKey(1)));

    TableMetadata whole = schema.getTable("PartsRenameWhole").getMetadata();
    whole.add(
        column("children").setType(PARTS).setRefTable("PartsRenamePart").setRefBack("firstLink"));

    Column renamed =
        new Column(
            whole,
            column("sections")
                .setType(PARTS)
                .setRefTable("PartsRenamePart")
                .setRefBack("firstLink"));
    Column renamedAndRepointed =
        new Column(
            whole,
            column("sections")
                .setType(PARTS)
                .setRefTable("PartsRenamePart")
                .setRefBack("secondLink"));

    assertDoesNotThrow(() -> SqlColumnExecutor.validateColumn(renamed, "children"));
    assertDoesNotThrow(() -> SqlColumnExecutor.validateColumn(renamedAndRepointed, "children"));

    MolgenisException addedAlongside =
        assertThrows(
            MolgenisException.class, () -> SqlColumnExecutor.validateColumn(renamed, null));
    assertTrue(addedAlongside.getMessage().contains("PartsRenameWhole.children"));
  }

  private static TableMetadata createPartsRelation(String prefix) {
    schema.create(
        table(prefix + "Whole", column("name").setPkey()),
        table(
            prefix + "Part",
            column("whole").setType(REF).setRefTable(prefix + "Whole").setRequired(true).setKey(1),
            column("name").setRequired(true).setKey(1)));
    schema
        .getTable(prefix + "Whole")
        .getMetadata()
        .add(column("children").setType(PARTS).setRefTable(prefix + "Part").setRefBack("whole"));
    return schema.getTable(prefix + "Part").getMetadata();
  }

  @Test
  void partsRejectsDroppingCounterpartFromKeyAfterwards() {
    TableMetadata part = createPartsRelation("PartsLaterKey");

    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                part.alterColumn(
                    "whole",
                    column("whole")
                        .setType(REF)
                        .setRefTable("PartsLaterKeyWhole")
                        .setRequired(true)));
    assertTrue(
        exception.getMessage().contains("must be part of the primary key of 'PartsLaterKeyPart'"));
    assertTrue(exception.getMessage().contains("a key column marked key=1"));
    assertTrue(exception.getMessage().contains("PartsLaterKeyPart.whole"));

    part.alterColumn(
        "whole",
        column("whole")
            .setType(REF)
            .setRefTable("PartsLaterKeyWhole")
            .setRequired(true)
            .setKey(1)
            .setDescription("still a valid counterpart"));
    assertEquals(1, schema.getTable("PartsLaterKeyPart").getMetadata().getColumn("whole").getKey());
  }

  @Test
  void partsRejectsWideningCounterpartToRefArrayAfterwards() {
    TableMetadata part = createPartsRelation("PartsLaterArray");

    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                part.alterColumn(
                    "whole",
                    column("whole")
                        .setType(REF_ARRAY)
                        .setRefTable("PartsLaterArrayWhole")
                        .setRequired(true)
                        .setKey(1)));
    assertTrue(exception.getMessage().contains("must be a single valued ref"));
    assertTrue(exception.getMessage().contains("PartsLaterArrayPart.whole"));
    assertTrue(
        exception
            .getMessage()
            .contains(
                "so every 'PartsLaterArrayPart' row is part of exactly one 'PartsLaterArrayWhole' row"));
  }

  private static long countForeignKeyConstraints(String tableName) {
    return ((SqlSchemaMetadata) schema.getMetadata())
            .getJooq()
            .resultQuery(
                "SELECT constraint_name FROM information_schema.table_constraints WHERE table_schema = {0} AND table_name = {1} AND constraint_type = 'FOREIGN KEY'",
                schema.getName(), tableName)
            .stream()
            .count();
  }

  @Test
  void testRefBackAndFile_fix4703() {
    Table test =
        schema.create(
            table(
                "test4703",
                column("id").setPkey(),
                column("parent").setType(REF).setRefTable("test4703"),
                column("uncles").setType(REF_ARRAY).setRefTable("test4703"),
                column("children").setType(REFBACK).setRefTable("test4703").setRefBack("parent"),
                column("cousins").setType(REFBACK).setRefTable("test4703").setRefBack("uncles"),
                column("photo").setType(FILE)));

    test.insert(
        row("id", "1"),
        row("id", "2", "parent", "1"),
        row("id", "3", "parent", "1"),
        row("id", "4", "parent", "1"),
        row("id", "5", "parent", "2", "uncles", "3,4"),
        row("id", "6", "parent", "2", "uncles", "3,4"));

    List<Row> result =
        test.query()
            .select(
                s("id"),
                s("parent", s("id")),
                s("children", s("id")),
                s("uncles", s("id")),
                s("cousins", s("id")),
                s("photo", s("id"), s("filename"), s("mimetype")))
            .retrieveRows();
    assertEquals(6, result.size());
  }
}
