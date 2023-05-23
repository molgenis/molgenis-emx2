/**
 * Created by Jelmer Veen 2022
 */

import { request } from "graphql-request";

class QueryEMX2 {
  tableName = "";
  filters = {};
  column = "";
  _schemaTablesInformation = {};
  selection = ["id", "name"];
  graphqlUrl = "";
  branch = "root";
  limits = {};
  orderings = {};
  findInAllColumns = "";
  page = {};
  aggregateQuery = false;

  /**
   * @param {string} graphqlUrl the endpoint to query
   */
  constructor(graphqlUrl) {
    this.graphqlUrl = graphqlUrl;
  }

  table (tableName) {
    /** Tables are always PascalCase */
    this.tableName = this._toPascalCase(tableName);
    return this;
  }

  /**
   * @param {string | string[]} columns
   * When you supply an object the Key is the table or REF property and the value is a string or string array
   */
  select (columns) {
    let requestedColumns = [];

    if (!Array.isArray(columns)) {
      requestedColumns = [columns];
    } else if (columns) {
      requestedColumns = this._createPathFromObject("", columns);
    }
    else {
      requestedColumns = this.selection
    }

    /** column names are always lowercase */
    requestedColumns.forEach((name) => name.toLowerCase());
    this.selection = requestedColumns;
    return this;
  }

  /**
   * Builds a query for use in EMX2 GraphQL api
   * @param {string} tableName
   * @param {string[]} columns
   * @param {object[]} filters
   * @returns GraphQL query string
   */
  async execute () {
    /** Fail fast */
    if (!this.tableName) {
      throw Error(
        `You need to provide ${this.tableName ? "columns" : "a table name"}`
      );
    }

    /** Add all columns that are not linked to other tables */
    if (this.selection.length === 0) {
      const columns = await this.getColumnsForTable(this.tableName);
      this.selection = columns;
    }

    return await request(
      this.graphqlUrl,
      this.aggregateQuery ? this.getAggregateQuery() : this.getQuery()
    );
  }
  /** Executes the query as aggregate */
  aggregate () {
    this.aggregateQuery = true;
    return this;
  }

  getQuery () {
    return this._createQuery(this.tableName, this.selection);
  }

  getAggregateQuery () {
    /** create a hard copy */
    const aggSelection = Object.assign([], this.selection);
    aggSelection.push("count");
    return this._createQuery(`${this.tableName}_agg`, aggSelection);
  }

