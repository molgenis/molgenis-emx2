"""
Script used in developing the StagingMigrator package.
"""
import logging
import os

from dotenv import load_dotenv

from tools.staging_migrator.src.molgenis_emx2_staging_migrator import StagingMigrator


def main():
    """
    Main function for executing developing actions.
    """
    # Set up the logger
    logging.basicConfig(level='INFO')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    # Load the login details into the environment
    load_dotenv()
    username = os.environ.get('MG_USERNAME')
    password = os.environ.get('MG_PASSWORD')

    with StagingMigrator(url='https://ype.molgeniscloud.org',
                         staging_area='TestCohort', catalogue='TestCatalogue') as migrator:
        migrator.signin(username, password)
        print(migrator.status)
        migrator.migrate()


if __name__ == '__main__':
    main()
