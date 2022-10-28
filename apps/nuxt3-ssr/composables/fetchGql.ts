export const fetchGql = async (url: string, query: string) => {
    const config = useRuntimeConfig();
    return await $fetch(`${config.API_BASE_URL }/${url}`, {
        method: "POST",
        body: {
            query,
        },
    });
}