import { useFetch } from "#app";
import type { Resp, Settings } from "../../types/types";

export function useBanner() {
  const { data } = useFetch<string | undefined>("/api/graphql", {
    method: "POST",
    body: {
      query: `{_settings (keys: ["SYSTEM_BANNER_HTML"]){ key, value }}`,
    },
    transform(response: Resp<Settings>) {
      return response.data?._settings?.find(
        (setting) => setting.key === "SYSTEM_BANNER_HTML"
      )?.value;
    },
    server: false,
    lazy: true,
  });

  return data;
}
