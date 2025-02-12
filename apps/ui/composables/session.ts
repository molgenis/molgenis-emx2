export const useSession = () => {
  return useAsyncData("session", async () => {
    const { data, error } = await $fetch("/api/graphql", {
      method: "POST",
      body: JSON.stringify({
        query: `{_session { email, roles, token }}`,
      }),
    });

    if (error) {
      console.error("Error fetching session", error);
      return null;
    }

    return data._session;
  });
};
