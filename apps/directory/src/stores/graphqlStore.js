import { ref } from 'vue'
import { defineStore } from 'pinia'
import { request } from 'graphql-request'

export const useGraphqlStore = defineStore('graphqlStore', () => {
  let tableInformation = ref([])
  const graphqlUrl = ref('graphql')

  /**
   * Gets the current selected schema information
   * and stores it in tableInformation
   */
  async function getTableInformation () {

    const result = await request(graphqlUrl.value, `
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
    tableInformation.value = result._schema.tables
  }

  /**
   * @param {string} tableName 
   */
  async function getColumnsForTable (tableName) {
    if (tableInformation.value.length === 0) {
      await getTableInformation();
    }

    const columns = tableInformation.value.find(ti => ti.name === tableName)
    if (columns.length === 0) return []

    return columns
  }

  async function queryTable (tableName, properties) {

    // todo check properties, can be a string or object
    // also need filters

    const query = `{
    ${tableName} {
       ${properties.join()}
      }
    }`

    return await request(graphqlUrl.value, query)
  }

  return { getColumnsForTable, queryTable }
})
