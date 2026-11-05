import importlib
import io
import os
import shutil
import subprocess
import tarfile
from pathlib import Path

import pytest

from migration_check import check_export, compare_dirs, load_model

TESTS_DIR = Path(__file__).parent
MIGRATIONS_DIR = TESTS_DIR / 'migrations'
REPO_DIR = TESTS_DIR.parents[3]
CASES = sorted(p.parent for p in MIGRATIONS_DIR.glob('*/*/from'))


def case_id(case: Path) -> str:
    return f"{case.parent.name}/{case.name}"


def model_dir_for(version_dir: Path, tmp_root: Path) -> Path:
    """The new data model for a version: MG_MIGRATION_MODEL_DIR, else the commit in model_commit.txt, else this checkout."""
    if os.environ.get('MG_MIGRATION_MODEL_DIR'):
        return Path(os.environ['MG_MIGRATION_MODEL_DIR'])
    commit_file = version_dir / 'model_commit.txt'
    if not commit_file.exists():
        return REPO_DIR / 'data' / '_models' / 'shared'
    commit = commit_file.read_text().strip()
    target = tmp_root / f"model_{commit}"
    if not target.exists():
        archive = subprocess.run(
            ['git', '-C', str(REPO_DIR), 'archive', '--format=tar', commit, 'data/_models/shared'],
            check=True, capture_output=True).stdout
        with tarfile.open(fileobj=io.BytesIO(archive)) as tar:
            tar.extractall(target, filter='data')
    return target / 'data' / '_models' / 'shared'


@pytest.fixture(scope='module', params=CASES, ids=case_id)
def case(request):
    return request.param


@pytest.fixture(scope='module')
def migrated(case, tmp_path_factory):
    version = case.parent.name
    model_dir = model_dir_for(case.parent, tmp_path_factory.getbasetemp())
    out = tmp_path_factory.mktemp(f"{version}_{case.name}") / 'data'
    shutil.copytree(case / 'from', out)

    transform_module = importlib.import_module(f"update.update_{version}")
    transform = transform_module.Transform(schema_name=case.name, data_dir=str(out), model_dir=str(model_dir))
    transform.delete_data_model_file()
    transform.update_data_model_file()
    transform.transform_data()
    return case, out


def test_from_is_a_valid_export_of_the_old_model(case):
    problems = check_export(case / 'from')
    assert problems == [], '\n'.join(problems)


def test_output_matches_expected(migrated):
    case, out = migrated
    differences = compare_dirs(case / 'to', out,
                               old_model=load_model(case / 'from' / 'molgenis.csv'),
                               new_model=load_model(out / 'molgenis.csv'))
    assert differences == [], '\n'.join(differences)


def test_output_fits_the_new_model(migrated):
    case, out = migrated
    problems = check_export(out, previous_model=load_model(case / 'from' / 'molgenis.csv'))
    assert problems == [], '\n'.join(problems)
