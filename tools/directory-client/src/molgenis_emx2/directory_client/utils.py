from collections import OrderedDict
from typing import List


def to_ordered_dict(rows: List[dict]) -> OrderedDict:
    rows_by_id = OrderedDict()
    for row in rows:
        rows_by_id[row["id"]] = row
    return rows_by_id
