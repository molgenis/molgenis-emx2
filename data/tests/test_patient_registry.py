import pytest
from molgenis_emx2_pyclient.exceptions import PyclientException

ONTOLOGY_SCHEMA = "CatalogueOntologies"

ERDERA_LISTS = [
    "Extraction kits",
    "Library preparation kits",
    "Library prep kit manufacturers",
    "Enrichment kit manufacturers",
    "Sample collection methods",
]
RNA_KIT_COLUMNS = [
    ("Samples RNA", "extraction kit", "Extraction kits"),
    ("Experiments RNA", "library preparation kit manufacturer", "Library prep kit manufacturers"),
    ("Experiments RNA", "library preparation kit", "Library preparation kits"),
    ("Experiments RNA", "enrichment kit manufacturer", "Enrichment kit manufacturers"),
    ("Experiments RNA", "target enrichment kit", "Sequencing enrichment kits"),
]
DNA_KIT_COLUMNS = [
    ("Samples lrGS", "extraction kit"),
    ("Samples srDNA", "extraction kit"),
    ("Experiments lrGS", "library preparation kit manufacturer"),
    ("Experiments srDNA", "library preparation kit manufacturer"),
    ("Experiments lrGS", "library preparation kit"),
    ("Experiments srDNA", "library preparation kit"),
]


@pytest.fixture(params=["registry", "staging"])
def model(request):
    return request.getfixturevalue(request.param)


def column(schema, table, name):
    return schema.get_table("name", table).get_column("name", name)


@pytest.mark.parametrize(
    "table, name",
    [
        ("Materials", "submitter"),
        ("Samples RNA", "sample collection method"),
        ("Experiments", "sample"),
    ],
)
def test_column_is_required_in_staging_only(staging, registry, table, name):
    assert column(staging, table, name).get("required") is True, f"{table}.{name} must be required in staging"
    assert column(registry, table, name).get("required") is not True, f"{table}.{name} must be optional in the registry"


def test_rna_sequencing_centre_is_free_text(model):
    assert column(model, "Experiments RNA", "sequencing centre").get("columnType") == "STRING"


@pytest.mark.parametrize("table", ["Experiments lrGS", "Experiments srDNA"])
def test_other_sequencing_centres_still_refer_to_organisations(staging, table):
    assert column(staging, table, "sequencing centre").get("refTableName") == "Organisations"


@pytest.mark.parametrize("table, name, ontology", RNA_KIT_COLUMNS)
def test_rna_kit_columns_refer_to_catalogue_ontologies(model, table, name, ontology):
    found = column(model, table, name)
    actual = (found.get("columnType"), found.get("refSchemaName"), found.get("refTableName"))
    assert actual == ("ONTOLOGY", ONTOLOGY_SCHEMA, ontology), f"{model.name}: {table}.{name}"


@pytest.mark.parametrize("table, name", DNA_KIT_COLUMNS)
def test_dna_kit_columns_stay_free_text_and_required_in_staging(staging, registry, table, name):
    for schema in (registry, staging):
        assert column(schema, table, name).get("columnType") == "STRING", f"{schema.name}: {table}.{name}"
    assert column(staging, table, name).get("required") is True, f"{table}.{name} must be required in staging"


def test_library_kit_manufacturer_is_renamed(model):
    old_columns = [
        f"{table.name}.{col.name}"
        for table in model.tables
        for col in table.columns
        if col.name == "library kit manufacturer"
    ]
    assert old_columns == []


@pytest.mark.parametrize("ontology", ERDERA_LISTS)
def test_erdera_terms_are_tagged_for_templates(server, registry, ontology):
    terms = server.client.get(table=ontology, schema=ONTOLOGY_SCHEMA, parse_arrays=True)
    assert terms, f"{ontology} has no terms"
    for term in terms:
        assert term["name"] != "Other", f"{ontology}: remove the term 'Other'"
        assert {"erdera", "RNA"} <= set(term["tags"] or []), f"{ontology}: tag '{term['name']}' with erdera,RNA"


def test_only_erdera_enrichment_kits_are_tagged(server, registry):
    kits = server.client.get(table="Sequencing enrichment kits", schema=ONTOLOGY_SCHEMA, parse_arrays=True)
    erdera_kits = sorted(kit["name"] for kit in kits if "erdera" in (kit["tags"] or []))
    assert erdera_kits == [
        "NEBNext Poly(A) mRNA Magnetic Isolation Module",
        "Polaris Depletion Kit - rRNA/Globin (HMR)",
    ]
    assert len(kits) > len(erdera_kits)


def test_rna_sample_rejects_other_sample_type(server, registry):
    with pytest.raises(PyclientException, match="RNA samples must have sample type RNA"):
        server.client.save_table(
            table="Samples RNA", schema=registry.name, data=[{"id": "pytest-rna-dna", "sample type": "DNA"}]
        )


@pytest.mark.parametrize("table, sample_type", [("Samples RNA", "RNA"), ("Samples srDNA", "DNA")])
def test_sample_type_is_accepted(server, registry, table, sample_type):
    server.client.save_table(
        table=table, schema=registry.name, data=[{"id": f"pytest-{table}-{sample_type}", "sample type": sample_type}]
    )
