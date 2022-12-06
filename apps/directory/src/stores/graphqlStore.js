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
            _schema {sxs
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

        console.log('result', result)
        tableInformation.value = result.tables
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

    return { getColumnsForTable }
})
