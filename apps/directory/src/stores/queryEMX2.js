/**
 * Created by Jelmer Veen
 */

import request from "graphql-request"

class QueryEMX2 {
    tableName = ''
    filters = []
    column = ''
    parentColumn = ''
    _tableInformation = {}
    selection = []
    graphqlUrl = ''

    /**
     * @param {string} graphqlUrl the endpoint to query
     */
    constructor(graphqlUrl) {
        this.graphqlUrl = graphqlUrl
    }

    table (tableName) {
        this.tableName = tableName[0].toUpperCase() + tableName.substring(1)
        /** Tables always start with an uppercase */
        return this
    }

    /**
     * @param {string | string[]} columns 
     */
    select (columns) {
        const requestedColumns = Array.isArray(columns) ? columns : [columns]
        requestedColumns.forEach(name => name.toLowerCase())
        this.selection = requestedColumns
        return this
    }

    /**
     * Builds a query for use in EMX2 GraphQL api
     * @param {string} tableName 
     * @param {string[]} columns 
     * @param {object[]} filters 
     * @returns GraphQL query string
     */
    async execute () {
        const tableFilters = this.filters.length ? `(filter: ${this.filters})` : ''

        /** Fail fast */
        if (!this.tableName) throw Error('You need to provide ', this.tableName ? 'a table name' : 'columns')

        // if no selection is made, check get schema to add all columns

        const query = `{
            ${this.tableName}${tableFilters} {
               ${this.selection.join()}
              }
            }`

        return await request(this.graphqlUrl, query)
    }

    /**
     * Gets the table information for the current schema
     */
    async getSchemaTablesInformation () {

        if (this._schemaTablesInformation) return this._schemaTablesInformation

        const result = await request(this.graphqlUrl, `
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
        `)
        this._schemaTablesInformation = result._schema.tables

        return this._schemaTablesInformation
    }


    /**
     * If you want to create a nested query, for example { collections: { name: { like: 'lifelines' } } }
     * then column = 'collections', nested column = 'name'
     * @param {*} column 
     * @param {*} nestedColumn 
     * @returns 
     */
    where (column, nestedColumn) {
        /** always convert to lowercase, else api will error */
        this.column = nestedColumn ? nestedColumn.toLowerCase() : column.toLowerCase()
        this.parentColumn = nestedColumn ? column.toLowerCase() : ''
        return this
    }

    and (column, nestedColumn) {
        this.type = '_and'
        return this.filter(column, nestedColumn)
    }

    or (column, nestedColumn) {
        this.type = '_or'
        return this.where(column, nestedColumn)
    }

    /** Text, String, Url, Int, Bool, Datetime Filter */
    equals (value) {
        const operator = 'equals'

        this._createFilter(operator, value)
        return this
    }
    /** Text, String, Url, Int, Bool, Datetime Filter */
    notEquals (value) {
        const operator = 'not_equals'

        this._createFilter(operator, value)
        return this
    }

    /** Text, String, Url, Filter */
    like (value) {
        const operator = 'like'

        return this._createFilter(operator, value)

    }
    /** Text, String, Url, Filter */
    notLike (value) {
        const operator = 'not_like'

        this._createFilter(operator, value)
        return this
    }
    /** Text, String, Url, Filter */
    triagramSearch (value) {
        const operator = 'triagram_search'

        this._createFilter(operator, value)
        return this
    }
    /** Text, String, Url, Filter */
    textSearch (value) {
        const operator = 'text_search'

        this._createFilter(operator, value)
        return this
    }

    /** Int, Datetime Filter */
    between (value) {
        const operator = 'between'

        this._createFilter(operator, value)
        return this
    }
    /** Int, Datetime Filter */
    notBetween (value) {
        const operator = 'not_between'
        this._createFilter(operator, value)
        return this
    }

    /** Private function to create the correct filter syntax. */
    _createFilter (operator, value) {
        let columnFilter = `{ ${this.column}: { ${operator}: "${value}"} }`

        if (this.parentColumn.length > 0) {
            columnFilter = `{ ${this.parentColumn}: ${columnFilter} }`
        }


        /**{
            Biobanks(filter: {collections: { name: { like: "cardiovascular"}}, _and: { name: { like: "UMC"}}}) {
               id,name
              }
            }
        */

        if (this.filters.length) {
            /** need to remove the last }, add an _and / _or and stitch it together */
            this.filters = `${this.filters.substring(0, this.filters.length - 1)}, ${this.type}: ${columnFilter}}`
        }
        else {
            this.filters = columnFilter
        }
        this.column = ''
        this.parentColumn = ''
        this.type = '_and'

        return this
    }
}

export default QueryEMX2