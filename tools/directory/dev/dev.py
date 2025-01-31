"""
Example usage file meant for development. Make sure you have an .env file and a
pyhandle_creds.json file in this folder.
"""

import asyncio
import logging
import os

from dotenv import load_dotenv
from tools.directory.src.molgenis_emx2.directory_client.directory import Directory
from tools.directory.src.molgenis_emx2.directory_client.directory_client import (
    DirectorySession,
)
from tools.directory.src.molgenis_emx2.directory_client.pid_service import PidService

# Get credentials from .env
load_dotenv()

target = os.getenv("TARGET")
username = os.getenv("USERNAME")
password = os.getenv("PASSWORD")
directory_schema = os.getenv("DIRECTORY")


async def sync_directory():
    # Set up the logger
    logging.basicConfig(level="INFO", format=" %(levelname)s: %(name)s: %(message)s")
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    # Login to the directory with a DirectorySession
    with DirectorySession(url=target, schema=directory_schema) as session:
        # Apply the 'signin' method with the username and password
        session.signin(username, password)

        # Get the nodes you want to work with
        # When staging a node the .env file should include a "node"_user="token"
        # with view permissions on the external staging area
        nodes_to_stage = session.get_external_nodes(["NL"])
        nodes_to_publish = session.get_nodes()

        # Create PidService
        pid_service = PidService.from_credentials("pyhandle_creds.json")
        print(f"Script runs on server {pid_service.base_url}")
        # Use the DummyPidService if testing without interacting with a handle server
        # pid_service = DummyPidService()
        # Use the NoOpPidService if you want to turn off the PID features completely
        # pid_service = NoOpPidService()

        # Instantiate the Directory class and do some work
        directory = Directory(session, pid_service)
        staging_report = await directory.stage_external_nodes(nodes_to_stage)
        publishing_report = await directory.publish_nodes(nodes_to_publish)

        if staging_report.has_errors():
            raise ValueError("Some nodes did not publish correctly")

        if publishing_report.has_errors():
            raise ValueError("Some nodes did not publish correctly")


if __name__ == "__main__":
    asyncio.run(sync_directory())
