/**
 * Created by Jelmer Veen 2022
 */

import { request } from "graphql-request";
import { IColumn, ITableMetaData } from "../../../meta-data-utils/dist";

class QueryEMX2 {
  tableId = "";
  filters: Record<string, any> = {};
  column = "";
  _schemaTablesInformation: Record<string, any> = {};
  selection = ["id", "name"];
  graphqlUrl = "";
  branch = "root";
  limits: Record<string, number> = {};
  orderings: Record<string, { column: string; direction: "asc" | "dsc" }> = {};
  findInAllColumns = "";
  page: Record<string, any> = {};
  aggregateQuery = false;
  type = "";
  orCount = 0;

  /**
   * @param {string} graphqlUrl the endpoint to query
   */
  constructor(graphqlUrl: string) {
    this.graphqlUrl = graphqlUrl;
  }

  table(tableId: string) {
    /** Tables are always PascalCase */
    this.tableId = tableId;
    return this;
  }

  /**
   * @param {string | string[]} columns
   * When you supply an object the Key is the table or REF property and the value is a string or string array
   */
  select(columns: IColumn[]) {
    let requestedColumns = [];

    if (!Array.isArray(columns)) {
      requestedColumns = [columns];
    } else if (columns) {
      requestedColumns = this._createPathFromObject("", columns);
    } else {
      requestedColumns = this.selection;
    }

    /** column names are always lowercase */
    requestedColumns.forEach((name) => name.toLowerCase());
    this.selection = requestedColumns;
    return this;
  }

  /**
   * Builds a query for use in EMX2 GraphQL api
   * @param {string} tableId
   * @param {string[]} columns
   * @param {object[]} filters
   * @returns GraphQL query string
   */
  async execute() {
    /** Fail fast */
    if (!this.tableId) {
      throw Error(
        `You need to provide ${this.tableId ? "columns" : "a table id"}`
      );
    }

    /** Add all columns that are not linked to other tables */
    if (this.selection.length === 0) {
      const columns = await this.getColumnsForTable(this.tableId);
      this.selection = columns;
    }

    return await request(
      this.graphqlUrl,
      this.aggregateQuery ? this.getAggregateQuery() : this.getQuery()
    );
  }
  /** Executes the query as aggregate */
  aggregate() {
    this.aggregateQuery = true;
    return this;
  }

  getQuery() {
    return this._createQuery(this.tableId, this.selection);
  }

  getAggregateQuery() {
    /** create a hard copy */
    const aggSelection = Object.assign([], this.selection);
    aggSelection.push("count");
    return this._createQuery(`${this.tableId}_agg`, aggSelection);
  }

  /**
   * Gets the table information for the current schema
   */
  async getSchemaTablesInformation() {
    if (Object.keys(this._schemaTablesInformation).length)
      return this._schemaTablesInformation;

    const result: any = await request(
      this.graphqlUrl,
      `
        {
            _schema {
              tables {
                name,
                columns {
                name,
                columnType
                }
              }
            }
          }
        `
    );
    this._schemaTablesInformation = result._schema.tables;

    return this._schemaTablesInformation;
  }

  async saveSetting(key: string, value: any) {
    const valueToUpload =
      typeof value === "string" ? value : JSON.stringify(value);

    const result: any = await request(
      this.graphqlUrl,
      `
      mutation{
        change(
            settings: {
              key: "${key}",
              value: "${encodeURI(valueToUpload)}"
            }
        ){
            message
        }
    }
      `
    );

    return result.change.message;
  }

  /** returns the columns with adjusted names so it can directly be used to query. */
  async getColumnsForTable(tableId: string) {
    await this.getSchemaTablesInformation();

    return this._schemaTablesInformation
      .find((table: ITableMetaData) => table.id === tableId)
      .columns.filter(
        (column: IColumn) =>
          !column.columnType.includes("REF") &&
          !column.columnType.includes("ONTOLOGY")
      );
  }

