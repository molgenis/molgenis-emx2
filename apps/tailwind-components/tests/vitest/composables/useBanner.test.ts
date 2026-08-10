import { beforeEach, describe, expect, it, vi } from "vitest";
import { ref } from "vue";

vi.mock("#app", async (importOriginal) => {
  const app = await importOriginal<typeof import("#app")>();
  return {
    ...app,
    useFetch: vi.fn(),
  };
});

import { useFetch } from "#app";
import { useBanner } from "../../../app/composables/useBanner";

describe("useBanner", () => {
  const dataRef = ref<string | undefined>(undefined);

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useFetch).mockReturnValue({ data: dataRef } as never);
  });

  it("calls useFetch with expected GraphQL request options", () => {
    const result = useBanner();

    expect(result).toBe(dataRef);
    expect(useFetch).toHaveBeenCalledTimes(1);

    const [url, options] = vi.mocked(useFetch).mock.calls[0] as [
      string,
      {
        method: string;
        body: { query: string };
        server: boolean;
        lazy: boolean;
        transform: (response: unknown) => unknown;
      }
    ];

    expect(url).toBe("/api/graphql");
    expect(options.method).toBe("POST");
    expect(options.body).toEqual({
      query: `{_settings (keys: ["SYSTEM_BANNER_HTML"]){ key, value }}`,
    });
    expect(options.server).toBe(false);
    expect(options.lazy).toBe(true);
    expect(options.transform).toEqual(expect.any(Function));
  });

  it("transform returns SYSTEM_BANNER_HTML when present", () => {
    useBanner();

    const [, options] = vi.mocked(useFetch).mock.calls[0] as [
      string,
      { transform: (response: any) => unknown }
    ];

    const response = {
      data: {
        _settings: [
          { key: "OTHER_SETTING", value: "x" },
          { key: "SYSTEM_BANNER_HTML", value: "<p>Banner</p>" },
        ],
      },
    };

    expect(options.transform(response)).toBe("<p>Banner</p>");
  });

  it("transform returns undefined when SYSTEM_BANNER_HTML is missing", () => {
    useBanner();

    const [, options] = vi.mocked(useFetch).mock.calls[0] as [
      string,
      { transform: (response: any) => unknown }
    ];

    const response = {
      data: {
        _settings: [{ key: "OTHER_SETTING", value: "x" }],
      },
    };

    expect(options.transform(response)).toBeUndefined();
  });
});
