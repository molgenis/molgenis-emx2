from molgenis_emx2 import Molgenis
from rdflib import Graph, Namespace, RDF
import requests

# EMX2 Configuration
EMX2_URL = "https://your-emx2-instance"
USERNAME = "your_username"
PASSWORD = "your_password"
DATA_SCHEMA = "your_data_schema"  # Schema for datasets
ONTOLOGY_SCHEMA = "your_ontology_schema"  # Schema for ontology tables

# Connect to EMX2
molgenis = Molgenis(EMX2_URL)
molgenis.login(USERNAME, PASSWORD)

# Define RDF Namespaces
DCAT = Namespace("http://www.w3.org/ns/dcat#")
DCT = Namespace("http://purl.org/dc/terms/")
RDFS = Namespace("http://www.w3.org/2000/01/rdf-schema#")
SKOS = Namespace("http://www.w3.org/2004/02/skos/core#")

# Cache ontology terms
ontology_cache = {
    "licenses": {},
    "themes": {},
    "categories": {}
}

# Buffers for batch upload
missing_ontologies = {
    "licenses": [],
    "themes": [],
    "categories": []
}
datasets = []


def get_ontology_terms(schema, table_name):
    """Retrieve and cache ontology terms from EMX2."""
    full_table_name = f"{schema}/{table_name}"
    if table_name in ontology_cache and ontology_cache[table_name]:
        return ontology_cache[table_name]

    try:
        ontology_data = molgenis.get_table(full_table_name)
        ontology_map = {entry["uri"]: entry["name"] for entry in ontology_data}
        ontology_cache[table_name] = ontology_map
        return ontology_map
    except Exception as e:
        print(f"Error fetching ontology {table_name}: {e}")
        return {}


def get_label_from_rdf(graph, uri):
    """Try to extract a label for a given URI from the RDF graph."""
    uri_ref = graph.resource(uri)
    for predicate in [RDFS.label, DCT.title, SKOS.prefLabel]:
        label = graph.value(uri_ref, predicate)
        if label:
            return str(label)
    return None


def derive_label_from_uri(uri):
    """Fallback: Extract a human-readable name from the URI."""
    return uri.split("/")[-1].replace("-", " ").replace("_", " ").title()


def check_and_collect_missing_terms(graph, ontology_table, property_uri):
    """Check for missing ontology terms and collect them for batch upload."""
    ontology_map = get_ontology_terms(ONTOLOGY_SCHEMA, ontology_table)

    for dataset in graph.subjects(RDF.type, DCAT.Dataset):
        term_uri = str(graph.value(dataset, property_uri))
        if term_uri and term_uri not in ontology_map:
            derived_label = get_label_from_rdf(graph, term_uri) or derive_label_from_uri(term_uri)

            # Avoid duplicates in the missing_ontologies list
            if not any(term["uri"] == term_uri for term in missing_ontologies[ontology_table]):
                missing_ontologies[ontology_table].append({"uri": term_uri, "name": derived_label})

    return ontology_map


def extract_datasets(graph):
    """Extract datasets from DCAT RDF and collect ontology terms for batch upload."""
    license_map = check_and_collect_missing_terms(graph, "licenses", DCT.license)
    theme_map = check_and_collect_missing_terms(graph, "themes", DCT.subject)
    category_map = check_and_collect_missing_terms(graph, "categories", DCT.type)

    for dataset in graph.subjects(RDF.type, DCAT.Dataset):
        dataset_info = {
            "id": str(graph.value(dataset, DCT.identifier)) or "",
            "title": str(graph.value(dataset, DCT.title)) or "Unknown",
            "description": str(graph.value(dataset, DCT.description)) or "",
            "publisher": str(graph.value(dataset, DCT.publisher)) or "",
            "issued": str(graph.value(dataset, DCT.issued)) or "",
            "modified": str(graph.value(dataset, DCT.modified)) or "",
            "license": license_map.get(str(graph.value(dataset, DCT.license)), "Unknown"),
            "theme": theme_map.get(str(graph.value(dataset, DCT.subject)), "Unknown"),
            "category": category_map.get(str(graph.value(dataset, DCT.type)), "Unknown")
        }
        datasets.append(dataset_info)


def batch_upload_ontologies():
    """Batch upload missing ontology terms in one step."""
    for table, records in missing_ontologies.items():
        if records:
            try:
                print(f"Uploading {len(records)} missing terms to {table}...")
                molgenis.import_data(f"{ONTOLOGY_SCHEMA}/{table}", records)
            except Exception as e:
                print(f"Error uploading missing terms to {table}: {e}")


def batch_upload_datasets():
    """Batch upload dataset records in one step."""
    if datasets:
        try:
            print(f"Uploading {len(datasets)} datasets...")
            molgenis.import_data(f"{DATA_SCHEMA}/datasets", datasets)
        except Exception as e:
            print(f"Error uploading datasets: {e}")


# Main execution
rdf_file = "dcat_example.rdf"

# Load RDF Graph
graph = Graph()
graph.parse(rdf_file, format="xml")  # Adjust format if needed (e.g., "turtle")

# Extract datasets and collect missing ontology terms
extract_datasets(graph)

# Perform batch uploads
batch_upload_ontologies()
batch_upload_datasets()

# Logout from MOLGENIS
molgenis.logout()
