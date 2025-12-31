from pathlib import Path

BASE_DIR = Path(__file__).parent.parent

changelog_query = """{_changes(limit: 1) {stamp}}"""
