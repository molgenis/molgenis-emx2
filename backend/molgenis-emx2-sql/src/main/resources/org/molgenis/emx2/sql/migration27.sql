ALTER TABLE "MOLGENIS".users_metadata
    ADD admin boolean DEFAULT false;
UPDATE "MOLGENIS".users_metadata
SET admin = true
WHERE username LIKE 'admin';