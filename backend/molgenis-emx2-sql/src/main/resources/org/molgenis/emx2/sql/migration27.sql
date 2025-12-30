ALTER TABLE "MOLGENIS".users_metadata
    ADD COLUMN IF NOT EXISTS admin boolean DEFAULT false;
