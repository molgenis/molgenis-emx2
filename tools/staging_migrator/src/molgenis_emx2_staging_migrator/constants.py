from pathlib import Path
from typing import TypeAlias, Literal

BASE_DIR = Path(__file__).parent.parent

SchemaType: TypeAlias = Literal['source', 'target']

changelog_query = """{_changes(limit: 1) {stamp}}"""
