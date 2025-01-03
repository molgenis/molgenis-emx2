export const useSettings = (schema: string) => {
  return useAsyncData("settings", async () => {
    console.log("init useSettings");

    const { data, error } = await $fetch(`/${schema}/settings/graphql`, {
      method: "POST",
      body: JSON.stringify({
        query: `{_settings { key, value }}`,
      }),
    });

    if (error) {
      console.error("Error fetching settings", error);
      return null;
    }
    return data._settings;
  });
};
