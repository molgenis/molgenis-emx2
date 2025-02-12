export async function useBannerData() {
  const route = useRoute();
  const CATALOGUE_BANNER_HTML = "CATALOGUE_BANNER_HTML";

  return useFetch(`/${route.params.schema}/graphql`, {
    method: "POST",
    body: {
      query: `{_settings (keys: ["${CATALOGUE_BANNER_HTML}"]){ key, value }}`,
    },
    transform(response) {
      return response.data._settings.filter(
        (setting: { key: string; value: string }) =>
          setting.key === CATALOGUE_BANNER_HTML
      )[0].value;
    },
    server: false,
    lazy: true,
  });
}
