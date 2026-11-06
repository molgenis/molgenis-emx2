import asyncio
import os

import pytest
from molgenis_emx2_pyclient import Client

SERVER = os.environ.get("MG_SERVER", "http://localhost:8080")
TOKEN = os.environ.get("MG_TOKEN")
USERNAME = os.environ.get("MG_USERNAME", "admin")
PASSWORD = os.environ.get("MG_PASSWORD", "admin")

REGISTRY_SCHEMA = "pytest RD3 registry"
STAGING_SCHEMA = "pytest RD3 staging"
STAGING_DEPENDENCIES = {"Staging area ids": "PATIENT_REGISTRY_ID_BANK", "erdera": "PATIENT_REGISTRY"}


class Server:
    """Creates schemas from templates and, at the end, deletes only the schemas that it created."""

    def __init__(self, client: Client):
        self.client = client
        self.created: list[str] = []

    def create(self, name: str, template: str):
        if name in self.client.schema_names:
            raise RuntimeError(f"Schema {name!r} already exists. Delete it, or use another server.")
        known = set(self.client.schema_names)
        asyncio.run(self.client.create_schema(name=name, template=template))
        self.created += [schema for schema in self.client.schema_names if schema not in known | {name}]
        self.created.append(name)

    def ensure(self, name: str, template: str):
        if name not in self.client.schema_names:
            self.create(name, template)

    def cleanup(self):
        for name in reversed(self.created):
            asyncio.run(self.client.delete_schema(name=name))


@pytest.fixture(scope="session")
def server():
    with Client(SERVER, token=TOKEN) as client:
        if not TOKEN:
            client.signin(USERNAME, PASSWORD)
        server = Server(client)
        try:
            yield server
        finally:
            server.cleanup()


@pytest.fixture(scope="session")
def registry(server):
    server.create(REGISTRY_SCHEMA, "PATIENT_REGISTRY")
    return server.client.get_schema_metadata(REGISTRY_SCHEMA)


@pytest.fixture(scope="session")
def staging(server, registry):
    for name, template in STAGING_DEPENDENCIES.items():
        server.ensure(name, template)
    server.create(STAGING_SCHEMA, "PATIENT_REGISTRY_STAGING")
    return server.client.get_schema_metadata(STAGING_SCHEMA)
