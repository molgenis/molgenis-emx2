export const fetchGql = (query: string) => {
    const route = useRoute();
    const config = useRuntimeConfig();
    return $fetch(`/${route.params.schema}/catalogue/graphql`, {
        method: "POST",
        baseURL: config.public.apiBase,
        body: {
            query,
        },
    });
}