import { DocumentNode } from "graphql";

export const fetchGql = (query: string | DocumentNode, variables?: object) => {

    let queryValue
    if (typeof query !== 'string') {
        if (query.loc?.source.body === undefined) {
            throw 'unable to load query: ' + query.toString()
        }
        queryValue = query.loc?.source.body
    } else {
        queryValue = query
    }

    let body: { query: string, variables?: object } = {
        query: queryValue
    }

    if (variables) {
        body.variables = variables
    }

    const route = useRoute();
    const config = useRuntimeConfig();
    return $fetch(`/${route.params.schema}/catalogue/graphql`, {
        method: "POST",
        baseURL: config.public.apiBase,
        body
    });
}