package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChangeLogUtilsTest {
  @Test
  void testBuildAuditTrigger() {
    String expectedTrigger =
        """
                        CREATE TRIGGER Pet_audit
                        AFTER INSERT OR UPDATE OR DELETE ON "pet store"."Pet"
                            FOR EACH ROW EXECUTE FUNCTION "MOLGENIS".mg_changelog_audit();
                          """;
    assertEquals(
        expectedTrigger.strip(), ChangeLogUtils.buildAuditTrigger("pet store", "Pet").strip());
  }

  @Test
  void testBuildAuditTriggerWithSpaceInTableName() {
    String expectedTrigger =
        """
                        CREATE TRIGGER My_pets_audit
                        AFTER INSERT OR UPDATE OR DELETE ON "pet store"."My pets"
                            FOR EACH ROW EXECUTE FUNCTION "MOLGENIS".mg_changelog_audit();
                          """;
    assertEquals(
        expectedTrigger.strip(), ChangeLogUtils.buildAuditTrigger("pet store", "My pets").strip());
  }

  @Test
  void testRemoveAuditTrigger() {
    assertEquals(
        "DROP TRIGGER IF EXISTS my_table_audit ON \"my schema\".\"my table\" CASCADE",
        ChangeLogUtils.buildAuditTriggerRemove("my schema", "my table").strip());
  }
}
