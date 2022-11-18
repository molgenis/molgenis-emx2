// const config = useRuntimeConfig();
export const fetchGql = (url: string, query: string) => {
    return $fetch(`/UMCG/catalogue/graphql`, {
        method: "POST",
        baseURL: 'http://localhost:3000/',
        body: {
            query,
        },
    });
}