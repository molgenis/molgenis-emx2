from typing import List
from unittest.mock import MagicMock

import pytest

from molgenis_emx2.directory_client.errors import DirectoryWarning
from molgenis_emx2.directory_client.printer import Printer
from molgenis_emx2.directory_client.validation import Validator


@pytest.fixture
def validator() -> Validator:
    return Validator(MagicMock(), MagicMock())


def test_validate_id(node_data):
    validator = Validator(node_data, Printer())

    warnings = validator.validate()

    assert warnings == [
        DirectoryWarning(
            message="bbmri-eric:ID:NL_invalid_person_classifier in entity: "
            "Persons does not start with "
            "bbmri-eric:contactID:NL_ or bbmri-eric:contactID:EU_"
        ),
        DirectoryWarning(
            message="bbmri-eric:contactID:NL_invalid_illegal_characters#$& in entity: "
            "Persons contains invalid characters. Only "
            "alphanumerics and -_: are allowed."
        ),
        DirectoryWarning(
            message="bbmri-eric:ID:NL_invalid_network_classifier in entity: "
            "Networks does not start with "
            "bbmri-eric:networkID:NL_ or bbmri-eric:networkID:EU_"
        ),
        DirectoryWarning(
            message="bbmri-eric:networkID:BE_invalid_node_code in entity: "
            "Networks does not start with "
            "bbmri-eric:networkID:NL_ or bbmri-eric:networkID:EU_"
        ),
        DirectoryWarning(
            message="bbmri-eric:akiID:BE_invalid_node_code in entity: "
            "AlsoKnownIn does not start with "
            "bbmri-eric:akiID:NL_"
        ),
        DirectoryWarning(
            message="bbmri-eric:test:NL_invalid_biobank_classifier in entity: "
            "Biobanks does not start with bbmri-eric:ID:NL_"
        ),
        DirectoryWarning(
            message="bbmri-eric:ID:BE_invalid_node_code in entity: "
            "Biobanks does not start with bbmri-eric:ID:NL_"
        ),
        DirectoryWarning(
            message="Biobank bbmri-eric:ID:NL_valid:biobankID-2 has an invalid url: "
            "www.invalid@url.nl"
        ),
        DirectoryWarning(
            message="bbmri-eric:NL_invalid:serviceID in entity: "
            "Services does not start with bbmri-eric:serviceID:NL_"
        ),
        DirectoryWarning(
            message="bbmri-eric:NL_invalid:studiesID in entity: "
            "Studies does not start with bbmri-eric:studyID:NL_"
        ),
        DirectoryWarning(
            message="bbmri-eric:collection:NL_invalid_collection_classifier in entity: "
            "Collections does not start with bbmri-eric:ID:NL_"
        ),
        DirectoryWarning(
            message="bbmri-eric:ID:BE_invalid_node_code:collectionID in entity: "
            "Collections does not start with bbmri-eric:ID:NL_"
        ),
        DirectoryWarning(
            message="bbmri-eric:NL_invalid:factsID in entity: "
            "CollectionFacts does not start with bbmri-eric:factID:NL_"
        ),
        DirectoryWarning(
            message="bbmri-eric:networkID:NL_valid:networkID-2 references invalid id: "
            "bbmri-eric:ID:NL_invalid_person_classifier"
        ),
        DirectoryWarning(
            message="bbmri-eric:networkID:BE_invalid_node_code references invalid id: "
            "bbmri-eric:ID:NL_invalid_network_classifier"
        ),
        DirectoryWarning(
            message="bbmri-eric:ID:NL_valid-biobankID-1 references invalid id: "
            "bbmri-eric:akiID:BE_invalid_node_code"
        ),
        DirectoryWarning(
            message="bbmri-eric:ID:NL_valid:biobankID-2 references invalid id: "
            "bbmri-eric:ID:NL_invalid_person_classifier"
        ),
        DirectoryWarning(
            message="bbmri-eric:ID:BE_invalid_node_code references invalid id: "
            "bbmri-eric:ID:NL_invalid_network_classifier"
        ),
        DirectoryWarning(
            message="bbmri-eric:ID:NL_valid-collectionID-1 references invalid id: "
            "bbmri-eric:akiID:BE_invalid_node_code"
        ),
        DirectoryWarning(
            message="bbmri-eric:ID:NL_valid:collectionID-2 references invalid id: "
            "bbmri-eric:ID:NL_invalid_person_classifier"
        ),
        DirectoryWarning(
            message="bbmri-eric:ID:NL_valid:collectionID-2 references invalid id: "
            "bbmri-eric:test:NL_invalid_biobank_classifier"
        ),
        DirectoryWarning(
            message="bbmri-eric:ID:NL_valid:collectionID-2 references invalid id: "
            "bbmri-eric:collection:NL_invalid_collection_classifier"
        ),
        DirectoryWarning(
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
            [DirectoryWarning("Collection 2 has age_low/age_high without age_unit")],
        ),
        (
            {"id": "3", "age_low": 0, "age_high": 0, "age_unit": "YEAR"},
            [
                DirectoryWarning(
                    "Collection 3 has invalid ages: age_low = 0 and age_high = " "0"
                )
            ],
        ),
        (
            {"id": "4", "age_low": 40, "age_high": 20},
            [
                DirectoryWarning("Collection 4 has age_low/age_high without age_unit"),
                DirectoryWarning("Collection 4 has invalid ages: age_low > age_high"),
            ],
        ),
    ],
)
def test_validate_collection_ages(
    validator: Validator, collection: dict, expected: List[DirectoryWarning]
):
    validator._validate_ages(collection)
    assert validator.warnings == expected
