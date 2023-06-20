import { defineStore } from "pinia";
import { computed, ref } from "vue";
import QueryEMX2 from "../functions/queryEMX2";
import { useSettingsStore } from "./settingsStore";

export const useCollectionStore = defineStore("collectionStore", () => {
    const settingsStore = useSettingsStore();
    const collectionColumns = settingsStore.config.collectionColumns;
    const graphqlEndpoint = settingsStore.config.graphqlEndpoint;
    const collectionGraphql = collectionColumns.map(
        (collectionColumn) => collectionColumn.column
    );

    let waitingForResponse = ref(false);

    const baseQuery = new QueryEMX2(graphqlEndpoint)
        .table("Collections")
        .select([
            "id",
            "name",
            ...collectionGraphql,
        ])
        .orderBy("collections", "id", "asc");


    async function getCollectionReport (id) {

        const collectionReportQuery = new QueryEMX2(graphqlEndpoint)
            .table("Collections")
            .select([
                "biobank.id",
                "biobank.name",
                "biobank.withdrawn",
                "contact.first_name",
                "contact.last_name",
                "contact.email",
                "contact.role",
                "contact.country.label",
                "head.first_name",
                "head.last_name",
                "head.role",
                "country.label",
                "network.name",
                "network.id",
                "url",
                "withdrawn",
                ...collectionGraphql
            ])
            .orderBy("Collections", "id", "asc")
            .where("id")
            .like(id)
        return await collectionReportQuery.execute();
    }

    const waiting = computed(() => {
        return waitingForResponse.value;
    });

    return {
        waiting,
        baseQuery,
        getCollectionReport,
    };
});
