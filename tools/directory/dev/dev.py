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
from tools.directory.src.molgenis_emx2.directory_client.pid_service import (
    DummyPidService,
    NoOpPidService,
    PidService,
)

# Get credentials from .env
load_dotenv('./tools/directory/dev/.env')

target = os.getenv("TARGET")
username = os.getenv("USERNAME")
password = os.getenv("PASSWORD")
directory_schema = os.getenv("DIRECTORY")
pid_service_type = os.getenv(
    "PID_SERVICE"
)  # Select from: ['production', 'dummy', 'none']


async def sync_directory():
    """
    Stage external nodes and publish them to the directory schema

    Note: when staging an external server-type node the .env file should include
    a "node"_user="token" with view permissions on the external staging area
    """
    # Set up the logger
    logging.basicConfig(level="INFO", format=" %(levelname)s: %(name)s: %(message)s")
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    # Login to the directory with a DirectorySession
    with DirectorySession(url=target, schema=directory_schema) as session:
        # Apply the 'signin' method with the username and password
        session.signin(username, password)

        # Create PidService
        if pid_service_type == 'production':
            pid_service = PidService.from_credentials("pyhandle_creds.json")
            print(f"Script runs on server {pid_service.base_url}")
        elif pid_service_type == 'dummy':
            # Use DummyPidService if testing without interacting with a handle server
            pid_service = DummyPidService()
        else:
            # Use NoOpPidService if you want to turn off the PID features completely
            pid_service = NoOpPidService()

        # Instantiate the Directory class
        directory = Directory(session, pid_service)

        # Stage
        nodes_to_stage = session.get_external_nodes()
        staging_report = await directory.stage_external_nodes(nodes_to_stage)
        if staging_report.has_errors():
            raise ValueError("Some nodes did not stage correctly")

        # Publish
        nodes_to_publish = session.get_nodes()
        publishing_report = await directory.publish_nodes(nodes_to_publish)
        if publishing_report.has_errors():
            raise ValueError("Some nodes did not publish correctly")


if __name__ == "__main__":
    asyncio.run(sync_directory())
