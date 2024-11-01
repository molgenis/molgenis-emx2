from abc import ABC, abstractmethod
from typing import List

from .errors import DirectoryWarning
from .model import Table
from .pid_service import BasePidService, NoOpPidService, Status
from .printer import Printer


class BasePidManager(ABC):
    @abstractmethod
    def assign_biobank_pids(self, biobanks: Table) -> List[DirectoryWarning]:
        pass

    @abstractmethod
    def update_biobank_pids(self, biobanks: Table, existing_biobanks: Table):
        pass

    @abstractmethod
    def terminate_biobanks(self, biobank_pids: List[str]):
        pass


class PidManager(BasePidManager):
    """
    This class is responsible for managing PIDs of BBMRI Biobank Directory entities:
    assignment, updates and status changes are done here.
    """

    def __init__(self, pid_service: BasePidService, printer: Printer):
        self.pid_service = pid_service
        self.printer = printer
        self.biobank_url_prefix = pid_service.base_url + "#/biobank/"

    def assign_biobank_pids(self, biobanks: Table) -> List[DirectoryWarning]:
        """
        Registers and assigns a new PID for biobanks that have an empty "pid" attribute.
        Make sure to enrich the table with existing PIDs before using this method.
        """
        warnings = []
        for biobank in biobanks.rows:
            if "pid" not in biobank:
                biobank["pid"] = self._register_biobank_pid(
                    biobank["id"], biobank["name"], warnings
                )
                if biobank.get("withdrawn", False):
                    self.pid_service.set_status(biobank["pid"], Status.WITHDRAWN)
                    self.printer.print(
                        f"Set STATUS of {biobank['pid']} to {Status.WITHDRAWN.value}"
                    )

        return warnings

    def update_biobank_pids(self, biobanks: Table, existing_biobanks: Table):
        """
        Detects changes in biobanks and updates their PIDs accordingly.
        """
        existing_biobanks = existing_biobanks.rows_by_id
        for biobank in biobanks.rows:
            id_ = biobank["id"]
            if id_ in existing_biobanks:
                if biobank["name"] != existing_biobanks.get(biobank["id"])["name"]:
                    self._update_biobank_name(biobank["pid"], biobank["name"])
                if (
                    biobank.get("withdrawn", False)
                    != existing_biobanks.get(biobank["id"])["withdrawn"]
                ):
                    self._update_withdrawn_status(biobank["pid"], biobank["withdrawn"])

    def terminate_biobanks(self, biobank_pids: List[str]):
        """
        Sets the STATUS of a PID into TERMINATED.
        """
        for biobank_pid in biobank_pids:
            self.pid_service.set_status(biobank_pid, Status.TERMINATED)
            self.printer.print(
                f"Set STATUS of {biobank_pid} to {Status.TERMINATED.value}"
            )

    def _register_biobank_pid(
        self, biobank_id: str, biobank_name: str, warnings: List[DirectoryWarning]
    ) -> str:
        """
        Registers a PID for a new biobank. If one or more PIDs for this biobank already
        exist, warnings will be shown.
        """
        url = self.biobank_url_prefix + biobank_id
        existing_pids = self.pid_service.reverse_lookup(url)

        if existing_pids:
            pid = existing_pids[0]
            warning = DirectoryWarning(
                f'PID(s) already exist for new biobank "{biobank_name}": '
                f"{str(existing_pids)}. Please check the PID's contents!"
            )
            self.printer.print_warning(warning)
            warnings.append(warning)
        else:
            pid = self.pid_service.register_pid(url=url, name=biobank_name)
            self.printer.print(f'Registered {pid} for new biobank "{biobank_name}"')

        return pid

    def _update_biobank_name(self, pid: str, name: str):
        self.pid_service.set_name(pid, name)
        self.printer.print(f'Updated NAME of {pid} to "{name}"')

    def _update_withdrawn_status(self, pid: str, withdrawn: bool):
        if withdrawn:
            self.pid_service.set_status(pid, Status.WITHDRAWN)
            self.printer.print(f"Set STATUS of {pid} to {Status.WITHDRAWN.value}")
        if not withdrawn:
            self.pid_service.remove_status(pid)
            self.printer.print(f"Remove WITHDRAWN STATUS of {pid}")


class NoOpPidManager(BasePidManager):
    """
    This implementation does nothing. It's used to turn off all PID features.
    """

    def assign_biobank_pids(self, biobanks: Table) -> List[DirectoryWarning]:
        return []

    def update_biobank_pids(self, biobanks: Table, existing_biobanks: Table):
        pass

    def terminate_biobanks(self, biobank_pids: List[str]):
        pass


class PidManagerFactory:
    """
    Returns an implementation of BasePidManager based on the BasePidService that is
    provided.
    """

    @staticmethod
    def create(pid_service: BasePidService, printer: Printer) -> BasePidManager:
        if type(pid_service) is NoOpPidService:
            return NoOpPidManager()
        else:
            return PidManager(pid_service, printer)
