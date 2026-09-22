"""
Utility functions for executing the tests.
"""
from pathlib import Path

import zipfile

RESOURCES_PATH = Path(__file__).parent / "resources"

def zip_folder(dir_name: str):
    """Zips a folder in the 'resources' directory."""
    with zipfile.ZipFile(RESOURCES_PATH / f"{dir_name}.zip", 'w') as zf:
        for file in (RESOURCES_PATH / dir_name).iterdir():
            zf.write(filename=file.absolute(), arcname=file.name)
