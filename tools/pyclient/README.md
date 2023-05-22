# MOLGENIS EMX2 PY Client

## For Developers

This package is modelled after the [molgenis-py-client](https://github.com/molgenis/molgenis-py-client/) library and contains the minimal configurations to build the package. See the following section for build commands.

### Building the package

Building the package requires the `tox` package. Install the required build dependencies.

```zsh
pip install tox
```

Build command and publish

```zsh
tox -e build
tox -e publish -- --repository pypi --username <USERNAME> --passwrd <PASSWORD>
```
