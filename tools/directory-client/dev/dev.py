"""
Example usage file meant for development. Make sure you have an .env file and a
pyhandle_creds.json file in this folder.
"""

import os

from dotenv import load_dotenv

from molgenis.bbmri_eric.bbmri_client import EricSession
from molgenis.bbmri_eric.eric import Eric
from molgenis.bbmri_eric.pid_service import PidService

# Get credentials from .env
load_dotenv()

target = os.getenv("TARGET")
username = os.getenv("USERNAME")
password = os.getenv("PASSWORD")

# Login to the directory with an EricSession
session = EricSession(url=target)
session.login(username, password)

# Get the nodes you want to work with
# When staging a node the .env file should include a "node"_user="token" with view
# permissions on the external staging area
nodes_to_stage = session.get_external_nodes(["NL", "BE"])
nodes_to_publish = session.get_nodes(["CY"])

# Create PidService
pid_service = PidService.from_credentials("pyhandle_creds.json")
print(f"Script runs on server {pid_service.base_url}")
# Use the DummyPidService if you want to test without interacting with a handle server
# pid_service = DummyPidService()
# Use the NoOpPidService if you want to turn off the PID features completely
# pid_service = NoOpPidService()

# Instantiate the Eric class and do some work
eric = Eric(session, pid_service)
staging_report = eric.stage_external_nodes(nodes_to_stage)
publishing_report = eric.publish_nodes(nodes_to_publish)

if publishing_report.has_errors():
    raise ValueError("Some nodes did not publish correctly")
