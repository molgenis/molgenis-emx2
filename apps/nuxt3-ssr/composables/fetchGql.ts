export const fetchGql = async (url: string, query: string, variables?: object) => {
    const body: { query: string, variables?: object } = { query }
    if (variables) {
        body.variables = variables
    }

    return await $fetch(`http://localhost:3000/${url}`, {
        method: "POST",
        body
    });
}