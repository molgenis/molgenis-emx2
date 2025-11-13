import { useRuntimeConfig, useRoute, useAsyncData, useState } from "#app";

export async function useTheme() {
  const config = useRuntimeConfig();
  const route = useRoute();

  const theme = useState("theme", () => "molgenis");

  const { data } = await useAsyncData(
    "theme",
    () =>
      $fetch(`/${config.public.schema}/graphql`, {
        method: "POST",
        body: {
          query: `{_settings (keys: ["CATALOGUE_THEME"]){ key, value }}`,
        },
      }),
    {
      transform: (data) => {
        const themeValue = data.data._settings.find(
          (s: { key: string }) => s.key === "CATALOGUE_THEME"
        );
        return themeValue?.value ?? config.public.emx2Theme ?? "molgenis";
      },
    }
  );

  theme.value = data.value;

  return (route.query.theme as string) ?? theme.value;
}
