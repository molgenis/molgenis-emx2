"""Script to migrate staging areas on the UMCG research data catalogue."""
import logging
import os

from dotenv import load_dotenv

from tools.staging_migrator.src.molgenis_emx2_staging_migrator import StagingMigrator

log = logging.getLogger('Migrate UMCG')


def migrate_umcg():

    # Load the login details into the environment
    load_dotenv()
    username = os.environ.get('MG_USERNAME')
    password = os.environ.get('MG_PASSWORD')
    server_url = os.environ.get('MG_URL')

    staging_areas = str(os.environ.get('STAGING_AREA')).split(',')
    catalogue = 'UMCG'

    with StagingMigrator(url=server_url, catalogue=catalogue) as migrator:
        migrator.signin(username, password)

        for staging_area in staging_areas:
            log.info(f"\nMigrating staging area '{staging_area}' to '{catalogue}'.")
            migrator.set_staging_area(staging_area)
            migrator.migrate()


if __name__ == '__main__':
    # Set up the logger
    logging.basicConfig(level='INFO')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)
    migrate_umcg()
