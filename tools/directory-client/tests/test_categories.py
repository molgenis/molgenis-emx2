from typing import List
from unittest.mock import MagicMock

import pytest

from molgenis.bbmri_eric.categories import Category, CategoryMapper
from molgenis.bbmri_eric.model import OntologyTable


@pytest.fixture
def mapper():
    return CategoryMapper(MagicMock())


@pytest.fixture
def disease_ontology() -> OntologyTable:
    meta = MagicMock()
    meta.id_attribute = "id"
    return OntologyTable.of(
        meta,
        [
            {"id": "urn:miriam:icd:T18.5"},
            {"id": "urn:miriam:icd:II", "ontology": "ICD-10"},
            {
                "id": "urn:miriam:icd:C97",
                "parentId": "urn:miriam:icd:II",
                "ontology": "ICD-10",
                "exact_mapping": ["ORPHA:93969"],
            },
            {"id": "urn:miriam:icd:U09"},
            {
                "id": "urn:miriam:icd:U09.9",
                "parentId": "urn:miriam:icd:U09",
                "ntbt_mapping": ["ORPHA:93969"],
            },
            {
                "id": "urn:miriam:icd:D69.6",
                "parentId": "urn:miriam:icd:D69",
            },
            {
                "id": "urn:miriam:icd:D69",
                "parentId": "urn:miriam:icd:D65-D69",
            },
            {"id": "urn:miriam:icd:I05-I09", "parentId": "urn:miriam:icd:IX"},
            {"id": "urn:miriam:icd:IX"},
            {"id": "urn:miriam:icd:U07.1"},
            {"id": "urn:miriam:icd:I05", "parentId": "urn:miriam:icd:I05-I09"},
            {
                "id": "urn:miriam:icd:D65-D69",
            },
            {"id": "urn:miriam:icd:E43", "parentId": "urn:miriam:icd:E40-E46"},
            {"id": "urn:miriam:icd:E40-E46"},
            {"id": "urn:miriam:icd:G32", "parentId": "urn:miriam:icd:G30-G32"},
            {"id": "urn:miriam:icd:G30-G32", "parentId": "urn:miriam:icd:VI"},
            {"id": "urn:miriam:icd:VI"},
            {"id": "urn:miriam:icd:Z00.5", "parentId": "urn:miriam:icd:Z00"},
            {"id": "urn:miriam:icd:Z00"},
            {"id": "ORPHA:93969", "ontology": "orphanet"},
        ],
        "parentId",
        ["exact_mapping", "ntbt_mapping"],
    )


@pytest.mark.parametrize(
    "collection,expected",
    [
        (dict(), []),
        ({"age_unit": "WEEK"}, []),
        ({"age_high": 8}, []),
        ({"age_high": 8, "age_unit": "YEAR"}, [Category.PAEDIATRICS.value]),
        ({"age_high": 365 * 18 + 1, "age_unit": "DAY"}, []),
        ({"age_high": 365 * 18 - 1, "age_unit": "DAY"}, [Category.PAEDIATRICS.value]),
        ({"age_high": 52 * 18 + 1, "age_unit": "WEEK"}, []),
        ({"age_high": 52 * 18 - 1, "age_unit": "WEEK"}, [Category.PAEDIATRICS.value]),
        ({"age_high": 12 * 18 + 1, "age_unit": "MONTH"}, []),
        ({"age_high": 12 * 18 - 1, "age_unit": "MONTH"}, [Category.PAEDIATRICS.value]),
        ({"age_low": 0, "age_high": 0, "age_unit": "YEAR"}, []),
        ({"age_low": 10, "age_high": 1, "age_unit": "YEAR"}, []),
        (
            {"age_low": 0, "age_high": 20, "age_unit": "YEAR"},
            [Category.PAEDIATRICS.value],
        ),
        ({"age_low": 18, "age_high": 40, "age_unit": "YEAR"}, []),
    ],
)
def test_map_paediatric(mapper, collection: dict, expected: List[str]):
    categories = []
    mapper._map_paediatric(collection, categories)
    assert categories == expected


@pytest.mark.parametrize(
    "collection,expected",
    [
        (dict(), []),
        ({"diagnosis_available": ["urn:miriam:icd:T18.5"]}, []),
        ({"diagnosis_available": ["ORPHA:93969"]}, [Category.RARE_DISEASE.value]),
        (
            {"diagnosis_available": ["urn:miriam:icd:C97"]},
            [Category.ONCOLOGY.value, Category.RARE_DISEASE.value],
        ),
        (
            {"diagnosis_available": ["urn:miriam:icd:U09.9"]},
            [
                Category.COVID19.value,
                Category.INFECTIOUS.value,
                Category.RARE_DISEASE.value,
            ],
        ),
        (
            {
                "diagnosis_available": [
                    "urn:miriam:icd:U09",
                    "urn:miriam:icd:T18.5",
                    "ORPHA:93969",
                ]
            },
            [
                Category.COVID19.value,
                Category.INFECTIOUS.value,
                Category.RARE_DISEASE.value,
            ],
        ),
        (
            {"diagnosis_available": ["urn:miriam:icd:D69.6"]},
            [Category.AUTOIMMUNE.value],
        ),
        (
            {"diagnosis_available": ["urn:miriam:icd:I05"]},
            [Category.CARDIOVASCULAR.value],
        ),
        (
            {"diagnosis_available": ["urn:miriam:icd:U07.1"]},
            [Category.COVID19.value, Category.INFECTIOUS.value],
        ),
        ({"diagnosis_available": ["urn:miriam:icd:E43"]}, [Category.METABOLIC.value]),
        (
            {"diagnosis_available": ["urn:miriam:icd:G32"]},
            [Category.NERVOUS_SYSTEM.value],
        ),
        (
            {"diagnosis_available": ["urn:miriam:icd:Z00.5"]},
            [Category.POPULATION.value],
        ),
    ],
)
def test_map_diseases(mapper, disease_ontology, collection: dict, expected: List[str]):
    mapper.diseases = disease_ontology
    categories = []
    mapper._map_diseases(collection, categories)
    assert categories == expected


@pytest.mark.parametrize(
    "collection,expected",
    [
        (dict(), []),
        ({"type": ["DISEASE_SPECIFIC"]}, []),
        ({"type": ["RD"]}, [Category.RARE_DISEASE.value]),
        ({"type": ["BIRTH_COHORT"]}, [Category.PAEDIATRICS.value]),
        ({"type": ["CASE_CONTROL", "POPULATION_BASED"]}, [Category.POPULATION.value]),
    ],
)
def test_map_collection_types(mapper, collection: dict, expected: List[str]):
    categories = []
    mapper._map_collection_types(collection, categories)
    assert categories == expected
