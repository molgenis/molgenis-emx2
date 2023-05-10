package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ChangeLogUtilsTest {
  @Test
  public void testBuildProcessAuditFunction() {
    String expectedFunction =
        """
                        CREATE OR REPLACE FUNCTION "pet store"."process_Pet_audit"() RETURNS TRIGGER AS $Pet_audit$
                               BEGIN
                                   IF (TG_OP = 'DELETE') THEN
                                       INSERT INTO "pet store".mg_changelog
                                       SELECT 'D', now(), user, TG_TABLE_NAME, row_to_json(OLD.*), row_to_json(NEW.*);
                                   ELSIF (TG_OP = 'UPDATE') THEN
                                       INSERT INTO "pet store".mg_changelog
                                       SELECT 'U', now(), user, TG_TABLE_NAME, row_to_json(OLD.*), row_to_json(NEW.*);
                                   ELSIF (TG_OP = 'INSERT') THEN
                                       INSERT INTO "pet store".mg_changelog
                                       SELECT 'I', now(), user, TG_TABLE_NAME, row_to_json(OLD.*), row_to_json(NEW.*);
                                   END IF;
                                   RETURN NULL; -- result is ignored since this is an AFTER trigger
                               END;
                               $Pet_audit$ LANGUAGE plpgsql;
                        """;
    assertEquals(
        expectedFunction.strip(),
        ChangeLogUtils.buildProcessAuditFunction("pet store", "Pet").strip());
  }

  @Test
  public void testBuildAuditTrigger() {
    String expectedTrigger =
        """
                        CREATE TRIGGER Pet_audit
                        AFTER INSERT OR UPDATE OR DELETE ON "pet store"."Pet"
                            FOR EACH ROW EXECUTE FUNCTION "pet store"."process_Pet_audit"();
                          """;
    assertEquals(
        expectedTrigger.strip(), ChangeLogUtils.buildAuditTrigger("pet store", "Pet").strip());
  }

  @Test
  public void testBuildAuditTriggerWithSpaceInTableName() {
    String expectedTrigger =
        """
                        CREATE TRIGGER My_pets_audit
                        AFTER INSERT OR UPDATE OR DELETE ON "pet store"."My pets"
                            FOR EACH ROW EXECUTE FUNCTION "pet store"."process_My_pets_audit"();
                          """;
    assertEquals(
        expectedTrigger.strip(), ChangeLogUtils.buildAuditTrigger("pet store", "My pets").strip());
  }

  @Test
  public void testRemoveProcessAuditFunction() {
    assertEquals(
        "DROP FUNCTION IF EXISTS \"my schema\".\"process_my_table_audit\"() CASCADE",
        ChangeLogUtils.buildProcessAuditFunctionRemove("my schema", "my table").strip());
  }

  @Test
  public void testRemoveAuditTrigger() {
    assertEquals(
        "DROP TRIGGER IF EXISTS my_table_audit ON \"my schema\".\"my table\" CASCADE",
        ChangeLogUtils.buildAuditTriggerRemove("my schema", "my table").strip());
  }
}
