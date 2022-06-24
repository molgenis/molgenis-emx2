package org.molgenis.emx2.sql;

import static junit.framework.TestCase.assertEquals;

import org.junit.Test;

public class AuditUtilsTest {
  @Test
  public void testBuildProcessAuditFunction() {
    String expectedFunction =
        """
            CREATE OR REPLACE FUNCTION process_Pet_audit() RETURNS TRIGGER AS $Pet_audit$
                   BEGIN
                       --
                       -- Create rows in Pet_audit to reflect the operations performed on emp,
                       -- making use of the special variable TG_OP to work out the operation.
                       --
                       IF (TG_OP = 'DELETE') THEN
                           INSERT INTO "pet store".mg_changelog
                           SELECT 'D', now(), user, row_to_json(OLD.*), row_to_json(NEW.*);
                       ELSIF (TG_OP = 'UPDATE') THEN
                           INSERT INTO "pet store".mg_changelog
                           SELECT 'U', now(), user, row_to_json(OLD.*), row_to_json(NEW.*);
                       ELSIF (TG_OP = 'INSERT') THEN
                           INSERT INTO "pet store".mg_changelog
                           SELECT 'I', now(), user, row_to_json(OLD.*), row_to_json(NEW.*);
                       END IF;
                       RETURN NULL; -- result is ignored since this is an AFTER trigger
                   END;
                   $Pet_audit$ LANGUAGE plpgsql;
            """;
    assertEquals(
        expectedFunction.strip(), AuditUtils.buildProcessAuditFunction("pet store", "Pet").strip());
  }

  @Test
  public void testBuildAuditTrigger() {
    String expectedTrigger =
        """
          CREATE TRIGGER Pet_audit
          AFTER INSERT OR UPDATE OR DELETE ON "pet store"."Pet"
              FOR EACH ROW EXECUTE FUNCTION process_Pet_audit();
            """;
    assertEquals(expectedTrigger.strip(), AuditUtils.buildAuditTrigger("pet store", "Pet").strip());
  }
}
