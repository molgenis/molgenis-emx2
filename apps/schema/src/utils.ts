import gql from "graphql-tag";
import { deepClone } from "molgenis-components";
import { renderSvg } from "nomnoml";

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
        tableType
        inherit
        externalSchema
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
          id
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
          refSchema
          refTable
          refLink
          refBack
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
      : schema.tables.filter(
          (table) =>
            table.tableType !== "ONTOLOGIES" &&
            table.externalSchema === schema.name
        );
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
            table.tableType === "ONTOLOGIES" &&
            table.externalSchema === schema.name
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
    if (table.inherit === undefined) {
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
  let subclasses = schema.tables.filter((table) => table.inherit === tableName);
  return subclasses.concat(
    subclasses
      .map((table) => {
        return getSubclassTables(schema, table.name);
      })
      .flat(1)
  );
}
