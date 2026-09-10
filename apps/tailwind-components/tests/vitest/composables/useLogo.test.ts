import { afterEach, describe, expect, it, vi } from "vitest";
vi.mock("../../../app/composables/useSettings", () => ({
  useSettings: vi.fn(),
}));

vi.mock("../../../app/composables/useSchemaSettings", () => ({
  useSchemaSettings: vi.fn(),
}));

import { useSettings } from "../../../app/composables/useSettings";
import { useSchemaSettings } from "../../../app/composables/useSchemaSettings";
import { useLogo } from "../../../app/composables/useLogo";

function settingsRef(value: unknown) {
  return { value };
}

describe("useLogo", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("returns system logo when no schema is provided", async () => {
    vi.mocked(useSettings).mockResolvedValue(
      settingsRef({ logoURL: "system-logo.svg" }) as never
    );
    vi.mocked(useSchemaSettings).mockResolvedValue(
      settingsRef({ logoURL: "schema-logo.svg" }) as never
    );

    const logoUrl = await useLogo();

    expect(logoUrl).toBe("system-logo.svg");
    expect(useSchemaSettings).not.toHaveBeenCalled();
  });

  it("returns schema logo when schema setting exists", async () => {
    vi.mocked(useSettings).mockResolvedValue(
      settingsRef({ logoURL: "system-logo.svg" }) as never
    );
    vi.mocked(useSchemaSettings).mockResolvedValue(
      settingsRef({ logoURL: "schema-logo.svg" }) as never
    );

    const logoUrl = await useLogo("test-schema");

    expect(logoUrl).toBe("schema-logo.svg");
    expect(useSchemaSettings).toHaveBeenCalledTimes(1);
  });

  it("falls back to system logo when schema logo is not a string", async () => {
    vi.mocked(useSettings).mockResolvedValue(
      settingsRef({ logoURL: "system-logo.svg" }) as never
    );
    vi.mocked(useSchemaSettings).mockResolvedValue(
      settingsRef({ logoURL: 123 }) as never
    );

    const logoUrl = await useLogo("test-schema");

    expect(logoUrl).toBe("system-logo.svg");
  });

  it("returns undefined when no valid logo setting exists", async () => {
    vi.mocked(useSettings).mockResolvedValue(settingsRef({}) as never);
    vi.mocked(useSchemaSettings).mockResolvedValue(settingsRef({}) as never);

    const logoUrl = await useLogo("test-schema");

    expect(logoUrl).toBeUndefined();
  });
});
