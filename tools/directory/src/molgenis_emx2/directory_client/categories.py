from copy import deepcopy
from enum import Enum
from typing import List, Set

from .model import OntologyTable


class Category(Enum):
    """
    Enum of Collection Categories with identifiers found in the Categories table.
    """

    AUTOIMMUNE = "autoimmune"
    CARDIOVASCULAR = "cardiovascular"
    COVID19 = "covid19"
    INFECTIOUS = "infectious"
    METABOLIC = "metabolic"
    NERVOUS_SYSTEM = "nervous_system"
    ONCOLOGY = "oncology"
    PAEDIATRICS = "paediatrics"
    POPULATION = "population"
    RARE_DISEASE = "rare_disease"


class AgeUnit(Enum):
    """
    Enum of age units with identifiers found in the AgeUnits table.
    """

    DAY = "DAY"
    WEEK = "WEEK"
    MONTH = "MONTH"
    YEAR = "YEAR"


AUTOIMMUNE_TERMS = {
    "urn:miriam:icd:B20",
    # "D50-D89",
    "urn:miriam:icd:D50-D53",
    "urn:miriam:icd:D55-D59",
    "urn:miriam:icd:D60-D64",
    "urn:miriam:icd:D65-D69",
    "urn:miriam:icd:D70-D77",
    # "urn:miriam:icd:D71",
    "urn:miriam:icd:D80-D89",
    "urn:miriam:icd:E06.3",
    "urn:miriam:icd:K90.0",
    "urn:miriam:icd:M06",
    "urn:miriam:icd:M35.9",
    "urn:miriam:icd:M45",
    "urn:miriam:icd:K75",
}

CARDIOVASCULAR_TERMS = {
    "urn:miriam:icd:IX",
}

COVID_TERMS = {
    "urn:miriam:icd:U07.1",
    "urn:miriam:icd:U07.2",
    "urn:miriam:icd:U08",
    "urn:miriam:icd:U09",
    "urn:miriam:icd:U09.9",
    "urn:miriam:icd:U10",
    "urn:miriam:icd:U10.9",
    "urn:miriam:icd:U11",
    "urn:miriam:icd:U11.9",
    "urn:miriam:icd:U12",
    "urn:miriam:icd:U12.9",
}

INFECTIOUS_TERMS = {
    "urn:miriam:icd:I",
    "urn:miriam:icd:J09-J18",
    "urn:miriam:icd:U07.1",
    "urn:miriam:icd:U07.2",
    "urn:miriam:icd:U08",
    "urn:miriam:icd:U09",
    "urn:miriam:icd:U09.9",
    "urn:miriam:icd:U10",
    "urn:miriam:icd:U10.9",
    "urn:miriam:icd:U11",
    "urn:miriam:icd:U11.9",
    "urn:miriam:icd:U12",
    "urn:miriam:icd:U12.9",
}

METABOLIC_TERMS = {
    # E00-E89
    # if E90 can be included: "urn:miriam:icd:IV",
    "urn:miriam:icd:E00-E07",
    "urn:miriam:icd:E10-E14",
    "urn:miriam:icd:E15-E16",
    "urn:miriam:icd:E20-E35",
    "urn:miriam:icd:E40-E46",
    "urn:miriam:icd:E50-E64",
    "urn:miriam:icd:E65-E68",
    "urn:miriam:icd:E70",
    "urn:miriam:icd:E71",
    "urn:miriam:icd:E72",
    "urn:miriam:icd:E73",
    "urn:miriam:icd:E74",
    "urn:miriam:icd:E75",
    "urn:miriam:icd:E76",
    "urn:miriam:icd:E77",
    "urn:miriam:icd:E78",
    "urn:miriam:icd:E79",
    "urn:miriam:icd:E80",
    "urn:miriam:icd:E83",
    "urn:miriam:icd:E84",
    "urn:miriam:icd:E85",
    "urn:miriam:icd:E86",
    "urn:miriam:icd:E87",
    "urn:miriam:icd:E88",
    "urn:miriam:icd:E89",
}

NERVOUS_SYSTEM_TERMS = {
    # G00-G99
    "urn:miriam:icd:VI",
}

ONCOLOGY_TERMS = {
    # "urn:miriam:icd:C00-D49",
    "urn:miriam:icd:II",
}

