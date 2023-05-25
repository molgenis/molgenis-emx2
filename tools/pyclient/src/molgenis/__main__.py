import logging
import os
import sys

from dotenv import load_dotenv
from client import Client

def main():
    logging.basicConfig(level=os.environ.get("LOGLEVEL", "INFO"))
    log = logging.getLogger('run')

    # Load the variables from the .env file into the environment
    load_dotenv()

    try:
        url = os.environ['MG_URL']
        username = os.environ['MG_USERNAME']
        password = os.environ['MG_PASSWORD']
        database = os.environ['MG_DATABASE']

        log.info(f'URL: {url}')
        log.info(f'USERNAME: {username}')
        log.info(f'PASSWORD: ********')
        log.info(f'DATABASE: {database}')

        Client(
            url=url,
            database=database,
            email=username,
            password=password
        )

    except KeyError:
        log.error('Make sure you filled in all variables in the .env file, script will exit now.')
        sys.exit()

if __name__ == "__main__":
    main()