  /** returns the correct column names and their types. */
  async getColumnsMetadataForTable(tableId: string) {
    await this.getSchemaTablesInformation();

    return this._schemaTablesInformation
      .find((table: ITableMetaData) => table.id === tableId)
      .columns.map((column: IColumn) => ({
        id: column.id,
        columnType: column.columnType,
      }));
  }

  /**
   * If you want to create a nested query, for example { collections: { name: { like: 'lifelines' } } }
   * then column = 'collections', subcolumn = 'name'
   * @param {string} columnId
   * @returns
   */
  where(columnId: string) {
    this.type = "_and";
    this.branch = "root";
    /** always convert to lowercase, else api will error */
    this.column = this._toCamelCase(columnId);
    return this;
  }

  orWhere(columnId: string) {
    /** need to know if we have the array syntax or just object */
    this.orCount = this.orCount + 1;
    this.type = "_or";
    this.branch = "root";
    /** always convert to lowercase, else api will error */
    this.column = this._toCamelCase(columnId);
    return this;
  }

  /**
   * Works as where, but then for nested properties
   * @param {string} columnId
   * @returns
   */
  filter(columnId: string) {
    this.type = "_and";
    const firstDot = columnId.indexOf(".");
    this.branch = this._toCamelCase(columnId.substring(0, firstDot));
    this.column = this._toCamelCase(columnId.substring(firstDot + 1));

    return this;
  }

  /**
   * Works as orWhere, but then for nested properties
   * @param {string} columnId
   * @returns
   */
  orFilter(columnId: string) {
    this.type = "_or";
    const firstDot = columnId.indexOf(".");
    this.branch = this._toCamelCase(columnId.substring(0, firstDot));
    this.column = this._toCamelCase(columnId.substring(firstDot + 1));

    return this;
  }

  /** Resets all filters, useful for when you want to add filters dynamically */
  resetAllFilters() {
    this.filters = {};
    this.findInAllColumns = "";
    this.page = {};
    return this;
  }

  /**
   * @param {string} itemId the name of the table or the nested column
   * @param {int} amount the amount you want to return
   * @returns
   */
  limit(itemId: string, amount: number) {
    let columnOrTable =
      itemId.toLowerCase() === this.tableId.toLowerCase() ? "root" : itemId;
    this.limits[columnOrTable] = amount;
    return this;
  }

  /**
   * @param {string} itemId the name of the table or the nested column
   * @param {int} amount the page you want to have starting at 0
   * @returns
   */
  offset(itemId: string, amount: number) {
    let columnOrTable =
      itemId.toLowerCase() === this.tableId.toLowerCase() ? "root" : itemId;
    this.page[columnOrTable] = amount;
    return this;
  }

  /**
   * @param {string} itemId the name of the table or the nested column
   * @param {string} columnId the name of the column to apply the order to
   * @param {string} direction "asc" or "dsc"
   */
  orderBy(itemId: string, columnId: string, direction: "asc" | "dsc") {
    let columnOrTable =
      itemId.toLowerCase() === this.tableId.toLowerCase()
        ? "root"
        : this._toCamelCase(itemId);
    this.orderings[columnOrTable] = { column: columnId, direction };
    return this;
  }

  /**
   * Additional function, which does the same as search but might be more semantic
   * @param {any} value searches this value across all columns, can only be applied to the top level table
   */
  find(value: any) {
    this.findInAllColumns = value;
    return this;
  }

  /**
   * @param {any} value searches this value across all columns, can only be applied to the top level table
   */
  search(value: any) {
    this.findInAllColumns = value;
    return this;
  }

  /** Text, String, Url, Int, Bool, DateTime Filter */
  equals(value: any) {
    const operator = "equals";

    this._createFilter(operator, value);
    return this;
  }

  /** Text, String, Url, Int, Bool, DateTime Filter */
  in(value: any) {
    /** custom type, to make it into a bracket type query: { like: ["red", "green"] } */
    const operator = "in";
    this._createFilter(operator, value);
    return this;
  }

