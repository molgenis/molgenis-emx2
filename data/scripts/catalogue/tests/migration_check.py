from dataclasses import dataclass
from pathlib import Path

import pandas as pd

SYSTEM_FILES = {'molgenis', 'molgenis_settings', 'molgenis_members', 'molgenis_roles'}


@dataclass
class Column:
    name: str
    type: str
    ref_table: str
    key: str


@dataclass
class Table:
    name: str
    extends: str
    columns: dict


def load_model(molgenis_csv: Path) -> dict:
    df = pd.read_csv(molgenis_csv, dtype=str, keep_default_na=False)
    tables = {}
    for _, row in df.iterrows():
        table = tables.setdefault(row['tableName'], Table(row['tableName'], '', {}))
        if not row['columnName']:
            table.extends = table.extends or row.get('tableExtends', '')
        elif row['columnName'] not in table.columns:
            table.columns[row['columnName']] = Column(
                row['columnName'], row.get('columnType', ''), row.get('refTable', ''), row.get('key', ''))
    return tables


def _family(model: dict, table: str) -> list:
    """The table, its ancestors and its descendants: an upload of a table can hold columns of all of them.

    Columns of a sibling table (another child of an ancestor) are not part of it.
    """
    ancestors = []
    current = table
    while current in model and current not in ancestors:
        ancestors.append(current)
        current = model[current].extends
    descendants = [table] if table in model else []
    changed = True
    while changed:
        changed = False
        for name, t in model.items():
            if t.extends in descendants and name not in descendants:
                descendants.append(name)
                changed = True
    return ancestors + descendants[1:]


def columns_of(model: dict, table: str) -> dict:
    columns = {}
    for name in _family(model, table):
        for column_name, column in model[name].columns.items():
            columns.setdefault(column_name, column)
    return columns


def derived_columns(model: dict, table: str) -> set:
    if table not in model:
        return set()
    return {name for name, column in columns_of(model, table).items() if column.type == 'refback'}


def _resolve(model: dict, table: str, parts: list):
    column = columns_of(model, table).get(parts[0])
    if column is None:
        return f"column '{parts[0]}' does not exist in table '{table}'"
    if column.type == 'refback' or len(parts) == 1:
        return None
    if column.ref_table not in model:
        return None
    keys = {name for name, c in columns_of(model, column.ref_table).items() if c.key == '1'}
    if parts[1] not in keys:
        return f"'{parts[1]}' is not a key column of '{column.ref_table}' (used in '{'.'.join(parts)}')"
    if len(parts) > 2:
        return _resolve(model, column.ref_table, parts[1:])
    return None


def data_files(directory: Path) -> dict:
    return {
        p.relative_to(directory).as_posix(): p
        for p in sorted(directory.rglob('*.csv'))
        if p.stem.lower() not in SYSTEM_FILES and '_files' not in p.relative_to(directory).parts
    }


def check_export(directory: Path, previous_model: dict = None) -> list:
    """Problems that make an upload of `directory` lose data silently, judged against its own molgenis.csv.

    EMX2 skips CSV files without a matching table and ignores unknown columns, so these mistakes
    do not fail an upload. Column names that were refbacks in `previous_model` are allowed:
    refbacks are computed by EMX2 and never stored.
    """
    model = load_model(directory / 'molgenis.csv')
    problems = []
    for name, path in data_files(directory).items():
        table = path.stem
        if not _has_rows(path):
            continue
        if table not in model:
            problems.append(f"{name}: no table '{table}' in the model, an upload skips this file")
            continue
        old_derived = derived_columns(previous_model or {}, table)
        header = pd.read_csv(path, dtype=str, nrows=0).columns
        for column in header:
            if column.startswith('mg_'):
                continue
            file_column = columns_of(model, table).get(column.removesuffix('_filename'))
            if column.endswith('_filename') and file_column and file_column.type == 'file':
                continue
            parts = column.split('.')
            if parts[0] in old_derived:
                continue
            error = _resolve(model, table, parts)
            if error:
                problems.append(f"{name}: {error}")
    return problems


def _read(path: Path, drop: set) -> pd.DataFrame:
    df = pd.read_csv(path, dtype=str, keep_default_na=False)
    df = df[[c for c in df.columns if c.split('.')[0] not in drop]]
    df = df[sorted(df.columns)]
    return df.sort_values(list(df.columns)).reset_index(drop=True) if len(df.columns) else df


def _has_rows(path: Path) -> bool:
    try:
        return len(pd.read_csv(path, dtype=str, keep_default_na=False)) > 0
    except pd.errors.EmptyDataError:
        return False


def compare_dirs(expected: Path, actual: Path, old_model: dict, new_model: dict) -> list:
    """Differences between the expected and the actual migration output.

    Column order, row order and molgenis.csv are not compared. Refback columns are skipped.
    A file without rows is the same as no file, because an upload of it changes nothing.
    """
    expected_files = {name: path for name, path in data_files(expected).items() if _has_rows(path)}
    actual_files = {name: path for name, path in data_files(actual).items() if _has_rows(path)}
    differences = [f"missing file: {name}" for name in expected_files if name not in actual_files]
    differences += [f"unexpected file: {name}" for name in actual_files if name not in expected_files]
    for name in expected_files.keys() & actual_files.keys():
        table = Path(name).stem
        drop = derived_columns(old_model, table) | derived_columns(new_model, table)
        want = _read(expected_files[name], drop)
        got = _read(actual_files[name], drop)
        if list(want.columns) != list(got.columns):
            missing = sorted(set(want.columns) - set(got.columns))
            extra = sorted(set(got.columns) - set(want.columns))
            differences.append(f"{name}: columns differ, missing {missing}, unexpected {extra}")
            continue
        if not want.equals(got):
            merged = want.merge(got, how='outer', indicator=True)
            only_want = merged[merged['_merge'] == 'left_only'].drop(columns='_merge')
            only_got = merged[merged['_merge'] == 'right_only'].drop(columns='_merge')
            differences.append(
                f"{name}: rows differ\n  expected but not found:\n{only_want.head(5).to_string()}"
                f"\n  found but not expected:\n{only_got.head(5).to_string()}")
    return differences
