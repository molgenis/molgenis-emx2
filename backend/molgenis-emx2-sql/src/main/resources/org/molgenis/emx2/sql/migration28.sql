UPDATE "MOLGENIS".users_metadata
SET admin = true
WHERE username LIKE 'admin' ESCAPE '#';