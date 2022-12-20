import { defineStore } from 'pinia'
import { ref } from 'vue';
import QueryEMX2 from './queryEMX2'
import { useSettingsStore } from './settingsStore'

export const useBiobanksStore = defineStore('biobanksStore', () => {
    const settingsStore = useSettingsStore();
    const biobankColumns = settingsStore.config.biobankColumns
    const graphqlEndpoint = settingsStore.config.graphqlEndpoint
    const graphqlColumns = biobankColumns.map(biobankColumn => biobankColumn.column)

    let biobanks = ref([])

    async function getBiobanks () {
        if (biobanks.value.length > 0) return biobanks.value
        else {
            const biobankResult = await new QueryEMX2(graphqlEndpoint)
                .table('Biobanks')
                .select(graphqlColumns)
                .execute()

                console.log(biobankResult)
            biobanks.value = biobankResult.Biobanks
            return biobanks.value
        }
    }

    return { getBiobanks }
})
