import csv
from collections import OrderedDict
from typing import List


def to_ordered_dict(rows: List[dict], id_attribute: str) -> OrderedDict:
    rows_by_id = OrderedDict()
    for row in rows:
        rows_by_id[row[id_attribute]] = row
    return rows_by_id


def create_csv(table: List[dict], file_name: str, meta_attributes: List[str]):
    with open(file_name, "w", encoding="utf-8") as fp:
        writer = csv.DictWriter(
            fp, fieldnames=meta_attributes, quoting=csv.QUOTE_ALL, extrasaction="ignore"
        )
        writer.writeheader()
        for row in table:
            for key, value in row.items():
                if isinstance(value, list):
                    row[key] = ",".join(value)
            writer.writerow(row)
