from unittest.mock import MagicMock, patch

import pytest

from molgenis.bbmri_eric.errors import EricWarning, ErrorReport
from molgenis.bbmri_eric.model import Node
from molgenis.bbmri_eric.publication_preparer import PublicationPreparer


@pytest.fixture
def validator_init():
    with patch("molgenis.bbmri_eric.publication_preparer.Validator") as validator_mock:
        yield validator_mock


@pytest.fixture
def model_fitter_init():
    with patch(
        "molgenis.bbmri_eric.publication_preparer.ModelFitter"
    ) as model_fitter_mock:
        yield model_fitter_mock


@pytest.fixture
def transformer_init():
    with patch(
        "molgenis.bbmri_eric.publication_preparer.Transformer"
    ) as transformer_mock:
        yield transformer_mock


@pytest.fixture
def pid_manager():
    return MagicMock()


@pytest.fixture
def preparer(session, printer, pid_manager):
    return PublicationPreparer(printer, pid_manager, session)


def test_prepare(preparer, session):
    validate_func = MagicMock()
    preparer._validate_node = validate_func
    model_fitter_func = MagicMock()
    preparer._fit_node_model = model_fitter_func
    transform_func = MagicMock()
    preparer._transform_node = transform_func
    manage_pids_func = MagicMock()
    preparer._manage_node_pids = manage_pids_func
    nl = Node.of("NL")
    state = MagicMock()
    report = MagicMock()
    state.report = report
    node_data = MagicMock()
    session.get_staging_node_data.return_value = node_data

    preparer.prepare(nl, state)

    session.get_staging_node_data.assert_called_with(nl)
    validate_func.assert_called_with(node_data, report)
    model_fitter_func.assert_called_with(node_data, report)
    transform_func.assert_called_with(node_data, state)
    manage_pids_func.assert_called_with(node_data, state)


def test_validate(preparer: PublicationPreparer, validator_init, printer):
    validator = MagicMock()
    validator_init.return_value = validator
    node_data = MagicMock()
    report = MagicMock()

    preparer._validate_node(node_data, report)

    validator_init.assert_called_with(node_data, printer)
    assert validator.validate.called


def test_validate_warnings(preparer: PublicationPreparer, validator_init, printer):
    validator = MagicMock()
    validator_init.return_value = validator
    warning = EricWarning("warning")
    validator.validate.return_value = [warning]
    node_data = MagicMock()
    nl = Node.of("NL")
    node_data.node = nl
    report: ErrorReport = ErrorReport([nl])

    preparer._validate_node(node_data, report)

    assert report.node_warnings[nl] == [warning]


def test_model_fitting(preparer: PublicationPreparer, model_fitter_init):
    model_fitter = MagicMock()
    model_fitter_init.return_value = model_fitter

    preparer._fit_node_model(MagicMock(), MagicMock())

    assert model_fitter_init.called
    assert model_fitter.fit_model.called


def test_model_fitting_warnings(preparer: PublicationPreparer, model_fitter_init):
    model_fitter = MagicMock()
    model_fitter_init.return_value = model_fitter
    warning = EricWarning("warning")
    model_fitter.fit_model.return_value = [warning]
    node_data = MagicMock()
    nl = Node.of("NL")
    node_data.node = nl
    state = MagicMock()
    state.report = ErrorReport(nl)

    preparer._fit_node_model(node_data, state.report)

    assert state.report.node_warnings[nl] == [warning]


def test_transform(preparer: PublicationPreparer, transformer_init):
    transformer = MagicMock()
    transformer_init.return_value = transformer

    preparer._transform_node(MagicMock(), MagicMock())

    assert transformer_init.called
    assert transformer.transform.called


def test_transform_warnings(preparer: PublicationPreparer, transformer_init, printer):
    transformer = MagicMock()
    transformer_init.return_value = transformer
    warning = EricWarning("warning")
    transformer.transform.return_value = [warning]
    node_data = MagicMock()
    nl = Node.of("NL")
    node_data.node = nl
    state = MagicMock()
    state.report = ErrorReport(nl)

    preparer._transform_node(node_data, state)

    assert state.report.node_warnings[nl] == [warning]


def test_manage_pids(preparer, transformer_init, pid_manager):
    biobanks = MagicMock()
    node_data = MagicMock()
    node_data.biobanks = biobanks
    existing_biobanks = MagicMock()
    state = MagicMock()
    state.existing_data.biobanks = existing_biobanks

    preparer._manage_node_pids(node_data, state)

    pid_manager.assign_biobank_pids.assert_called_with(biobanks)
    pid_manager.update_biobank_pids.assert_called_with(biobanks, existing_biobanks)


def test_manage_pids_warning(preparer, transformer_init, pid_manager):
    nl = Node.of("NL")
    node_data = MagicMock()
    node_data.node = nl
    state = MagicMock()
    state.report = ErrorReport([nl])
    warning = EricWarning("warning")
    pid_manager.assign_biobank_pids.return_value = [warning]

    preparer._manage_node_pids(node_data, state)

    assert state.report.node_warnings[nl] == [warning]
