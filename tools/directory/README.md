# molgenis-emx2-directory-client

MOLGENIS EMX2 Python tooling for a BBMRI Biobank Directory

## Description

This library contains tools for the MOLGENIS EMX2 BBMRI Biobank Directory that help with
staging and publishing the data of the national nodes. **Staging** is the process of copying
data from a national node's external server (for example [BBMRI-NL](https://catalogue.bbmri.nl/menu/main/home)) to
the staging area on the Directory server. Not all national nodes have external servers; these
do not need to be staged. **Publishing** is the process of copying and combining the data from the staging areas
to the public combined tables of the Directory.

## Usage

These tools can be used as a library in a script. Start by installing the library with
`pip install molgenis-emx2-directory-client`.

For an example of how to use this library to stage and publish nodes, see [`dev.py`](dev/dev.py).

If you just want to retrieve the data of a node for another purpose, you can use the `DirectorySession`
and `ExternalServerSession` directly:

```python
import logging
import asyncio
import os
from molgenis_emx2.directory_client.directory_client import DirectorySession
from molgenis_emx2.directory_client.directory_client import NodeData

os.environ['NN_SCHEMA_PREFIX'] = "BBMRI"

# Get the staging and published data of NL from the directory
async def get_data():
    # Set up the logger
    logging.basicConfig(level="INFO", format=" %(levelname)s: %(name)s: %(message)s")
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    # Login to the directory with a DirectorySession
    with DirectorySession(url="<DIRECTORY_URL>", schema="<DIRECTORY_SCHEMA>") as session:
        # Apply the 'signin' method with the username and password
        session.signin(username, password)

        nl = session.get_node("NL")
        nl_staging_data: NodeData = session.get_staging_node_data(nl)
        nl_published_data: NodeData = session.get_published_node_data(nl)

    # Now you can use the NodeData objects as you wish
    for person in nl_staging_data.persons.rows:
        print(person)

    # Now you can use the NodeData objects as you wish
    for biobank in nl_published_data.biobanks.rows:
        print(biobank)

if __name__ == "__main__":
    asyncio.run(get_data())
```

## For developers

Clone the `molgenis-emx2` repository from GitHub

```console
git clone git@github.com:molgenis/molgenis-emx2.git
```

Change the working directory to `.../tools/directory`

This project uses [pre-commit](https://pre-commit.com/) and
[pipenv](https://pypi.org/project/pipenv/) for the development workflow.
Install pre-commit and pipenv if you haven't already:

```console
pip install pre-commit
pip install pipenv
```

Install the git commit hooks:

```console
pre-commit install
```

Create an environment and install the package including all (dev) dependencies:

```console
pipenv install --dev
```

Enter the environment:

```console
pipenv shell
```

Build and run the tests:

```console
tox
```

## Build

Before building the source, the package `bumpversion` needs to be installed.

```console
(venv) $ pip install bumpversion
```

Bump the source version. This will update setup.py and **init**.py. NB! Make sure that
the version numbers in these file have single quotes.
Always start with creating a new -dev0 version with major, minor or patch parameter
depending on the release scope

```console
(venv) $ ./bump-version.sh major
OR
(venv) $ ./bump-version.sh minor
OR
(venv) $ ./bump-version.sh patch
```

Then either create a new dev-version in case any changes have been made

```console
(venv) $ ./bump-version.sh build
```

Or if all is fine, create a new release version

```console
(venv) $ ./bump-version.sh release
```

After bumping the version, the source can be build

```console
(venv) $ tox -e build
```

Then dev (and release) versions of the source can be uploaded to testpypi

```console
(venv) $ tox -e publish -- --skip-existing --repository testpypi
```

And release versions of the source can be uploaded to pypi

```console
(venv) $ tox -e publish -- --skip-existing --repository pypi
```

Or install locally

```console
(venv) $ pip install dist/molgenis_emx2_pyclient*.whl
```

When releasing a new version, don't forget to update CHANGELOG.md and,
if applicable, README.md and AUTHORS.md.

## Note

This project has been set up using PyScaffold 4.0.2. For details and usage
information on PyScaffold see <https://pyscaffold.org/>.
