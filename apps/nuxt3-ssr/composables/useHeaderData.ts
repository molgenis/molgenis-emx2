import type { IMgError } from "../interfaces/types";

const method = "POST";

const modelQuery = `
      query Networks($networksFilter:NetworksFilter) {
        Networks(filter:$networksFilter){ models { id } }
      }`;

const headerQuery = `
      query HeaderQuery($networksFilter:NetworksFilter, $variablesFilter:VariablesFilter) {
        Networks(filter:$networksFilter) {
          id,
          dataSources_agg { count }
          cohorts_agg { count }
          networks_agg { count }
          logo { url }
       }
       Variables_agg(filter:$variablesFilter) {
          count
       }
    }`;

export async function useHeaderData() {
  const route = useRoute();

  // no need to do fetch if we are on the home page or on the all catalogues pages
  if (!route.params.catalogue || route.params.catalogue == "all") {
    return { catalogue: null, variableCount: 0 };
  }

  const config = useRuntimeConfig();
  const apiPath = `/${route.params.schema}/catalogue/graphql`;
  const baseURL = config.public.apiBase;
  const { data, error } = await useAsyncData<any, IMgError>(
    `catalogue-${route.params.catalogue}`,
    async () => {
      const modelsResp = await $fetch<{
        data: { Networks: { models: { id: string }[] }[] };
      }>(apiPath, {
        baseURL,
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

      const models = modelsResp.data.Networks[0].models;
      const modelIds = models ? models.map((m) => m.id) : [];
      let variables: {
        networksFilter: { id: { equals: string | string[] } };
        variablesFilter?: { resource: { id: { equals: string | string[] } } };
      } = { networksFilter: { id: { equals: route.params.catalogue } } };
      if (modelIds) {
        variables.variablesFilter = { resource: { id: { equals: modelIds } } };
      }

      return $fetch(apiPath, {
        key: `header-${route.params.catalogue}`,
        baseURL,
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

  const catalogue = data.value.data.Networks[0];
  const variableCount = data.value.data.Variables_agg.count || 0;

  return { catalogue, variableCount };
}
