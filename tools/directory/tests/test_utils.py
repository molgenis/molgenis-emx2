import pytest

from molgenis_emx2.directory_client import utils


@pytest.fixture
def rows():
    return [
        {
            "id": "collA",
            "parent_collection": "collB",
            "sub_collections": [],
        },
        {
            "id": "collB",
            "sub_collections": ["collA"],
        },
    ]


def test_to_ordered_dict(rows):
    rows_by_id = utils.to_ordered_dict(rows, "id")
    assert rows_by_id["collA"]["parent_collection"] == "collB"
    assert rows_by_id["collB"]["sub_collections"] == ["collA"]
