import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import {
  BUILT_IN_THEMES,
  applyThemeOverrides,
  createThemeId,
  diffThemeVariables,
  isSafeThemeValue,
  parseCustomThemes,
  parseRootVariables,
  resolveTheme,
  sanitizeVariables,
  themeVariableGroup,
  toHexColor,
  validateThemeName,
  type ICustomTheme,
} from "../../../app/utils/themes";

const cssDir = resolve(__dirname, "../../../app/assets/css");
const readCss = (path: string) => readFileSync(resolve(cssDir, path), "utf-8");

const brand: ICustomTheme = {
  id: "brand",
  name: "Brand",
  basedOn: "molgenis",
  variables: { "--color-blue-500": "#123456" },
};

describe("resolveTheme", () => {
  it("returns null when nothing is configured", () => {
    expect(resolveTheme({})).toBeNull();
  });

  it("prefers the schema theme over the application theme", () => {
    expect(
      resolveTheme({
        schema: { active: "dark", customThemes: [] },
        app: { active: "umcg", customThemes: [] },
      })?.base
    ).toBe("dark");
  });

  it("falls back to the application theme", () => {
    expect(
      resolveTheme({
        schema: { customThemes: [] },
        app: { active: "umcg", customThemes: [] },
      })?.base
    ).toBe("umcg");
  });

  it("uses the base and overrides of a custom theme", () => {
    expect(
      resolveTheme({ app: { active: "brand", customThemes: [brand] } })
    ).toEqual({
      id: "brand",
      base: "molgenis",
      overrides: { "--color-blue-500": "#123456" },
    });
  });

  it("lets a schema use an application custom theme", () => {
    expect(
      resolveTheme({
        schema: { active: "brand", customThemes: [] },
        app: { customThemes: [brand] },
      })?.base
    ).toBe("molgenis");
  });

  it("prefers a schema custom theme with the same id", () => {
    expect(
      resolveTheme({
        schema: {
          active: "brand",
          customThemes: [{ ...brand, basedOn: "dark" }],
        },
        app: { customThemes: [brand] },
      })?.base
    ).toBe("dark");
  });

  it("does not let the application use a schema custom theme", () => {
    expect(
      resolveTheme({
        schema: { customThemes: [brand] },
        app: { active: "brand", customThemes: [] },
      })
    ).toBeNull();
  });

  it("falls through a theme that no longer exists", () => {
    expect(
      resolveTheme({
        schema: { active: "deleted", customThemes: [] },
        app: { active: "aumc", customThemes: [] },
      })?.base
    ).toBe("aumc");
  });

  it("lets the query parameter win, without overrides", () => {
    expect(
      resolveTheme({
        query: "light",
        app: { active: "brand", customThemes: [brand] },
      })
    ).toEqual({ id: "light", base: "light", overrides: {} });
  });
});

describe("parseCustomThemes", () => {
  it.each([undefined, "", "not json", "{}", "null"])(
    "returns an empty list for %s",
    (value) => {
      expect(parseCustomThemes(value)).toEqual([]);
    }
  );

  it("drops themes that shadow a built-in or have an unknown base", () => {
    const stored = [
      brand,
      { ...brand, id: "dark" },
      { ...brand, id: "other", basedOn: "unknown" },
      { id: "incomplete" },
    ];
    expect(parseCustomThemes(JSON.stringify(stored))).toEqual([brand]);
  });

  it("removes unsafe values from stored themes", () => {
    const stored = [
      {
        ...brand,
        variables: {
          "--color-blue-500": "#123456",
          "--background-image-x": "url(https://example.org/x.png)",
        },
      },
    ];
    expect(parseCustomThemes(JSON.stringify(stored))[0]?.variables).toEqual({
      "--color-blue-500": "#123456",
    });
  });
});

describe("sanitizeVariables", () => {
  it.each([
    "url(https://example.org)",
    "URL(x)",
    "image-set('x.png' 1x)",
    "red; background: blue",
    "}</style><script>",
    "@import 'x'",
    "a\\62",
    "/* comment",
    "expression(alert(1))",
    "   ",
  ])("rejects %s", (value) => {
    expect(isSafeThemeValue(value)).toBe(false);
  });

  it("rejects invalid variable names and non-string values", () => {
    expect(
      sanitizeVariables({
        color: "red",
        "--bad name": "red",
        "--number": 1,
        "--good": " red ",
      })
    ).toEqual({ "--good": "red" });
  });
});

