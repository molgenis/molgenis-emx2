import { describe, it, expect } from "vitest";
import {
  buildContextualListPath,
  buildCrumbs,
  buildNestedRecordPath,
  ownLabelColumns,
  parseContextualListPath,
  parseRecordPath,
} from "../../../app/utils/recordPath";
import { getTitleText } from "../../../app/utils/displayUtils";
import type {
  FieldNode,
  OperationDefinitionNode,
  SelectionNode,
} from "graphql";
import metadataQuery from "../../../app/gql/metadata";
import type {
  IColumn,
  IRow,
  ISchemaMetaData,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";

function fieldsSelectedByMetadataQuery(path: string[]): string[] {
  let selections: readonly SelectionNode[] = (
    metadataQuery.definitions[0] as OperationDefinitionNode
  ).selectionSet.selections;
  for (const fieldName of path) {
    const field = selections.find(
      (selection): selection is FieldNode =>
        selection.kind === "Field" && selection.name.value === fieldName
    );
    if (!field?.selectionSet) return [];
    selections = field.selectionSet.selections;
  }
  return selections
    .filter((selection): selection is FieldNode => selection.kind === "Field")
    .map((field) => field.name.value);
}

function withOnlyFields<T extends object>(source: T, fieldNames: string[]): T {
  return Object.fromEntries(
    Object.entries(source).filter(([fieldName]) =>
      fieldNames.includes(fieldName)
    )
  ) as T;
}

function asFetchedByMetadataQuery(schema: ISchemaMetaData): ISchemaMetaData {
  const tableFields = fieldsSelectedByMetadataQuery(["_schema", "tables"]);
  const columnFields = fieldsSelectedByMetadataQuery([
    "_schema",
    "tables",
    "columns",
  ]);
  return {
    ...withOnlyFields(schema, fieldsSelectedByMetadataQuery(["_schema"])),
    tables: schema.tables.map((table) => ({
      ...withOnlyFields(table, tableFields),
      columns: table.columns.map((column) =>
        withOnlyFields(column, columnFields)
      ),
    })),
  };
}

function column(id: string, overrides: Partial<IColumn> = {}): IColumn {
  return { id, label: id, columnType: "STRING", ...overrides };
}

function refColumn(
  id: string,
  refTableId: string,
  overrides: Partial<IColumn> = {}
): IColumn {
  return column(id, {
    columnType: "REF",
    refTableId,
    refSchemaId: "catalogue",
    required: true,
    ...overrides,
  });
}

function partsColumn(
  id: string,
  refTableId: string,
  refBackId: string,
  overrides: Partial<IColumn> = {}
): IColumn {
  return column(id, {
    columnType: "PARTS",
    refTableId,
    refSchemaId: "catalogue",
    refBackId,
    ...overrides,
  });
}

function table(
  id: string,
  columns: IColumn[],
  overrides: Partial<ITableMetaData> = {}
): ITableMetaData {
  return {
    id,
    schemaId: "catalogue",
    name: id,
    label: id,
    tableType: "DATA",
    columns,
    ...overrides,
  };
}

function catalogueSchema(): ISchemaMetaData {
  return {
    id: "catalogue",
    label: "Catalogue",
    tables: [
      table("Resources", [
        column("id", { key: 1, required: true }),
        column("name", { columnType: "TEXT", key: 3, required: true }),
        partsColumn("tables", "Tables", "resource"),
      ]),
      table(
        "Collections",
        [
          column("id", { key: 1, required: true, inherited: true }),
          column("name", {
            columnType: "TEXT",
            key: 3,
            required: true,
            inherited: true,
          }),
          partsColumn("tables", "Tables", "resource", { inherited: true }),
          partsColumn("subpopulations", "Subpopulations", "resource"),
          partsColumn("collectionEvents", "CollectionEvents", "resource"),
        ],
        { inheritId: "Resources" }
      ),
      table("Tables", [
        refColumn("resource", "Resources", { key: 1 }),
        column("name", { key: 1, required: true }),
        partsColumn("variables", "Variables", "table"),
      ]),
      table("Variables", [
        refColumn("resource", "Resources", { key: 1 }),
        refColumn("table", "Tables", { key: 1, refLinkId: "resource" }),
        column("name", { key: 1, required: true }),
        partsColumn("permittedValues", "VariableValues", "variable"),
      ]),
      table("VariableValues", [
        refColumn("resource", "Resources", { key: 1 }),
        refColumn("table", "Tables", { key: 1, refLinkId: "resource" }),
        refColumn("variable", "Variables", { key: 1, refLinkId: "table" }),
        column("value", { key: 1, required: true }),
      ]),
      table("Subpopulations", [
        refColumn("resource", "Resources", { key: 1 }),
        column("name", { key: 1, required: true }),
      ]),
      table("CollectionEvents", [
        refColumn("resource", "Resources", { key: 1 }),
        column("name", { key: 1, required: true }),
      ]),
      table("Organisations", [
        refColumn("resource", "Resources", { key: 1 }),
        column("name", { key: 1, required: true }),
        partsColumn("contacts", "Contacts", "organisation"),
      ]),
      table("Contacts", [
        refColumn("organisation", "Organisations", { key: 1 }),
        column("name", { key: 1, required: true }),
      ]),
      table("Documents", [
        column("name", { key: 1, required: true }),
        column("version", { key: 1, required: true }),
        partsColumn("attachments", "Attachments", "document"),
      ]),
      table("Attachments", [
        refColumn("document", "Documents", { key: 1 }),
        column("name", { key: 1, required: true }),
      ]),
      table("Observations", [
        column("id", { key: 1, required: true }),
        partsColumn("findings", "Findings", "observation"),
      ]),
      table(
        "ClinicalObservations",
        [
          column("id", { key: 1, required: true, inherited: true }),
          partsColumn("findings", "Findings", "observation", {
            inherited: true,
          }),
        ],
        { name: "Clinical observations", inheritId: "Observations" }
      ),
      table("Findings", [
        refColumn("observation", "Observations", { key: 1 }),
        column("name", { key: 1, required: true }),
      ]),
      table(
        "ImagingFindings",
        [
          refColumn("observation", "Observations", {
            key: 1,
            inherited: true,
          }),
          column("name", { key: 1, required: true, inherited: true }),
        ],
        { name: "Imaging findings", inheritId: "Findings" }
      ),
    ],
  };
}

const resourcesRow: IRow = { id: "c1", name: "Cohort One" };
const collectionRow: IRow = {
  mg_tableclass: "catalogue.Collections",
  id: "c1",
  name: "Cohort One",
};
const tablesRow: IRow = { resource: { id: "c1" }, name: "t1" };
const variablesRow: IRow = {
  resource: { id: "c1" },
  table: { resource: { id: "c1" }, name: "t1" },
  name: "v1",
};
const clinicalObservationFindingRow: IRow = {
  observation: { mg_tableclass: "catalogue.Clinical observations", id: "o1" },
  name: "f1",
};
const imagingFindingRow: IRow = {
  mg_tableclass: "catalogue.Imaging findings",
  observation: { id: "o1" },
  name: "f1",
};
const organisationsRow: IRow = { resource: { id: "c1" }, name: "o1" };
const documentsRow: IRow = { name: "d1", version: "v2" };
const subpopulationsRow: IRow = { resource: { id: "c1" }, name: "s1" };
const collectionEventsRow: IRow = { resource: { id: "c1" }, name: "e1" };
const variableValuesRow: IRow = {
  resource: { id: "c1" },
  table: { resource: { id: "c1" }, name: "t1" },
  variable: {
    resource: { id: "c1" },
    table: { resource: { id: "c1" }, name: "t1" },
    name: "v1",
  },
  value: "vv1",
};

const parityDeclarations = [
  {
    declaration: "Resources.tables",
    tableId: "Tables",
    row: tablesRow,
    path: "/catalogue/Resources/c1/tables/t1",
    key: { resource: { id: "c1" }, name: "t1" },
  },
  {
    declaration: "Tables.variables",
    tableId: "Variables",
    row: variablesRow,
    path: "/catalogue/Resources/c1/tables/t1/variables/v1",
    key: {
      resource: { id: "c1" },
      table: { resource: { id: "c1" }, name: "t1" },
      name: "v1",
    },
  },
  {
    declaration: "Collections.subpopulations",
    tableId: "Subpopulations",
    row: subpopulationsRow,
    path: "/catalogue/Collections/c1/subpopulations/s1",
    key: { resource: { id: "c1" }, name: "s1" },
  },
  {
    declaration: "Collections.collection events",
    tableId: "CollectionEvents",
    row: collectionEventsRow,
    path: "/catalogue/Collections/c1/collectionEvents/e1",
    key: { resource: { id: "c1" }, name: "e1" },
  },
  {
    declaration: "Variables.permitted values",
    tableId: "VariableValues",
    row: variableValuesRow,
    path: "/catalogue/Resources/c1/tables/t1/variables/v1/permittedValues/vv1",
    key: {
      resource: { id: "c1" },
      table: { resource: { id: "c1" }, name: "t1" },
      variable: {
        resource: { id: "c1" },
        table: { resource: { id: "c1" }, name: "t1" },
        name: "v1",
      },
      value: "vv1",
    },
  },
];

describe("the runtime metadata query", () => {
  it("selects the inheritance fields the record path utilities read", () => {
    expect(fieldsSelectedByMetadataQuery(["_schema", "tables"])).toEqual(
      expect.arrayContaining(["name", "inheritId"])
    );
    expect(
      fieldsSelectedByMetadataQuery(["_schema", "tables", "columns"])
    ).toContain("inherited");
  });
});

describe("buildNestedRecordPath on the metadata the runtime query returns", () => {
  it("names the root segment after the subclass even when its table name differs from its id", () => {
    expect(
      buildNestedRecordPath(
        asFetchedByMetadataQuery(catalogueSchema()),
        "Findings",
        clinicalObservationFindingRow
      )
    ).toBe("/catalogue/ClinicalObservations/o1/findings/f1");
  });
});

describe("parseRecordPath", () => {
  it("decodes alternating table and key segments into one table plus primary key per level, deepest level last", () => {
    expect(
      parseRecordPath(
        catalogueSchema(),
        "/catalogue/Resources/c1/tables/t1/variables/v1"
      )
    ).toEqual([
      {
        tableSegment: "Resources",
        keySegment: "c1",
        tableId: "Resources",
        key: { id: "c1" },
      },
      {
        tableSegment: "tables",
        keySegment: "t1",
        tableId: "Tables",
        key: { resource: { id: "c1" }, name: "t1" },
      },
      {
        tableSegment: "variables",
        keySegment: "v1",
        tableId: "Variables",
        key: {
          resource: { id: "c1" },
          table: { resource: { id: "c1" }, name: "t1" },
          name: "v1",
        },
      },
    ]);
  });

  it("decodes the flat keys form to the same table and primary key as the nested form of that row", () => {
    const schema = catalogueSchema();
    const nested = parseRecordPath(schema, "/catalogue/Resources/c1/tables/t1");
    const flat = parseRecordPath(
      schema,
      `/catalogue/Tables/t1?keys=${encodeURIComponent(
        JSON.stringify({ resource: { id: "c1" }, name: "t1" })
      )}`
    );
    expect(flat?.[flat.length - 1]?.key).toEqual({
      resource: { id: "c1" },
      name: "t1",
    });
    expect(flat?.[flat.length - 1]?.key).toEqual(
      nested?.[nested.length - 1]?.key
    );
    expect(flat?.[flat.length - 1]?.tableId).toBe("Tables");
  });

  it("decodes a subclass root segment through the parts column the subclass inherits", () => {
    const levels = parseRecordPath(
      catalogueSchema(),
      "/catalogue/Collections/c1/tables/t1/variables/v1"
    );
    expect(levels?.map((level) => level.tableId)).toEqual([
      "Collections",
      "Tables",
      "Variables",
    ]);
    expect(levels?.[levels.length - 1]?.key).toEqual({
      resource: { id: "c1" },
      table: { resource: { id: "c1" }, name: "t1" },
      name: "v1",
    });
  });

  it("resolves the subclass root and the declaring table root to the same record", () => {
    const schema = catalogueSchema();
    const viaSubclass = parseRecordPath(
      schema,
      "/catalogue/Collections/c1/tables/t1/variables/v1"
    );
    const viaDeclaringTable = parseRecordPath(
      schema,
      "/catalogue/Resources/c1/tables/t1/variables/v1"
    );

    expect(viaSubclass?.[viaSubclass.length - 1]).toEqual({
      tableSegment: "variables",
      keySegment: "v1",
      tableId: "Variables",
      key: {
        resource: { id: "c1" },
        table: { resource: { id: "c1" }, name: "t1" },
        name: "v1",
      },
    });
    expect(viaSubclass?.[viaSubclass.length - 1]).toEqual(
      viaDeclaringTable?.[viaDeclaringTable.length - 1]
    );
  });

  it("returns null instead of throwing when the flat keys parameter is not valid JSON", () => {
    expect(
      parseRecordPath(catalogueSchema(), "/catalogue/Tables/t1?keys=%7Bbroken")
    ).toBe(null);
  });

  it("returns null instead of throwing when a table segment is not in the schema", () => {
    expect(parseRecordPath(catalogueSchema(), "/catalogue/Nope/c1")).toBe(null);
    expect(
      parseRecordPath(catalogueSchema(), "/catalogue/Resources/c1/nope/x1")
    ).toBe(null);
  });
});

describe("encode and decode are inverses for every parity declaration", () => {
  it.each(parityDeclarations)(
    "roundtrips $declaration",
    ({ tableId, row, path, key }) => {
      expect(buildNestedRecordPath(catalogueSchema(), tableId, row)).toBe(path);
      const levels = parseRecordPath(catalogueSchema(), path);
      expect(levels?.[levels.length - 1]).toEqual({
        tableSegment: path.split("/").at(-2),
        keySegment: path.split("/").at(-1),
        tableId,
        key,
      });
    }
  );
});

describe("buildCrumbs", () => {
  it("emits one crumb per URL segment plus the schema, each url a strict prefix of the next, ending on the record path", () => {
    const schema = catalogueSchema();
    const path = "/catalogue/Resources/c1/tables/t1/variables/v1";
    const levels = parseRecordPath(schema, path);

    expect(buildCrumbs(schema, levels ?? [])).toEqual([
      { url: "/catalogue", label: "Catalogue" },
      { url: "/catalogue/Resources", label: "Resources" },
      { url: "/catalogue/Resources/c1", label: "c1" },
      { url: "/catalogue/Resources/c1/tables", label: "Tables" },
      { url: "/catalogue/Resources/c1/tables/t1", label: "t1" },
      {
        url: "/catalogue/Resources/c1/tables/t1/variables",
        label: "Variables",
      },
      { url: path, label: "v1" },
    ]);
  });

  it("labels each record crumb with the display label supplied for that level, keeping the key segment where none is supplied", () => {
    const schema = catalogueSchema();
    const path = "/catalogue/Resources/c1/tables/t1/variables/v1";
    const levels = parseRecordPath(schema, path);

    expect(
      buildCrumbs(schema, levels ?? [], ["Cohort One", undefined, "Age"]).map(
        (crumb) => crumb.label
      )
    ).toEqual([
      "Catalogue",
      "Resources",
      "Cohort One",
      "Tables",
      "t1",
      "Variables",
      "Age",
    ]);
  });
});

describe("ownLabelColumns", () => {
  const nestedPath = "/catalogue/Resources/c1/tables/t1/variables/v1";

  it("titles a nested level from its own key alone, leaving the ancestors to the preceding segments", () => {
    const schema = catalogueSchema();
    const levels = parseRecordPath(schema, nestedPath) ?? [];

    expect(
      getTitleText(ownLabelColumns(schema, levels[1]!, levels[0]), tablesRow)
    ).toBe("t1");
    expect(
      getTitleText(ownLabelColumns(schema, levels[2]!, levels[1]), variablesRow)
    ).toBe("v1");
  });

  it("titles a level nested two parts deep from its own key alone", () => {
    const schema = catalogueSchema();
    const levels =
      parseRecordPath(schema, `${nestedPath}/permittedValues/vv1`) ?? [];

    expect(
      getTitleText(
        ownLabelColumns(schema, levels[3]!, levels[2]),
        variableValuesRow
      )
    ).toBe("vv1");
  });

  it("keeps every column of a root level, which no preceding segment gives context to", () => {
    const schema = catalogueSchema();
    const levels = parseRecordPath(schema, nestedPath) ?? [];

    expect(
      ownLabelColumns(schema, levels[0]!).map((column) => column.id)
    ).toEqual(["id", "name", "tables"]);
  });
});

describe("buildNestedRecordPath", () => {
  it("returns the nested path for a row that sits in a parts chain", () => {
    expect(
      buildNestedRecordPath(catalogueSchema(), "Variables", variablesRow)
    ).toBe("/catalogue/Resources/c1/tables/t1/variables/v1");
  });

  it("keeps the subclass root when the parent row is a subclass instance", () => {
    expect(
      buildNestedRecordPath(catalogueSchema(), "Tables", {
        resource: { mg_tableclass: "catalogue.Collections", id: "c1" },
        name: "t1",
      })
    ).toBe("/catalogue/Collections/c1/tables/t1");
  });

  it("finds the parts declaration on a base table when the row's own table is a subclass of it", () => {
    expect(
      buildNestedRecordPath(catalogueSchema(), "Findings", imagingFindingRow)
    ).toBe("/catalogue/Observations/o1/findings/f1");
  });

  it("returns null for a row with no parts chain so callers keep their flat url", () => {
    expect(
      buildNestedRecordPath(catalogueSchema(), "Resources", resourcesRow)
    ).toBe(null);
    expect(
      buildNestedRecordPath(catalogueSchema(), "Collections", collectionRow)
    ).toBe(null);
  });

  it("returns null for a chain level whose own key no single segment can carry", () => {
    expect(
      buildNestedRecordPath(
        catalogueSchema(),
        "Organisations",
        organisationsRow
      )
    ).toBe(null);
    expect(
      buildNestedRecordPath(catalogueSchema(), "Documents", documentsRow)
    ).toBe(null);
  });
});

describe("parseContextualListPath", () => {
  it("decodes a path ending on a parts column into the child table, its part-of link and the parent scope", () => {
    expect(
      parseContextualListPath(
        catalogueSchema(),
        "/catalogue/Resources/c1/tables"
      )
    ).toEqual({
      levels: [
        {
          tableSegment: "Resources",
          keySegment: "c1",
          tableId: "Resources",
          key: { id: "c1" },
        },
      ],
      tableId: "Tables",
      partsColumnId: "tables",
      partOfColumnId: "resource",
      contextColumnIds: ["resource"],
    });
  });

  it("names every child key column the url already pins, not just the part-of column", () => {
    const contextualList = parseContextualListPath(
      catalogueSchema(),
      "/catalogue/Resources/c1/tables/t1/variables"
    );
    expect(contextualList?.tableId).toBe("Variables");
    expect(contextualList?.partOfColumnId).toBe("table");
    expect(contextualList?.contextColumnIds).toEqual(["resource", "table"]);
    expect(contextualList?.levels.map((level) => level.tableId)).toEqual([
      "Resources",
      "Tables",
    ]);
    expect(contextualList?.levels.at(-1)?.key).toEqual({
      resource: { id: "c1" },
      name: "t1",
    });
  });

  it("decodes a parts column the parent subclass inherits", () => {
    const contextualList = parseContextualListPath(
      catalogueSchema(),
      "/catalogue/Collections/c1/subpopulations"
    );
    expect(contextualList?.tableId).toBe("Subpopulations");
    expect(contextualList?.partOfColumnId).toBe("resource");
    expect(contextualList?.levels.at(-1)?.tableId).toBe("Collections");
  });

  it("returns null for a record path, so a record never renders as a list", () => {
    expect(
      parseContextualListPath(
        catalogueSchema(),
        "/catalogue/Resources/c1/tables/t1"
      )
    ).toBe(null);
  });

  it("returns null when the last segment is not a parts column of the parent", () => {
    expect(
      parseContextualListPath(catalogueSchema(), "/catalogue/Resources/c1/name")
    ).toBe(null);
    expect(
      parseContextualListPath(catalogueSchema(), "/catalogue/Resources/c1/nope")
    ).toBe(null);
    expect(
      parseContextualListPath(
        catalogueSchema(),
        "/catalogue/Resources/c1/variables"
      )
    ).toBe(null);
  });

  it("decodes what buildContextualListPath encodes", () => {
    const schema = catalogueSchema();
    const path = buildContextualListPath(
      schema,
      "Tables",
      tablesRow,
      "variables"
    );
    expect(path).toBe("/catalogue/Resources/c1/tables/t1/variables");
    const contextualList = parseContextualListPath(schema, path ?? "");
    expect(contextualList?.tableId).toBe("Variables");
    expect(contextualList?.levels.at(-1)?.key).toEqual({
      resource: { id: "c1" },
      name: "t1",
    });
  });
});

describe("buildContextualListPath", () => {
  it("points a parts collection at the parent scoped list url", () => {
    expect(
      buildContextualListPath(
        catalogueSchema(),
        "Resources",
        resourcesRow,
        "tables"
      )
    ).toBe("/catalogue/Resources/c1/tables");
  });

  it("returns null rather than an ambiguous url when the parent level has a composite own key", () => {
    expect(
      buildContextualListPath(
        catalogueSchema(),
        "Organisations",
        organisationsRow,
        "contacts"
      )
    ).toBe(null);
    expect(
      buildContextualListPath(
        catalogueSchema(),
        "Documents",
        documentsRow,
        "attachments"
      )
    ).toBe(null);
  });

  it("returns null rather than a wrong url when the parent scope cannot be resolved", () => {
    expect(
      buildContextualListPath(
        catalogueSchema(),
        "Resources",
        resourcesRow,
        "name"
      )
    ).toBe(null);
    expect(
      buildContextualListPath(
        catalogueSchema(),
        "Tables",
        { name: "t1" },
        "variables"
      )
    ).toBe(null);
  });
});
