import { ref } from 'vue'
import { defineStore } from 'pinia'
import QueryEMX2 from './queryEMX2'

export const useGraphqlStore = defineStore('graphqlStore', () => {
  const graphqlUrl = ref('graphql')

  /**
   * @param {string} tableName 
   */
  async function getColumnsForTable (tableName) {
    return await new QueryEMX2().getColumnsForTable(tableName);
  }

  async function queryTable () {

    const results = await new QueryEMX2('graphql')
      .table('Biobanks')
      .select(['id', 'name'])
      .where('Collections', 'name').like('cardiovascular')
      .or('name').like('UMC')
      .execute()

    return results;
  }

  return { getColumnsForTable, queryTable }
})
