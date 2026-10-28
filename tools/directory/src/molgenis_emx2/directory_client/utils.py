import csv
from collections import OrderedDict
from typing import List

data_categories_mapping = {
    "NATIONAL_REGISTRIES": "NATIONAL_REGISTRIES",
    "IMAGING_DATA": "IMAGING_DATA",
    "PHYSIOLOGICAL_BIOCHEMICAL_MEASUREMENTS": ["Physiological dataset","Biochemical dataset"],
    "GENEALOGICAL_RECORDS": "Genealogical records",
    "MEDICAL_RECORDS": "Clinical dataset",
    "SURVEY_DATA": "SURVEY_DATA",
    "OTHER": "Other sample-derived dataset",
    "BIOLOGICAL_SAMPLES": "Biological samples",
    "ANTIBODIES": "Antibodies",
    "BLOOD": "Blood",
    "NAV": "Not available",
    "TREATMENT_PROTOCOL": "Treatment protocol",
    "DiseaseDuration": "Disease duration",
    "CLINICAL_SYMPTOMS": "Clinical dataset",
    "CT": "Body image",
}


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

def apply_mapping(mapping, element):
    """Take a value and apply the ontology mapping"""
    if isinstance(element, list):
        mapped_element = []
        for value in element:
            if isinstance(mapping[value], list):
                mapped_element += mapping[value]
            elif isinstance(mapping[value], str):
                mapped_element.append(mapping[value])
            else:
                raise ValueError
        return mapped_element
    elif isinstance(element, str):
        return mapping[element]
    else:
        raise ValueError