PAEDIATRIC_AGE_LIMIT = {
    AgeUnit.DAY: 365 * 18,
    AgeUnit.WEEK: 52 * 18,
    AgeUnit.MONTH: 12 * 18,
    AgeUnit.YEAR: 18,
}

POPULATION_TERMS = {
    "urn:miriam:icd:Z00",
}


class CategoryMapper:
    def __init__(self, diseases: OntologyTable):
        self.diseases = diseases

    def map(self, collection: dict) -> List[str]:
        """
        Maps data from a collection to a list of categories that the collection belongs
        to.
        :param collection: the collection to map
        :return: a list of categories
        """
        categories = []

        self._map_paediatric(collection, categories)
        self._map_diseases(collection, categories)
        self._map_collection_types(collection, categories)
        self._map_networks(collection, categories)

        return list(set(categories))

    @classmethod
    def _map_paediatric(cls, collection: dict, categories: List[str]):
        unit = collection.get("age_unit", None)
        if unit:
            low = collection.get("age_low", None)
            high = collection.get("age_high", None)

            if (
                low is not None
                and high is not None
                and ((low == 0 and high == 0) or (low > high))
            ):
                return

            age_limit = PAEDIATRIC_AGE_LIMIT[AgeUnit[unit]]
            if high is not None and (high < age_limit):
                categories.append(Category.PAEDIATRICS.value)
            if low is not None and (low < age_limit):
                # categories.append(Category.PAEDIATRIC_INCLUDED.value)
                categories.append(Category.PAEDIATRICS.value)

    @classmethod
    def _map_collection_types(cls, collection: dict, categories: List[str]):
        if "RD" in collection.get("type", []):
            categories.append(Category.RARE_DISEASE.value)
        if "BIRTH_COHORT" in collection.get("type", []):
            categories.append(Category.PAEDIATRICS.value)
        if "CASE_CONTROL" in collection.get(
            "type", []
        ) or "POPULATION_BASED" in collection.get("type", []):
            categories.append(Category.POPULATION.value)

    def _map_diseases(self, collection: dict, categories: List[str]):
        diagnoses = deepcopy(collection.get("diagnosis_available", []))
        if diagnoses:
            if self.diseases.matching_attrs:
                matching_diagnoses = self.diseases.get_matching_ontologies(diagnoses)
                diagnoses.extend(matching_diagnoses)
                diagnoses = set(diagnoses)

            if self._contains_descendant_of(diagnoses, AUTOIMMUNE_TERMS):
                categories.append(Category.AUTOIMMUNE.value)

            if self._contains_descendant_of(diagnoses, CARDIOVASCULAR_TERMS):
                categories.append(Category.CARDIOVASCULAR.value)

            if self._contains_descendant_of(diagnoses, COVID_TERMS):
                categories.append(Category.COVID19.value)

            if self._contains_descendant_of(diagnoses, INFECTIOUS_TERMS):
                categories.append(Category.INFECTIOUS.value)

            if self._contains_descendant_of(diagnoses, METABOLIC_TERMS):
                categories.append(Category.METABOLIC.value)

            if self._contains_descendant_of(diagnoses, NERVOUS_SYSTEM_TERMS):
                categories.append(Category.NERVOUS_SYSTEM.value)

            if self._contains_descendant_of(diagnoses, ONCOLOGY_TERMS):
                categories.append(Category.ONCOLOGY.value)

            if self._contains_descendant_of(diagnoses, POPULATION_TERMS):
                categories.append(Category.POPULATION.value)

            if self._contains_orphanet(diagnoses):
                categories.append(Category.RARE_DISEASE.value)

    def _map_networks(self, collection: dict, categories: List[str]):
        if (
            "bbmri-eric:networkID:EU_BBMRI-ERIC:networks:COVID19"
            in collection["network"]
        ):
            categories.append(Category.COVID19.value)

    def _contains_orphanet(self, diagnoses: List[str]) -> bool:
        for diagnosis in diagnoses:
            term = self.diseases.rows_by_id.get(diagnosis, None)
            if term and term.get("codesystem", "") == "orphanet":
                return True

    def _contains_descendant_of(self, diagnoses: List[str], terms: Set[str]):
        for diagnosis in diagnoses:
            if self.diseases.is_descendant_of_any(diagnosis, terms):
                return True
