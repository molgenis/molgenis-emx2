from typing import List
from unittest.mock import MagicMock, call, patch

import pytest

from molgenis.bbmri_eric.eric import Eric
from molgenis.bbmri_eric.errors import EricError, ErrorReport
from molgenis.bbmri_eric.model import ExternalServerNode, Node
from molgenis.bbmri_eric.publisher import PublishingState


@pytest.fixture
def report_init():
    with patch("molgenis.bbmri_eric.eric.ErrorReport") as report_mock:
        yield report_mock


@pytest.fixture
def eric(session, printer, pid_service) -> Eric:
    eric = Eric(session, pid_service)
    eric.printer = printer
    eric.stager = MagicMock()
    eric.preparator = MagicMock()
    eric.pid_manager = MagicMock()
    eric.publisher = MagicMock()
    return eric


def test_stage_external_nodes(eric):
    error = EricError("error")
    eric.stager.stage.side_effect = [None, error]
    nl = ExternalServerNode("NL", "will succeed", None, "url.nl")
    be = ExternalServerNode("BE", "wil fail", None, "url.be")

    report = eric.stage_external_nodes([nl, be])

    assert eric.printer.print_node_title.mock_calls == [call(nl), call(be)]
    assert eric.stager.stage.mock_calls == [call(nl), call(be)]
    assert nl not in report.node_errors
    assert report.node_errors[be] == error
    eric.printer.print_summary.assert_called_once_with(report)


def test_publish_node_staging_fails(eric, session, report_init):
    nl = ExternalServerNode("NL", "Netherlands", None, "url")
    state = _setup_state([nl], eric, report_init)

    error = EricError("error")
    eric.stager.stage.side_effect = error

    report = eric.publish_nodes([nl])

    eric.printer.print_node_title.assert_called_once_with(nl)
    eric.stager.stage.assert_called_with(nl)
    assert not session.get_published_node_data.called
    assert not eric.preparator.prepare.called
    assert eric.publisher.publish.called_with(state)
    assert len(state.data_to_publish.biobanks.rows_by_id) == 0
    assert report.node_errors[nl] == error
    eric.printer.print_summary.assert_called_once_with(report)


def test_publish_node_prepare_fails(eric, report_init):
    nl = ExternalServerNode("NL", "Netherlands", None, "url")
    state = _setup_state([nl], eric, report_init)

    error = EricError("error")
    eric.preparator.prepare.side_effect = error

    report = eric.publish_nodes([nl])

    eric.printer.print_node_title.assert_called_once_with(nl)
    eric.stager.stage.assert_called_with(nl)
    assert eric.publisher.publish.called_with(state)
    assert report.node_errors[nl] == error
    eric.printer.print_summary.assert_called_once_with(report)


def test_publish_nodes(eric, report_init):
    no = Node("NO", "succeeds", None)
    nl = ExternalServerNode("NL", "fails during publishing", None, "url")
    state = _setup_state([no, nl], eric, report_init)

    error = EricError("error")
    eric.publisher.publish.side_effect = error
    eric.stager.stage.return_value = []

    report = eric.publish_nodes([no, nl])

    assert eric.printer.print_node_title.mock_calls == [call(no), call(nl)]
    eric.stager.stage.assert_has_calls([call(nl)])
    eric.preparator.prepare.assert_has_calls(
        [call(no, state), call(nl, state)], any_order=True
    )
    eric.publisher.publish.assert_called_with(state)
    assert len(report.node_errors) == 0
    assert report.error == error
    assert len(report.node_warnings[nl]) == 0
    eric.printer.print_summary.assert_called_once_with(report)


# noinspection PyProtectedMember
def _setup_state(nodes: List[Node], eric: Eric, report_init):
    report = ErrorReport(nodes)
    report_init.return_value = report

    state = PublishingState(
        existing_data=MagicMock(),
        eu_node_data=MagicMock(),
        quality_info=MagicMock(),
        nodes=nodes,
        report=report,
        diseases=MagicMock(),
    )
    eric._init_state = MagicMock()
    eric._init_state.return_value = state
    return state
