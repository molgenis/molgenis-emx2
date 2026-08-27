import json
import logging
import os

import pandas as pd
from molgenis_emx2_pyclient import Client

SCHEMA = "ERIC"
BIOBANKS = "Biobanks"
COLLECTIONS = "Collections"

log = logging.getLogger(__name__)


def _graphql(client: Client, schema: str, mutation: str) -> dict:
    response = client.session.post(
        url=f"{client.url}/{schema}/api/graphql",
        json={"query": mutation},
    )
    response.raise_for_status()
    data = response.json()
    if "errors" in data:
        raise RuntimeError(f"GraphQL error: {json.dumps(data['errors'])}")
    return data


def create_country_role(client: Client, country: str) -> None:
    mutation = """
    mutation {
      change(roles: [{
        name: %s,
        permissions: [
          { table: "Biobanks",    select: true, insert: true, update: true, delete: true, isRowLevel: true },
          { table: "Collections", select: true, insert: true, update: true, delete: true, isRowLevel: true }
        ]
      }]) {
        message
      }
    }
    """ % json.dumps(country)
    _graphql(client, SCHEMA, mutation)
    log.info("Created/updated role %s", country)


def assign_mg_roles(client: Client, table: str, rows: list[dict]) -> None:
    updated = [
        {**row, "mg_roles": row["country"]}
        for row in rows
        if row.get("country")
    ]
    df = pd.DataFrame(updated).convert_dtypes()
    client.save_table(table, schema=SCHEMA, data=df)
    log.info("Assigned mg_roles for %d rows in %s", len(updated), table)


def main():
    logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")

    url = os.environ.get("MG_URL", "http://localhost:8080")

    with Client(url) as client:
        client.signin("admin", "admin")
        client.set_schema(SCHEMA)
        biobanks = client.get(BIOBANKS, schema=SCHEMA)
        countries = sorted({row["country"] for row in biobanks if row.get("country")})
        log.info("Found %d distinct countries in %s", len(countries), BIOBANKS)

        for country in countries:
            create_country_role(client, country)

        assign_mg_roles(client, BIOBANKS, biobanks)

        collections = client.get(COLLECTIONS, schema=SCHEMA)
        assign_mg_roles(client, COLLECTIONS, collections)


if __name__ == "__main__":
    main()
