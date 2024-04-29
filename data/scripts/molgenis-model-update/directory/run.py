from decouple import config

import importlib
import os
import shutil
import sys
# append the path of the parent directory
sys.path.append("..")
from util.update_client import UpdateClient


# Data model details
ERIC_VERSION = config("ERIC_VERSION")
NN_VERSION = config("NN_VERSION")

update_module = importlib.import_module(
    f"updates.update_{ERIC_VERSION.replace('.', '_')}", package=None)

if not os.path.isdir("./files"):
    os.mkdir("./files")
else:
    # Remove all data from previous run
    shutil.rmtree('./files')
    os.mkdir("./files")

os.chdir("./files")

# Server details
ERIC = config("ERIC_SCHEMA_NAME")
ONTOLOGIES_SCHEMA_NAME = config("ONTOLOGIES_SCHEMA_NAME")
NATIONAL_NODES = config('NATIONAL_NODES',
                        cast=lambda v: [s.strip() for s in v.split(',')])
SERVER_TOKEN = config("SERVER_TOKEN")
SERVER_URL = config("SERVER_URL")

print("-----  Config variables loaded ----")
print(f"SERVER_URL: {SERVER_URL}")
print(f"ERIC_SCHEMA_NAME: {ERIC}")
print(f"ONTOLOGIES_SCHEMA_NAME: {ONTOLOGIES_SCHEMA_NAME}")
print(f"NATIONAL_NODES: {NATIONAL_NODES}")
print("-----   ----")

# sign in to server
with UpdateClient(url=SERVER_URL, token=SERVER_TOKEN, module=update_module) as session:
    if ERIC:
        print(f"Updating BBMRI-ERIC Directory data model to version {ERIC_VERSION}")
        session.re_create_model(ERIC, "BIOBANK_DIRECTORY")

    if NATIONAL_NODES:
        for nn in NATIONAL_NODES:
            print(f"\nUpdating {nn} data model to version {ERIC_VERSION}")
            session.re_create_model(nn, "BIOBANK_DIRECTORY_STAGING")
            print("-----   ----")
