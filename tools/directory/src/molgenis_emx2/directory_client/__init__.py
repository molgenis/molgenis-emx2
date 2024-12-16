import sys

if sys.version_info[:2] >= (3, 8):
    # noinspection PyCompatibility
    from importlib.metadata import PackageNotFoundError, version  # pragma: no cover
else:
    # noinspection PyUnresolvedReferences
    from importlib_metadata import PackageNotFoundError, version  # pragma: no cover

try:
    dist_name = "molgenis-emx2-directory-client"
    __version__ = version(dist_name)
except PackageNotFoundError:  # pragma: no cover
    __version__ = "unknown"
finally:
    del version, PackageNotFoundError

__version__ = "1.1.0"
