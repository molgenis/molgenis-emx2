"""
Script used in developing the StagingMigrator package.
"""
import logging
import os

from dotenv import load_dotenv

from tools.staging_migrator.src.molgenis_emx2_staging_migrator import StagingMigrator

STAGING_AREA = 'ACUTELINES'
CATALOGUE = 'UMCG'


def main():
    """Main function for testing StagingMigrator functionality."""

    # Set up the logger
    logging.basicConfig(level='DEBUG')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    load_dotenv()
    server_url = os.environ.get('MG_URL')
    token = os.environ.get('MG_TOKEN')

    with StagingMigrator(url=server_url, token=token,
                         staging_area=STAGING_AREA, catalogue=CATALOGUE) as migrator:
        print(migrator.status)

        migrator.migrate()


if __name__ == '__main__':
    main()
