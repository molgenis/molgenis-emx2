package org.molgenis.emx2.io;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("slow")
public class TestImportEmptyAutoId {

    static Database db;

    @BeforeAll
    public static void setup() {
        db = TestDatabaseFactory.getTestDatabase();
    }

    @Test
    public void testImportEmptyAutoIdFieldXls() {
        ClassLoader classLoader = getClass().getClassLoader();
        File xlsFile = new File(classLoader.getResource("test_import_empty_auto_id.xlsx").getFile());

        Schema schema = db.getSchema("pet store");
        MolgenisIO.importFromExcelFile(xlsFile.toPath(), schema, true);

        List<Row> rows = schema.getTable("Order").retrieveRows();
        Row finalRow = rows.get(rows.size() -1);

        assertNotNull(finalRow.getString("orderId"));
    }

}