describe("createThemeId", () => {
  it("slugifies the name", () => {
    expect(createThemeId(" My Brand! ", [])).toBe("my-brand");
  });

  it("avoids built-in and taken ids", () => {
    expect(createThemeId("Dark", [])).toBe("dark-2");
    expect(createThemeId("Brand", ["brand", "brand-2"])).toBe("brand-3");
  });

  it("has a fallback for names without usable characters", () => {
    expect(createThemeId("!!!", [])).toBe("theme");
  });
});

describe("validateThemeName", () => {
  it("accepts a new name", () => {
    expect(validateThemeName("Brand", ["Other"])).toBeUndefined();
  });

  it.each(["", "   ", "molgenis", "MOLGENIS", "uncan connect", "other"])(
    "rejects %s",
    (name) => {
      expect(validateThemeName(name, ["Other"])).toBeDefined();
    }
  );

  it("rejects a name that is too long", () => {
    expect(validateThemeName("x".repeat(61), [])).toBeDefined();
  });
});

describe("diffThemeVariables", () => {
  it("keeps only values that differ from the base", () => {
    expect(
      diffThemeVariables(
        { "--a": "red", "--b": " blue ", "--c": "", "--d": "green" },
        { "--a": "red", "--b": "navy", "--c": "x" }
      )
    ).toEqual({ "--b": "blue", "--d": "green" });
  });
});

describe("parseRootVariables", () => {
  it("handles multi-line values, comments and quoted semicolons", () => {
    const css = `/* :root { --ignored: 1; } */
      .other { --other: 1; }
      :root[data-theme="x"] {
        --gradient: linear-gradient(
          0deg,
          #fff 0%
        );
        --quoted: "a;b";
        color: red;
      }`;
    expect(parseRootVariables(css)).toEqual({
      "--gradient": "linear-gradient( 0deg, #fff 0% )",
      "--quoted": '"a;b"',
    });
  });

  it("does not read selectors nested below :root", () => {
    expect(parseRootVariables(`:root .x { --a: 1; }`)).toEqual({});
  });

  it("finds the base variables in main.css", () => {
    const variables = parseRootVariables(readCss("main.css"));
    expect(variables["--color-blue-500"]).toBe("#0173e4");
    expect(Object.keys(variables).length).toBeGreaterThan(200);
    expect(variables["--text-color-pagination"]).toBeDefined();
  });

  it.each(BUILT_IN_THEMES.filter((theme) => theme.id !== "light"))(
    "reads $id with only values that pass the safety check",
    ({ id }) => {
      const variables = parseRootVariables(readCss(`theme/${id}.css`));
      expect(Object.keys(variables).length).toBeGreaterThan(0);
      expect(
        Object.entries(variables).filter(
          ([, value]) => !isSafeThemeValue(value)
        )
      ).toEqual([]);
    }
  );
});

describe("toHexColor", () => {
  const variables = {
    "--a": "var(--b)",
    "--b": "#abc",
    "--loop": "var(--loop)",
  };

  it.each([
    ["#123456", "#123456"],
    ["#ABCDEF80", "#abcdef"],
    ["#abc", "#aabbcc"],
    ["rgb(255, 0, 16)", "#ff0010"],
    ["rgba(1 2 3 / 50%)", "#010203"],
    ["var(--a)", "#aabbcc"],
  ])("converts %s", (value, expected) => {
    expect(toHexColor(value, variables)).toBe(expected);
  });

  it.each(["none", "linear-gradient(#fff, #000)", "var(--loop)", "var(--x)"])(
    "returns undefined for %s",
    (value) => {
      expect(toHexColor(value, variables)).toBeUndefined();
    }
  );
});

describe("themeVariableGroup", () => {
  it.each([
    ["--color-blue-500", "Palette"],
    ["--text-color-title", "Text"],
    ["--background-color-button", "Background"],
    ["--background-image-x", "Background"],
    ["--border-color-input", "Border"],
    ["--border-radius-base", "Shape"],
    ["--box-shadow-primary", "Shape"],
    ["--chart-line", "Other"],
  ])("groups %s under %s", (name, group) => {
    expect(themeVariableGroup(name)).toBe(group);
  });
});

describe("applyThemeOverrides", () => {
  it("sets values and removes those no longer present", () => {
    const element = document.createElement("div");
    const applied = applyThemeOverrides(element, { "--a": "red" }, []);
    expect(element.style.getPropertyValue("--a")).toBe("red");

    applyThemeOverrides(element, { "--b": "blue" }, applied);
    expect(element.style.getPropertyValue("--a")).toBe("");
    expect(element.style.getPropertyValue("--b")).toBe("blue");
  });

  it("skips unsafe values", () => {
    const element = document.createElement("div");
    expect(applyThemeOverrides(element, { "--a": "url(x)" }, [])).toEqual([]);
    expect(element.style.getPropertyValue("--a")).toBe("");
  });
});
