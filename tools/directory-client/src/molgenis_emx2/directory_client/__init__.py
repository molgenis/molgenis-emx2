import sys

if sys.version_info[:2] >= (3, 8):
    # noinspection PyCompatibility
    from importlib.metadata import PackageNotFoundError, version  # pragma: no cover
else:
    # noinspection PyUnresolvedReferences
    from importlib_metadata import PackageNotFoundError, version  # pragma: no cover

try:
    dist_name = "molgenis-py-bbmri-eric"
    __version__ = version(dist_name)
except PackageNotFoundError:  # pragma: no cover
    __version__ = "unknown"
finally:
    del version, PackageNotFoundError
