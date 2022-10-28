export const fetchGql = async (url: string, query: string) => {
    return await $fetch(`http://localhost:3000/${url}`, {
        method: "POST",
        body: {
            query,
        },
    });
}