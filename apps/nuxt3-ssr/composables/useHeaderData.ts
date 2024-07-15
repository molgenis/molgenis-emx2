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

  const apiPath = `/${route.params.schema}/graphql`;

  const { data, error } = await useAsyncData<any, IMgError>(
    `catalogue-${route.params.catalogue}`,
    async () => {
      let variables:
        | {
            networksFilter: { id: { equals: string | string[] } };
            variablesFilter?: {
              resource: { id: { equals: string | string[] } };
            };
          }
        | undefined = undefined;
      if (route.params.catalogue && route.params.catalogue !== "all") {
        const modelsResp = await $fetch<{
          data: { Networks: { models: { id: string }[] }[] };
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

        const models = modelsResp.data.Networks[0].models;
        const modelIds = models ? models.map((m) => m.id) : [];
        variables = {
          networksFilter: { id: { equals: route.params.catalogue } },
        };
        if (modelIds) {
          variables.variablesFilter = {
            resource: { id: { equals: modelIds } },
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

  const catalogue = data.value.data.Networks[0];
  const variableCount = data.value.data.Variables_agg.count || 0;

  return { catalogue, variableCount };
}
