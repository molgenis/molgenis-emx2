from .client import Client

from importlib import resources
try:
    import tomllib
except ModuleNotFoundError:
    import tomli as tomllib

# Version of the realpython-reader package
__version__ = "8.185.0"
