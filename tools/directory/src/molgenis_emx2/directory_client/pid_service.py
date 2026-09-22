import secrets
from abc import ABCMeta, abstractmethod
from enum import Enum
from typing import List, Optional
from urllib.parse import quote

from pyhandle.client.resthandleclient import RESTHandleClient
from pyhandle.clientcredentials import PIDClientCredentials
from pyhandle.handleclient import PyHandleClient
from pyhandle.handleexceptions import (
    HandleAuthenticationError,
    HandleNotFoundException,
    HandleSyntaxError,
)

from .errors import DirectoryError


class Status(Enum):
    TERMINATED = "TERMINATED"
    MERGED = "MERGED"
    WITHDRAWN = "Withdrawn from the BBMRI-ERIC Directory"


def pyhandle_error_handler(func):
    """
    Decorator that catches PyHandleExceptions and wraps them in a DirectoryError.
    """

    def inner_function(*args, **kwargs):
        try:
            return func(*args, **kwargs)
        except HandleAuthenticationError as e:
            raise DirectoryError("Handle authentication failed") from e
        except HandleNotFoundException as e:
            raise DirectoryError(f"Handle not found on handle server: {e.handle}")
        except HandleSyntaxError as e:
            raise DirectoryError(f"Handle has incorrect syntax: {e.handle}")

    return inner_function


class BasePidService(metaclass=ABCMeta):
    service_prefix = "1."

    base_url: str

    @abstractmethod
    def reverse_lookup(self, url: str) -> Optional[List[str]]:
        pass

    @abstractmethod
    def register_pid(self, url: str, name: str) -> str:
        pass

    @abstractmethod
    def set_name(self, pid: str, new_name: str):
        pass

    @abstractmethod
    def set_status(self, pid: str, status: Status):
        pass

    @abstractmethod
    def remove_status(self, pid: str):
        pass

    @staticmethod
    def generate_pid(prefix: str) -> str:
        """
        Generates a new PID. Uses a cryptographically secure random, 12 digit
        hexadecimal number separated by hyphens every 4 digits (example: 6ed7-328b-2793)
        Has been tested to have <1 collisions every 10 million ids. PIDs of the
        Directory are also prefixed with "1.".
        """
        id_ = secrets.token_hex(6)
        return f"{prefix}/{BasePidService.service_prefix}{id_[:4]}-{id_[4:8]}-{id_[8:]}"


class PidService(BasePidService):
    """
    Low level service for interacting with the handle server.
    """

    def __init__(self, client: RESTHandleClient, prefix: str, base_url: str):
        self.client = client
        self.prefix = prefix
        self.base_url = base_url.rstrip("/") + "/"

    @staticmethod
    def from_credentials(credentials_json: str, base_url: str = None) -> "PidService":
        """
        Factory method to create a PidService from a credentials JSON file. The
        credentials file should have the following contents:

        {
          "handle_server_url": "...",
          "private_key": "...",
          "certificate_only": "...",
          "client": "rest",
          "prefix": "...",
          "reverselookup_username": "...",
          "reverselookup_password": "...",
          "server_url": "..." <- optional
        }

        :param base_url: the base URL to which the PIDs will link
        :param credentials_json: a full path to the credentials file
        :return: a PidService
        """
        credentials = PIDClientCredentials.load_from_JSON(credentials_json)

        if not base_url:
            try:
                base_url = credentials.get_config()["server_url"]
            except KeyError:
                raise ValueError("server_url missing in credentials file")

        return PidService(
            PyHandleClient("rest").instantiate_with_credentials(credentials),
            credentials.get_prefix(),
            base_url,
        )

    @pyhandle_error_handler
    def reverse_lookup(self, url: str) -> Optional[List[str]]:
        """
        Looks for handles with this url.

        :param url: the URL to look up
        :raise: EricError if insufficient permissions for reverse lookup
        :return: a (potentially empty) list of PIDs
        """
        url = quote(url)
        pids = self.client.search_handle(URL=url, prefix=self.prefix)

        if pids is None:
            raise DirectoryError("Insufficient permissions for reverse lookup")

        return pids

    @pyhandle_error_handler
    def register_pid(self, url: str, name: str) -> str:
        """
        Generates a new PID and registers it with a URL and a NAME field.

        :param url: the URL for the handle
        :param name: the NAME for the handle
        :return: the generated PID
        """
        pid = self.generate_pid(self.prefix)
        return self.client.register_handle(handle=pid, location=url, NAME=name)

    @pyhandle_error_handler
    def set_name(self, pid: str, new_name: str):
        """
        Sets the NAME field of an existing PID. Adds the field if it doesn't exist.

        :param pid: the PID to change the NAME of
        :param new_name: the new value for the NAME field
        """
        self.client.modify_handle_value(pid, NAME=new_name)

    @pyhandle_error_handler
    def set_status(self, pid: str, status: Status):
        """
        Sets the STATUS field of an existing PID. Adds the field if it doesn't exist.

        :param pid: the PID to change the STATUS of
        :param status: a Status enum
        """
        self.client.modify_handle_value(pid, STATUS=status.value)

    @pyhandle_error_handler
    def remove_status(self, pid: str):
        """
        Removes the STATUS field of an existing PID.

        :param pid: the PID to remove the STATUS field of
        """
        self.client.delete_handle_value(pid, "STATUS")


class DummyPidService(BasePidService):
    """
    This dummy implementation can be used to test publishing without actually
    interacting with a Handle server. It will create fake PIDs.
    """

    def __init__(self):
        self.base_url = "FAKE-SERVER/"

    def reverse_lookup(self, url: str) -> Optional[List[str]]:
        pass

    def register_pid(self, url: str, name: str) -> str:
        return self.generate_pid("FAKE-PREFIX")

    def set_name(self, pid: str, new_name: str):
        pass

    def set_status(self, pid: str, status: Status):
        pass

    def remove_status(self, pid: str):
        pass


class NoOpPidService(BasePidService):
    """
    The NoOpPidService does completely nothing. It can be used as a feature toggle:
    using this implementation will turn off all PID features.
    """

    def reverse_lookup(self, url: str) -> Optional[List[str]]:
        pass

    def register_pid(self, url: str, name: str) -> str:
        pass

    def set_name(self, pid: str, new_name: str):
        pass

    def set_status(self, pid: str, status: Status):
        pass

    def remove_status(self, pid: str):
        pass