  /** Text, String, Url, Int, Bool, DateTime Filter */
  notEquals(value: any) {
    const operator = "not_equals";
    this._createFilter(operator, value);
    return this;
  }

  /** Text, String, Url, Filter */
  orLike(value: any) {
    /** custom type, to make it into a bracket type query: { like: ["red", "green"] } */
    const operator = "orLike";
    return this._createFilter(operator, value);
  }

  /** Text, String, Url, Filter */
  like(value: any) {
    const operator = "like";

    return this._createFilter(operator, value);
  }
  /** Text, String, Url, Filter */
  notLike(value: any) {
    const operator = "not_like";

    this._createFilter(operator, value);
    return this;
  }
  /** Text, String, Url, Filter */
  triagramSearch(value: any) {
    const operator = "triagram_search";

    this._createFilter(operator, value);
    return this;
  }
  /** Text, String, Url, Filter */
  textSearch(value: any) {
    const operator = "text_search";

    this._createFilter(operator, value);
    return this;
  }

  /** Int, DateTime Filter */
  between(value: any) {
    const operator = "between";

    this._createFilter(operator, value);
    return this;
  }
  /** Int, DateTime Filter */
  notBetween(value: any) {
    const operator = "not_between";
    this._createFilter(operator, value);
    return this;
  }

  _toPascalCase(value: string) {
    return value[0].toUpperCase() + value.substring(1);
  }

  _toCamelCase(value: string) {
    return value[0].toLowerCase() + value.substring(1);
  }

  _createQuery(root: string, properties: string[]) {
    const rootModifier = this._generateModifiers("root");

    let result = `{\n${root}${rootModifier} {\n`;

    let branches = this._createBranches(properties);
    result = this._generateOutput(branches, 1, this.filters, result);

    result += "  }\n}";

    return result;
  }

  // the best query would be for example"
  // Biobanks(orderby: { name: ASC }, filter: {collections: {_and: [{materials: {name: {like: "BUFFY_COAT"}}}, {materials: {name: {like: "CELL_LINES"}}}]}})
  // but this requires another rewrite ;)
  _createFilterString(filters: Record<string, any>) {
    let filterString = "";

    if (!filters) return filterString;

    if (filters["_and"].length) {
      filterString += `_and: [ ${filters["_and"].join(", ")} ]`;
    }

    if (filters["_or"].length) {
      if (filterString.length) {
        filterString += ", ";
      }

      filterString += `_or: [ ${filters["_or"].join(", ")} ]`;
    }
    return filterString;
  }

  /** Create a nested object to represent the branches and their properties */
  _createBranches(properties: string[]) {
    let branches = {};
    for (let property of properties) {
      let parts = property.split(".");
      let currentBranch: Record<string, any> = branches;

      /** Create nested objects for each part of the property path */
      for (let i = 0; i < parts.length - 1; i++) {
        let part = this._toCamelCase(parts[i].trim());
        if (!currentBranch[part]) {
          currentBranch[part] = {};
        }
        currentBranch = currentBranch[part];
      }

      /** Add the property to the innermost branch */
      let propertyName = this._toCamelCase(parts[parts.length - 1].trim());
      if (propertyName.indexOf(".") >= 0) {
        /** If the property name has a period, it is a branch */
        currentBranch[propertyName] = {};
      } else {
        /** Otherwise, it is a property */
        /** Store the properties in a separate object from the branches */
        if (!currentBranch.properties) {
          currentBranch.properties = {};
        }
        currentBranch.properties[propertyName] = true;
      }
    }
    return branches;
  }

