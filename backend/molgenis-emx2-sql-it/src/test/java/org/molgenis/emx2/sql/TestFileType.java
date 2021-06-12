package org.molgenis.emx2.sql;

import static org.junit.Assert.*;

import java.io.File;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;

public class TestFileType {
  private static Database db;
  private static Schema schema;

  @BeforeClass
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(TestFileType.class.getSimpleName());
  }

  @Test
  public void test3() {
    Table t =
        schema.create(
            TableMetadata.table(
                "test1",
                Column.column("id").setPkey(),
                Column.column("image").setType(ColumnType.FILE)));
    File image = getFile();
    t.insert(new Row("id", 1, "image", image));

    Assert.assertEquals(
        (Integer) 37458,
        t.query()
            .select(SelectColumn.s("image", SelectColumn.s("size")))
            .retrieveRows()
            .get(0)
            .getInteger("image_size"));

    String result =
        t.query()
            .select(
                SelectColumn.s(
                    "image",
                    SelectColumn.s("size"),
                    SelectColumn.s("extension"),
                    SelectColumn.s("mimetype")))
            .retrieveJSON();
    System.out.println(result);
    assertTrue(result.contains("37458"));
  }

  @Test
  public void testBinaryFileWrapper() {
    BinaryFileWrapper w = new BinaryFileWrapper(getFile());
    Assert.assertEquals("image/png", w.getMimeType());
    Assert.assertEquals("png", w.getExtension());
    Assert.assertEquals(37458, w.getSize());
    assertNotNull(w.getContents());
    Assert.assertEquals(37458, w.getContents().length);
  }

  @Test
  public void testRowWithFileType() {
    Row r = new Row("image", getFile());
    Assert.assertEquals("png", r.getString("image_extension"));
  }

  private File getFile() {
    ClassLoader classLoader = getClass().getClassLoader();
    return new File(classLoader.getResource("testfiles/molgenis.png").getFile());
  }
}
