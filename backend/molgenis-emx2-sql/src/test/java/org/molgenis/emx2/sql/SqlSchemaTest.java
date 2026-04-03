package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.molgenis.emx2.MolgenisException;

@Tag("slow")
// todo move to sql
class SqlSchemaTest {
  @Mock SqlDatabase db;
  @Mock SqlSchemaMetadata metadata;

  @Test
  void getSettingValue() {
    metadata = mock(SqlSchemaMetadata.class);
    db = mock(SqlDatabase.class);
    String testSetting = "TEST_SETTING";
    String testValue = "TEST_VALUE";
    when(metadata.getSetting(testSetting)).thenReturn(testValue);
    SqlSchema schema = new SqlSchema(db, metadata);

    String result = schema.getSettingValue(testSetting);

    String expectedResult = "TEST_VALUE";
    assertEquals(expectedResult, result);
  }

  @Test
  void getSettingValueGetNonExistingValue() {
    metadata = mock(SqlSchemaMetadata.class);
    db = mock(SqlDatabase.class);
    String testSetting = "NON_EXISTING_SETTING";
    when(metadata.getSetting(testSetting)).thenReturn(null);
    SqlSchema schema = new SqlSchema(db, metadata);
    assertThrows(MolgenisException.class, () -> schema.getSettingValue(testSetting));
  }
}