  /** Generate the bit inside parentheses */
  _generateModifiers(property: string) {
    const modifierParts = [];

    modifierParts.push(
      this.findInAllColumns.length && property === "root"
        ? `search: "${this.findInAllColumns}"`
        : ""
    );
    modifierParts.push(
      this.limits[property] ? `limit: ${this.limits[property]}` : ""
    );
    modifierParts.push(
      this.page[property] ? `offset: ${this.page[property]}` : ""
    );
    modifierParts.push(
      this.orderings[property]
        ? `orderby: { ${this.orderings[property].column}: ${this.orderings[
            property
          ].direction.toUpperCase()} }`
        : ""
    );

    const filterString = this._createFilterString(this.filters[property]);

    if (filterString.length) {
      modifierParts.push(`filter: { ${filterString} }`);
    }

    const filledModifiers = modifierParts.filter((f) => f.length > 0);

    return filledModifiers.length ? `(${filledModifiers.join(", ")})` : "";
  }

  _createFilterFromPath(path: string, operator: string, value: any) {
    const valueArray = Array.isArray(value) ? value : [value];

    for (const value of valueArray) {
      /** reverse the path, so we can build it from the inside out */
      const reversedPathParts = path.split(".").reverse();
      let graphqlValue = typeof value === "boolean" ? `${value}` : `"${value}"`;

      /** if it is an _or and a like, concat them */
      const queryType = !this.type ? "_and" : this.type;
      if (operator === "orLike") {
        graphqlValue = `["${valueArray.join('", "')}"]`;
        operator = "like"; /** set it to the correct operator for graphQl */
      }

      if (operator === "in") {
        graphqlValue = `["${valueArray.join('", "')}"]`;
        operator = "equals";
      }

      /** most inner part of the query e.g. 'like: "red" */
      let filter = `{ ${operator}: ${graphqlValue} }`;

      for (const pathPart of reversedPathParts) {
        filter = `{ ${pathPart}: ${filter} }`;
      }
      this.filters[this.branch][queryType].push(filter);

      /** we folded all into one so just return */
      if (graphqlValue.includes("[")) return;
    }
  }

  /** Private function to create the correct filter syntax. */
  _createFilter(operator: string, value: any) {
    if (!this.filters[this.branch]) {
      this.filters[this.branch] = {
        _and: [],
        _or: [],
      };
    }

    this._createFilterFromPath(this.column, operator, value);
    this.column = "";

    return this;
  }

  /** Recursively generate the output string for the branches and their properties */
  _generateOutput(
    branches: Record<string, any>,
    indentationLevel: number,
    filters: Record<string, any>,
    result: string
  ) {
    let indentation = "    ".repeat(indentationLevel);

    /** Add properties first */
    if (branches.properties) {
      let properties = branches.properties;
      for (let propertyName in properties) {
        if (properties[propertyName] === true) {
          result += `${indentation}${propertyName},\n`;
        }
      }

      result = `${result.substring(0, result.length - 2)}\n`;
    }

    /** Add the branches and their properties */
    for (let branchName in branches) {
      /** continue with the query by adding a comma to the end of the property */
      let branch = branches[branchName];
      if (branchName !== "properties") {
        result = `${result.substring(0, result.length - 1)},\n`;
        const branchModifiers = this._generateModifiers(branchName);
        /** Only add branches, not properties */
        result += `${indentation}${branchName}${branchModifiers || ""} {\n`;

        result = this._generateOutput(
          branch,
          indentationLevel + 1,
          filters,
          result
        );
        result += indentation + "}\n";
      }
    }

    return result;
  }

  _createPathFromObject(
    path: string,
    properties: any[],
    requestedColumns: any[] = []
  ) {
    for (const property of properties) {
      if (typeof property === "object") {
        const refProperty = Object.keys(property)[0];
        const nextPath = path ? `${path}.${refProperty}` : refProperty;
        this._createPathFromObject(
          nextPath,
          property[refProperty],
          requestedColumns
        );
      } else {
        if (!path || path.length === 0) {
          requestedColumns.push(property);
        } else {
          requestedColumns.push(`${path}.${property}`);
        }
      }
    }
    return requestedColumns;
  }
}

export default QueryEMX2;
