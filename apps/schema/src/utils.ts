import gql from "graphql-tag";
import { deepClone, ITableMetaData, IColumn } from "molgenis-components";

export const schemaQuery = gql`
  {
    _session {
      schemas
      roles
    }
    _schema {
      name
      tables {
        name
        schemaName
        tableType
        inheritName
        labels {
          locale
          value
        }
        descriptions {
          locale
          value
        }
        semantics
        columns {
          name
          labels {
            locale
            value
          }
          table
          position
          columnType
          inherited
          key
          refSchemaName
          refTableName
          refLinkName
          refBackName
          refLabel
          required
          readonly
          defaultValue
          descriptions {
            locale
            value
          }
          semantics
          validation
          visible
          computed
        }
      }
    }
  }
`;

export function addOldNamesAndRemoveMeta(rawSchema: any) {
  //deep copy to not change the input
  const schema = deepClone(rawSchema);
  if (schema) {
    //normal tables
    let tables = !schema.tables
      ? []
      : schema.tables.filter((table) => table.tableType !== "ONTOLOGIES" && table.schemaName === schema.name);
    tables.forEach((t) => {
      t.oldName = t.name;
      if (t.columns) {
        t.columns = t.columns
          .filter((c) => !c.name.startsWith("mg_"))
          .map((c) => {
            c.oldName = c.name;
            return c;
          })
          .filter((c) => !c.inherited);
      } else {
        t.columns = [];
      }
    });
    schema.ontologies = !schema.tables
      ? []
      : schema.tables.filter(
          (table) =>
            table.tableType === "ONTOLOGIES" && table.schemaName === schema.name
        );
    //set old name so we can delete them properly
    schema.ontologies.forEach((o) => {
      o.oldName = o.name;
    });
    schema.tables = tables;
  }

  return schema;
}

export function convertToSubclassTables(rawSchema: any) {
  //deep copy to not change the input
  const schema = deepClone(rawSchema);
  //columns of subclasses should be put in root tables, sorted by position
  // this because position can only edited in context of root table
  schema.tables.forEach((table) => {
    if (table.inheritName === undefined) {
      getSubclassTables(schema, table.name).forEach((subclass) => {
        //get columns from subclass tables
        table.columns.push(...subclass.columns);
        //remove the columns from subclass table
        subclass.columns = [];
        subclass.oldName = subclass.name;
        //add subclass to root table
        if (!table.subclasses) {
          table.subclasses = [subclass];
        } else {
          table.subclasses.push(subclass);
        }
      });
    }
    //sort
    table.columns.sort((a, b) => a.position - b.position);
  });
  //remove the subclass tables
  schema.tables = schema.tables.filter((table) => table.inherit === undefined);
  return schema;
}

export function getSubclassTables(schema, tableName) {
  let subclasses = schema.tables.filter(
    (table) => table.inheritName === tableName
  );
  return subclasses.concat(
    subclasses
      .map((table) => {
        return getSubclassTables(schema, table.name);
      })
      .flat(1)
  );
}

export function convertToCamelCase(string: string): string {
  if (!string) return string;
  const words = string.trim().split(/\s+/);
  let result = "";
  words.forEach((word: string, index: number) => {
    if (index === 0) {
      result += word.charAt(0).toLowerCase();
    } else {
      result += word.charAt(0).toUpperCase();
    }
    if (word.length > 1) {
      result += word.slice(1);
    }
  });
  return result;
}

export function convertToPascalCase(string: string): string {
  if (!string) return string;
  const words = string.trim().split(/\s+/);
  let result = "";
  words.forEach((word: string) => {
    result += word.charAt(0).toUpperCase();
    if (word.length > 1) {
      result += word.slice(1);
    }
  });
  return result;
}

export function getLocalizedLabel(
  tableOrColumnMetadata: ITableMetaData | IColumn,
  locale?: string
): string {
  let label;
  if (tableOrColumnMetadata?.labels) {
    label = tableOrColumnMetadata.labels.find(
      (el) => el.locale === locale
    )?.value;
    if (!label) {
      label = tableOrColumnMetadata.labels.find(
        (el) => el.locale === "en"
      )?.value;
    }
  }
  if (!label) {
    label = tableOrColumnMetadata.name;
  }
  return label;
}

export function getLocalizedDescription(
  tableOrColumnMetadata: ITableMetaData | IColumn,
  locale: string
): string | undefined {
  if (tableOrColumnMetadata.descriptions) {
    return tableOrColumnMetadata.descriptions.find((el) => el.locale === locale)
      ?.value;
  }
}

export function addTableIdsLabelsDescription(originalTable: ITableMetaData) {
  const table = deepClone(originalTable);
  table.id = convertToPascalCase(table.name);
  table.label = getLocalizedLabel(table);
  table.description = getLocalizedDescription(table, "en");
  table.schemaId = table.schemaName;
  table.inheritId = convertToPascalCase(table.inheritName);
  table.columns = table.columns.map((column) => {
    column.id = convertToCamelCase(column.name);
    column.label = getLocalizedLabel(column, "en") || column.name;
    column.description = getLocalizedDescription(column, "en");
    column.refTableId = convertToPascalCase(column.refTableName);
    column.refLinkId = convertToCamelCase(column.refLinkName);
    column.refSchemaId = column.refSchemaName; //todo, might change later
    column.refBackId = convertToCamelCase(column.refBackName);
    return column;
  });
  return table;
}
