ALTER TABLE "MOLGENIS"."schema_metadata"
    ADD COLUMN IF NOT EXISTS is_changelog_enabled BOOLEAN DEFAULT FALSE ;