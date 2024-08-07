import type { IMgError } from "../interfaces/types";

const method = "POST";

const modelQuery = `
      query Collections($networksFilter:CollectionsFilter) {
        Collections(filter:$networksFilter){ datasets { name } }
      }`;

const headerQuery = `
      query HeaderQuery($networksFilter:CollectionsFilter, $variablesFilter:VariablesFilter) {
        Collections(filter:$networksFilter) {
          id,
          collections_agg { count }
          logo { url }
       }
       Variables_agg(filter:$variablesFilter) {
          count
       }
    }`;

export async function useHeaderData() {
  const route = useRoute();

  const apiPath = `/${route.params.schema}/graphql`;

  const { data, error } = await useAsyncData<any, IMgError>(
    `catalogue-${route.params.catalogue}`,
    async () => {
      let variables:
        | {
            networksFilter: { id: { equals: string | string[] } };
            variablesFilter?: {
              collection: { id: { equals: string | string[] } };
            };
          }
        | undefined = undefined;
      if (route.params.catalogue && route.params.catalogue !== "all") {
        const modelsResp = await $fetch<{
          data: { Collections: { datasets: { id: string }[] }[] };
        }>(apiPath, {
          method,
          body: {
            query: modelQuery,
            variables: {
              networksFilter: { id: { equals: route.params.catalogue } },
            },
          },
        }).catch((e) => {
          console.log("models error: ", e);
          return { e };
        });

        if ("e" in modelsResp) {
          const contextMsg = "Error on fetching page header data";
          throw new Error(contextMsg);
        }

        const models = modelsResp.data?.Collections[0]?.datasets;
        const modelIds = models ? models.map((m) => m.id) : [];
        variables = {
          networksFilter: { id: { equals: route.params.catalogue } },
        };
        if (modelIds) {
          variables.variablesFilter = {
            collection: { id: { equals: modelIds } },
          };
        }
      }

      return $fetch(apiPath, {
        key: `header-${route.params.catalogue}`,
        method,
        body: {
          query: headerQuery,
          variables,
        },
      });
    }
  );

  if (error.value) {
    const contextMsg = "Error on fetching page header data";
    logError(error.value, contextMsg);
    throw new Error(contextMsg);
  }

  const catalogue = data.value.data?.Collections
    ? data.value.data?.Collections[0]
    : null;
  const variableCount = data.value.data?.Variables_agg.count || 0;

  return { catalogue, variableCount };
}
