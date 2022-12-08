/**
 * Created by Jelmer Veen
 */

import request from "graphql-request"

class QueryEMX2 {
    tableName = ''
    filters = ''
    column = ''
    parentColumn = ''
    _tableInformation = {}
    selection = []

    /**
     * @param {string} graphqlUrl the endpoint to query
     */
    constructor(graphqlUrl, tableName) {
        this.graphqlUrl = graphqlUrl
        this.tableName = tableName
    }

    Select (columns) {
        this.selection = Array.isArray(columns) ? columns : [columns]
        return this
    }

    /**
     * Builds a query for use in EMX2 GraphQL api
     * @param {string} tableName 
     * @param {string[]} columns 
     * @param {object[]} filters 
     * @returns GraphQL query string
     */
    Execute () {
        const tableFilters = this.filters ? `(${this.filters})` : ''

        /** Fail fast */
        if (!this.tableName) throw Error('You need to provide ', this.tableName ? 'a table name' : 'columns')

        return `{
            ${this.tableName}${tableFilters} {
               ${this.selection.join()}
              }
            }`
    }

    /**
     * Gets the table information for the current schema
     */
    async GetSchemaTablesInformation () {

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
    Filter (column, nestedColumn) {
        this.column = nestedColumn ? nestedColumn : column
        this.parentColumn = nestedColumn ? column : ''
        return this
    }

    And () {
        this.type = '_and'
        return this
    }

    Or () {
        this.type = '_or'
        return this
    }

    /** Text, String, Url, Int, Bool, Datetime Filter */
    Equals (value) {
        const operator = 'equals'

        this._createFilter(operator, value)
        return this
    }
    /** Text, String, Url, Int, Bool, Datetime Filter */
    NotEquals (value) {
        const operator = 'not_equals'

        this._createFilter(operator, value)
        return this
    }

    /** Text, String, Url, Filter */
    Like (value) {
        const operator = 'like'

        return this._createFilter(operator, value)

    }
    /** Text, String, Url, Filter */
    NotLike (value) {
        const operator = 'not_like'

        this._createFilter(operator, value)
        return this
    }
    /** Text, String, Url, Filter */
    TriagramSearch (value) {
        const operator = 'triagram_search'

        this._createFilter(operator, value)
        return this
    }
    /** Text, String, Url, Filter */
    TextSearch (value) {
        const operator = 'text_search'

        this._createFilter(operator, value)
        return this
    }

    /** Int, Datetime Filter */
    Between (value) {
        const operator = 'between'

        this._createFilter(operator, value)
        return this
    }
    /** Int, Datetime Filter */
    NotBetween (value) {
        const operator = 'not_between'
        this._createFilter(operator, value)
        return this
    }

    /** Private function to create the correct filter syntax. */
    _createFilter (operator, value) {
        let columnFilter = `{ ${this.column}: { ${operator}: "${value}"} }`

        if (this.parentColumn.length > 0) {
            columnFilter = `{${this.parentColumn}: ${columnFilter}}`
        }
        this.filters += this.filters.length ? `${this.type}: ${columnFilter}}` : columnFilter
        this.column = ''
        this.parentColumn = ''
        this.type = '_and'

        return this
    }
}

export default QueryEMX2