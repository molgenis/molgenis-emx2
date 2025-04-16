"""
Script for developing new features.
"""

import logging
import os

from dotenv import load_dotenv

from tools.pyclient.src.molgenis_emx2_pyclient.metadata import Schema
from tools.staging_migrator.src.molgenis_emx2_staging_migrator import StagingMigrator

CATALOGUE = 'testcat1'

log = logging.getLogger('publisher')


def main(staging_area: str):

    # Set up the logger
    logging.basicConfig(level='DEBUG')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    load_dotenv()
    server_url = os.environ.get('MG_URL')
    token = os.environ.get('MG_TOKEN')

    with StagingMigrator(url=server_url, token=token, catalogue=CATALOGUE) as migrator:
        migrator.set_staging_area(staging_area)
        target_path = migrator.download_schema_zip(schema=CATALOGUE, schema_type='target', include_system_columns=True)

        schema = migrator.get_schema_metadata(staging_area)
        # df = migrator._load_table('source', schema.get_table('name', 'Variable values'))
        # df = migrator._get_filtered(schema.get_table('name', 'Resources'))
        # migrator.create_zip()
        migrator.migrate(keep_zips=True)
        # stream = migrator.create_zip()
        # with open(target_path.parent / "upload.zip", 'wb') as zf:
        #     zf.write(stream.getbuffer())


if __name__ == '__main__':
    main("testCohort")
