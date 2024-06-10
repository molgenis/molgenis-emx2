from typing import List
from unittest.mock import MagicMock

import pytest

from molgenis.bbmri_eric.errors import EricWarning
from molgenis.bbmri_eric.printer import Printer
from molgenis.bbmri_eric.validation import Validator


@pytest.fixture
def validator() -> Validator:
    return Validator(MagicMock(), MagicMock())


def test_validate_id(node_data):
    validator = Validator(node_data, Printer())

    warnings = validator.validate()

    assert warnings == [
        EricWarning(
            message="bbmri-eric:ID:NL_invalid_person_classifier in entity: "
            "eu_bbmri_eric_NL_persons does not start with "
            "bbmri-eric:contactID:NL_ or bbmri-eric:contactID:EU_"
        ),
        EricWarning(
            message="bbmri-eric:contactID:NL_invalid_illegal_characters#$& in entity: "
            "eu_bbmri_eric_NL_persons contains invalid characters. Only "
            "alphanumerics and -_: are allowed."
        ),
        EricWarning(
            message="bbmri-eric:ID:NL_invalid_network_classifier in entity: "
            "eu_bbmri_eric_NL_networks does not start with "
            "bbmri-eric:networkID:NL_ or bbmri-eric:networkID:EU_"
        ),
        EricWarning(
            message="bbmri-eric:networkID:BE_invalid_node_code in entity: "
            "eu_bbmri_eric_NL_networks does not start with "
            "bbmri-eric:networkID:NL_ or bbmri-eric:networkID:EU_"
        ),
        EricWarning(
            message="bbmri-eric:akiID:BE_invalid_node_code in entity: "
            "eu_bbmri_eric_NL_also_known_in does not start with "
            "bbmri-eric:akiID:NL_"
        ),
        EricWarning(
            message="bbmri-eric:test:NL_invalid_biobank_classifier in entity: "
            "eu_bbmri_eric_NL_biobanks does not start with bbmri-eric:ID:NL_"
        ),
        EricWarning(
            message="bbmri-eric:ID:BE_invalid_node_code in entity: "
            "eu_bbmri_eric_NL_biobanks does not start with bbmri-eric:ID:NL_"
        ),
        EricWarning(
            message="Biobank bbmri-eric:ID:NL_valid:biobankID-2 has an invalid url: "
            "www.invalid@url.nl"
        ),
        EricWarning(
            message="bbmri-eric:collection:NL_invalid_collection_classifier in entity: "
            "eu_bbmri_eric_NL_collections does not start with bbmri-eric:ID:NL_"
        ),
        EricWarning(
            message="bbmri-eric:ID:BE_invalid_node_code:collectionID in entity: "
            "eu_bbmri_eric_NL_collections does not start with bbmri-eric:ID:NL_"
        ),
        EricWarning(
            message="bbmri-eric:NL_invalid:factsID in entity: "
            "eu_bbmri_eric_NL_facts does not start with bbmri-eric:factID:NL_"
        ),
        EricWarning(
            message="bbmri-eric:networkID:NL_valid:networkID-2 references invalid id: "
            "bbmri-eric:ID:NL_invalid_person_classifier"
        ),
        EricWarning(
            message="bbmri-eric:networkID:BE_invalid_node_code references invalid id: "
            "bbmri-eric:ID:NL_invalid_network_classifier"
        ),
        EricWarning(
            message="bbmri-eric:ID:NL_valid-biobankID-1 references invalid id: "
            "bbmri-eric:akiID:BE_invalid_node_code"
        ),
        EricWarning(
            message="bbmri-eric:ID:NL_valid:biobankID-2 references invalid id: "
            "bbmri-eric:ID:NL_invalid_person_classifier"
        ),
        EricWarning(
            message="bbmri-eric:ID:BE_invalid_node_code references invalid id: "
            "bbmri-eric:ID:NL_invalid_network_classifier"
        ),
        EricWarning(
            message="bbmri-eric:ID:NL_valid-collectionID-1 references invalid id: "
            "bbmri-eric:akiID:BE_invalid_node_code"
        ),
        EricWarning(
            message="bbmri-eric:ID:NL_valid:collectionID-2 references invalid id: "
            "bbmri-eric:ID:NL_invalid_person_classifier"
        ),
        EricWarning(
            message="bbmri-eric:ID:NL_valid:collectionID-2 references invalid id: "
            "bbmri-eric:test:NL_invalid_biobank_classifier"
        ),
        EricWarning(
            message="bbmri-eric:ID:NL_valid:collectionID-2 references invalid id: "
            "bbmri-eric:collection:NL_invalid_collection_classifier"
        ),
        EricWarning(
            message="bbmri-eric:ID:BE_invalid_node_code:collectionID references "
            "invalid id: bbmri-eric:ID:NL_invalid_network_classifier"
        ),
    ]


@pytest.mark.parametrize(
    "collection,expected",
    [
        (dict(), []),
        ({"id": "0", "age_low": 0, "age_unit": "YEAR"}, []),
        (
            {"id": "2", "age_low": 5},
            [EricWarning("Collection 2 has age_low/age_high without age_unit")],
        ),
        (
            {"id": "3", "age_low": 0, "age_high": 0, "age_unit": "YEAR"},
            [
                EricWarning(
                    "Collection 3 has invalid ages: age_low = 0 and age_high = " "0"
                )
            ],
        ),
        (
            {"id": "4", "age_low": 40, "age_high": 20},
            [
                EricWarning("Collection 4 has age_low/age_high without age_unit"),
                EricWarning("Collection 4 has invalid ages: age_low > age_high"),
            ],
        ),
    ],
)
def test_validate_collection_ages(
    validator: Validator, collection: dict, expected: List[EricWarning]
):
    validator._validate_ages(collection)
    assert validator.warnings == expected
