"""
Script used in developing the StagingMigrator package.
"""
import logging
import os

from dotenv import load_dotenv

from tools.staging_migrator.src.molgenis_emx2_staging_migrator import StagingMigrator

CATALOGUE_TEST = 'catalogue test'


def main():
    """
    Main function for executing developing actions.
    """
    # Set up the logger
    logging.basicConfig(level='DEBUG')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    # Load the login details into the environment
    load_dotenv()
    # username = os.environ.get('MG_USERNAME')
    # password = os.environ.get('MG_PASSWORD')
    token = os.environ.get('MG_TOKEN')

    # with StagingMigrator(url='https://emx2.dev.molgenis.org',
    #                      staging_area='TestCohort', catalogue='catalogue') as migrator:
    #     migrator.signin(username, password)
    #     print(migrator.status)
    #     migrator.migrate()
    with StagingMigrator(url='https://ype.molgeniscloud.org', token=token,
                         staging_area='ABCD') as migrator:
        print(migrator.__repr__())
        if CATALOGUE_TEST in migrator.schema_names:
            migrator.delete_schema(CATALOGUE_TEST)
        migrator.create_schema(name=CATALOGUE_TEST, template='DATA_CATALOGUE')
        migrator.set_catalogue(CATALOGUE_TEST)
        print(migrator.status)
        try:
            migrator.migrate()
        except Exception as e:
            print(e)
        migrator.delete_schema(CATALOGUE_TEST)


if __name__ == '__main__':
    main()