  /**
   * Gets the table information for the current schema
   */
  async getSchemaTablesInformation () {
    if (Object.keys(this._schemaTablesInformation).length)
      return this._schemaTablesInformation;

    const result = await request(
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

  /** returns the columns with adjusted names so it can directly be used to query. */
  async getColumnsForTable (tableName) {
    await this.getSchemaTablesInformation();

    return this._schemaTablesInformation
      .find((table) => table.name === tableName)
      .columns.filter(
        (column) =>
          !column.columnType.includes("REF") &&
          !column.columnType.includes("ONTOLOGY")
      )
      .map((column) => this._toCamelCase(column.name.replace(/\s+/g, "")));
  }

  /** returns the correct column names and their types. */
  async getColumnsMetadataForTable (tableName) {
    await this.getSchemaTablesInformation();

    return this._schemaTablesInformation
      .find((table) => table.name === tableName)
      .columns.map((column) => ({
        name: this._toCamelCase(column.name.replace(/\s+/g, "")),
        columnType: column.columnType,
      }));
  }

  matchAll (column, table = 'root') {
    this.type = "_matchAll"
    this.branch = table
    this.column = this._toCamelCase(column);
    return this;
  }

  /**
   * If you want to create a nested query, for example { collections: { name: { like: 'lifelines' } } }
   * then column = 'collections', subcolumn = 'name'
   * @param {string} column
   * @param {string} subcolumn
   * @returns
   */
  where (column) {
    this.type = "_and";
    this.branch = "root";
    /** always convert to lowercase, else api will error */
    this.column = this._toCamelCase(column);
    return this;
  }

  orWhere (column) {
    /** need to know if we have the array syntax or just object */
    this.orCount = this.orCount + 1;
    this.type = "_or";
    this.branch = "root";
    /** always convert to lowercase, else api will error */
    this.column = this._toCamelCase(column);
    return this;
  }

  /**
   * Works as where, but then for nested properties
   * @param {string} column
   * @param {string} nestedColumn
   * @returns
   */
  filter (column) {
    this.type = "_and";
    const firstDot = column.indexOf(".");
    this.branch = this._toCamelCase(column.substring(0, firstDot));
    this.column = this._toCamelCase(column.substring(firstDot + 1));

    return this;
  }

  /**
   * Works as orwhere, but then for nested properties
   * @param {string} column
   * @param {string} nestedColumn
   * @returns
   */
  subfilter (column) {
    const firstDot = column.indexOf(".");
    const subcolumn = column.substring(firstDot + 1);
    let secondDot = subcolumn.indexOf(".");
    this.type = "_or"

    if (secondDot > 0) {
      this.branch = this._toCamelCase(subcolumn.substring(0, secondDot));
      this.column = this._toCamelCase(subcolumn.substring(secondDot + 1));
    }
    else {
      const queryParts = column.split('.');
      this.branch = this._toCamelCase(queryParts[0]);
      this.column = this._toCamelCase(queryParts[1]);
    }

    return this;
  }
  /** Resets all filters, useful for when you want to add filters dynamically */
  resetAllFilters () {
    this.filters = {}
    this.findInAllColumns = "";
    this.page = {};
    return this;
  }

  /**
   * @param {string} item the name of the table or the nested column
   * @param {int} amount the amount you want to return
   * @returns
   */
  limit (item, amount) {
    let columnOrTable =
      item.toLowerCase() === this.tableName.toLowerCase()
        ? "root"
        : this._toCamelCase(item);
    this.limits[columnOrTable] = amount;
    return this;
  }

  /**
   * @param {string} item the name of the table or the nested column
   * @param {int} amount the page you want to have starting at 0
   * @returns
   */
  offset (item, amount) {
    let columnOrTable =
      item.toLowerCase() === this.tableName.toLowerCase()
        ? "root"
        : this._toCamelCase(item);
    this.page[columnOrTable] = amount;
    return this;
  }

  /**
   * @param {string} item the name of the table or the nested column
   * @param {string} column the name of the column to apply the order to
   * @param {string} direction "asc" or "dsc"
   */
  orderBy (item, column, direction) {
    let columnOrTable =
      item.toLowerCase() === this.tableName.toLowerCase()
        ? "root"
        : this._toCamelCase(item);
    this.orderings[columnOrTable] = { column, direction };
    return this;
  }

  /**
   * Additional function, which does the same as search but might be more semantic
   * @param {any} value searches this value across all columns, can only be applied to the top level table
   */
  find (value) {
    this.findInAllColumns = value;
    return this;
  }

  /**
   * @param {any} value searches this value across all columns, can only be applied to the top level table
   */
  search (value) {
    this.findInAllColumns = value;
    return this;
  }

  /** Text, String, Url, Int, Bool, Datetime Filter */
  equals (value) {
    const operator = "equals";

    this._createFilter(operator, value);
    return this;
  }
  /** Text, String, Url, Int, Bool, Datetime Filter */
  notEquals (value) {
    const operator = "not_equals";

    this._createFilter(operator, value);
    return this;
  }

  /** Text, String, Url, Filter */
  like (value) {
    const operator = "like";

    return this._createFilter(operator, value);
  }
  /** Text, String, Url, Filter */
  notLike (value) {
    const operator = "not_like";

    this._createFilter(operator, value);
    return this;
  }
  /** Text, String, Url, Filter */
  triagramSearch (value) {
    const operator = "triagram_search";

    this._createFilter(operator, value);
    return this;
  }
  /** Text, String, Url, Filter */
  textSearch (value) {
    const operator = "text_search";

    this._createFilter(operator, value);
    return this;
  }

  /** Int, Datetime Filter */
  between (value) {
    const operator = "between";

    this._createFilter(operator, value);
    return this;
  }
  /** Int, Datetime Filter */
  notBetween (value) {
    const operator = "not_between";
    this._createFilter(operator, value);
    return this;
  }

  _toPascalCase (value) {
    return value[0].toUpperCase() + value.substring(1);
  }

  _toCamelCase (value) {
    return value[0].toLowerCase() + value.substring(1);
  }

  _createQuery (root, properties) {
    const rootModifier = this._generateModifiers("root");

    let result = "";

    result += `{
${root}${rootModifier} {\n`;

    /** Create a nested object to represent the branches and their properties */
    let branches = {};
    for (let property of properties) {
      let parts = property.split(".");
      let currentBranch = branches;

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

    result = this._generateOutput(branches, 1, this.filters, result);

    result += "  }\n}";

    return result;
  }

  _foldFilters (filters, nextProperty, type, depth = 0) {
    let filterCount = 0
    let filterString = ''
    const filterLayer = filters[nextProperty]

    /** nothing next or no filters */
    if (!filterLayer) return filterString

    /** we are down to the last part */
    if (typeof filterLayer === "string") {
      return filterLayer
    }

    /** check if we have prebuild filters for or. Checking or could work, but if we need more complex filters that would not suffice */
    if (Array.isArray(filterLayer)) {
      filterCount = filterLayer.length
      if (filterCount > 1) {
        filterString += filterLayer.join(" }, { ")
      }
      else {
        filterString = filterLayer[0]
      }
    }
    else {
      /** check if we have branches */
      const nextFilterLayerKeys = Object.keys(filterLayer)

      filterCount = nextFilterLayerKeys.length
      const branchFilters = []

      for (let filterBranch = 0; filterBranch < filterCount; filterBranch++) {
        const nextLayerKey = nextFilterLayerKeys[filterBranch]
        const nestedFilterString = this._foldFilters(filterLayer, nextLayerKey, type, depth + 1)

        branchFilters.push(`${nextLayerKey}: { ${nestedFilterString} }`)
      }

      if (branchFilters) {
        let joinSymbol = ", "
        if (depth === 0) {
          joinSymbol = filterCount > 1 ? " }, { " : ", "
        }
        filterString += branchFilters.join(joinSymbol)
      }
    }

    /** depth 0, the start of the recursion, so this is where we actually return the constructed string */
    if (depth === 0) {
      return `${type}: [{ ${filterString} }]`
    }
    else {
      return filterString
    }
  }

  _createFilterString (property, filters) {
    if (!filters) return ''

    let andFilters = this._foldFilters(filters._and, property, "_and")
    let matchAllFilters = this._foldFilters(filters._matchAll, property, "_and")
    let orFilters = this._foldFilters(filters._or, property, "_or")

    let filterString = andFilters

    if (matchAllFilters.length > 0) {
      if (filterString.length) {
        filterString = `${filterString.substring(0, filterString.length - 2)}, ${matchAllFilters} }`
      }
      else {
        filterString = matchAllFilters
      }
    }

    if (filterString.length > 0 && orFilters.length > 0) {
      filterString += `, ${orFilters}`
    }
    else if (orFilters.length > 0) {
      filterString = orFilters
    }

    return filterString
  }

  /** Generate the bit inside parentheses */
  _generateModifiers (property) {
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

    const filterString = this._createFilterString(property, this.filters[property])

    if (filterString.length) {
      modifierParts.push(`filter: { ${filterString} }`)
    }

    const filledModifiers = modifierParts.filter((f) => f.length > 0);

    return filledModifiers.length ? `(${filledModifiers.join(", ")})` : "";
  }

  _createFilterFromPath (path, operator, value) {

    /** the last part is the actual attribute. */
    const pathParts = path.split('.')

    const filter = `${operator}: ${value}`
    const queryType = !this.type ? "_and" : this.type

    const applyQueryTo = this.branch
    if (!this.filters[this.branch][queryType][applyQueryTo]) {
      /** or needs to be individual statements, the and needs to be folded into one
       * _or [{collections: { name ... }}, {collections: {acronym ...}}] 
       * Vs
       * _and: {collections: {name: {...}, acronym: {....}}}
       */
      this.filters[this.branch][queryType][applyQueryTo] = {}
    }

    let filterRef = this.filters[this.branch][queryType][applyQueryTo]

    /** split the parts, so we can combine them later */
    //if (queryType === "_and") {
    const pathDepth = pathParts.length

    for (let depth = 0; depth < pathDepth; depth++) {
      const filterPath = pathParts[depth];

      if (!filterRef[filterPath]) {
        filterRef[filterPath] = depth === pathDepth - 1 ? filter : {}
        filterRef = filterRef[filterPath]
      }
      else {
        filterRef = filterRef[filterPath]
      }
    }
    // }
    // /** make the query directly */
    // else {
    //   const reversePath = pathParts.reverse()
    //   let filterStringPlaceholder = ''
    //   for (const trail of reversePath) {
    //     if (filterStringPlaceholder === '') {
    //       filterStringPlaceholder = `${trail}: { ${filter} }`
    //     }
    //     else {
    //       filterStringPlaceholder = `${trail}: { ${filterStringPlaceholder} }`
    //     }

    //   }
    //   /** if we already have this exact filter, just return. */
    //   if (filterRef.includes(filterStringPlaceholder)) return

    //   /** add it to the filter stack */
    //   filterRef.push(filterStringPlaceholder)
    // }
  }

  /** Private function to create the correct filter syntax. */
  _createFilter (operator, value) {

    let graphQLValue = ''

    if (Array.isArray(value)) {
      graphQLValue = `["${value.join('", "')}"]`
    }
    else {
      graphQLValue = typeof value === "boolean" ? `${value}` : `"${value}"`
    }

    if (!this.filters[this.branch]) {
      this.filters[this.branch] = {
        _and: {},
        _or: {},
        _matchAll: {}
      }
    }

    this._createFilterFromPath(this.column, operator, graphQLValue)
    this.column = "";

    return this;
  }

  /** Recursively generate the output string for the branches and their properties */
  _generateOutput (branches, indentationLevel, filters, result) {
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
        const brancheModifiers = this._generateModifiers(branchName);
        /** Only add branches, not properties */
        result += `${indentation}${branchName}${brancheModifiers || ""} {\n`;

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

  _createPathFromObject (path, properties, requestedColumns = []) {
    for (const property of properties) {
      if (typeof property === "object") {
        const refProperty = Object.keys(property)[0];
        const nextPath = path ? `${path}.${refProperty} ` : refProperty;
        this._createPathFromObject(
          nextPath,
          property[refProperty],
          requestedColumns
        );
      } else {
        if (!path || path.length === 0) {
          requestedColumns.push(property);
        } else {
          requestedColumns.push(`${path}.${property} `);
        }
      }
    }
    return requestedColumns;
  }
}

export default QueryEMX2;
