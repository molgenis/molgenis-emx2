export interface IBuiltInTheme {
  id: string;
  label: string;
}

export interface ICustomTheme {
  id: string;
  name: string;
  basedOn: string;
  variables: Record<string, string>;
}

export interface IThemeLevel {
  active?: string;
  customThemes: ICustomTheme[];
}

export interface IConfiguredTheme {
  id: string;
  base: string;
  overrides: Record<string, string>;
}

export const BUILT_IN_THEMES: readonly IBuiltInTheme[] = [
  { id: "light", label: "Light" },
  { id: "dark", label: "Dark" },
  { id: "molgenis", label: "MOLGENIS" },
  { id: "umcg", label: "UMCG" },
  { id: "aumc", label: "AUMC" },
  { id: "hdsu", label: "HDSU" },
  { id: "uncan-connect", label: "UNCAN Connect" },
];

export const THEME_NAME_MAX_LENGTH = 60;

// present in the theme files, but not styling values a user should edit
export const HIDDEN_THEME_VARIABLES: readonly string[] = [
  "--logo",
  "--text-content-required",
];

const VARIABLE_NAME = /^--[\w-]+$/;
const UNSAFE_VALUE = /url\(|image-set\(|expression\(|[@;{}<\\]|\/\*/i;

export function isBuiltInTheme(id: string): boolean {
  return BUILT_IN_THEMES.some((theme) => theme.id === id);
}

export function isSafeThemeValue(value: string): boolean {
  return value.trim() !== "" && !UNSAFE_VALUE.test(value);
}

export function sanitizeVariables(
  variables: Record<string, unknown>
): Record<string, string> {
  const sanitized: Record<string, string> = {};
  for (const [name, value] of Object.entries(variables)) {
    if (
      VARIABLE_NAME.test(name) &&
      typeof value === "string" &&
      isSafeThemeValue(value)
    ) {
      sanitized[name] = value.trim();
    }
  }
  return sanitized;
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function toCustomTheme(value: unknown): ICustomTheme | undefined {
  if (
    isPlainObject(value) &&
    typeof value.id === "string" &&
    value.id !== "" &&
    !isBuiltInTheme(value.id) &&
    typeof value.name === "string" &&
    typeof value.basedOn === "string" &&
    isBuiltInTheme(value.basedOn) &&
    isPlainObject(value.variables)
  ) {
    return {
      id: value.id,
      name: value.name,
      basedOn: value.basedOn,
      variables: sanitizeVariables(value.variables),
    };
  }
  return undefined;
}

export function parseCustomThemes(json: string | undefined): ICustomTheme[] {
  if (!json) {
    return [];
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(json);
  } catch {
    return [];
  }
  if (!Array.isArray(parsed)) {
    return [];
  }
  return parsed.flatMap((item) => toCustomTheme(item) ?? []);
}

function lookupTheme(
  id: string | undefined,
  customThemes: ICustomTheme[]
): IConfiguredTheme | undefined {
  if (!id) {
    return undefined;
  }
  if (isBuiltInTheme(id)) {
    return { id, base: id, overrides: {} };
  }
  const custom = customThemes.find((theme) => theme.id === id);
  return custom
    ? { id, base: custom.basedOn, overrides: custom.variables }
    : undefined;
}

export function resolveTheme({
  query,
  schema,
  app,
}: {
  query?: unknown;
  schema?: IThemeLevel;
  app?: IThemeLevel;
}): IConfiguredTheme | null {
  if (typeof query === "string" && query !== "") {
    return { id: query, base: query, overrides: {} };
  }
  const appThemes = app?.customThemes ?? [];
  return (
    lookupTheme(schema?.active, [
      ...(schema?.customThemes ?? []),
      ...appThemes,
    ]) ??
    lookupTheme(app?.active, appThemes) ??
    null
  );
}

export function createThemeId(name: string, takenIds: string[]): string {
  const slug =
    name
      .trim()
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/^-+|-+$/g, "") || "theme";
  const isTaken = (id: string) => isBuiltInTheme(id) || takenIds.includes(id);
  let candidate = slug;
  for (let suffix = 2; isTaken(candidate); suffix++) {
    candidate = `${slug}-${suffix}`;
  }
  return candidate;
}

export function validateThemeName(
  name: string,
  otherNames: string[]
): string | undefined {
  const normalized = name.trim().toLowerCase();
  if (normalized === "") {
    return "Enter a name for this theme";
  }
  if (normalized.length > THEME_NAME_MAX_LENGTH) {
    return `Use at most ${THEME_NAME_MAX_LENGTH} characters`;
  }
  const isBuiltInName = BUILT_IN_THEMES.some(
    (theme) =>
      theme.id.toLowerCase() === normalized ||
      theme.label.toLowerCase() === normalized
  );
  if (isBuiltInName) {
    return "This name is reserved for a built-in theme";
  }
  if (otherNames.some((other) => other.trim().toLowerCase() === normalized)) {
    return "A theme with this name already exists";
  }
  return undefined;
}

export function diffThemeVariables(
  variables: Record<string, string>,
  base: Record<string, string>
): Record<string, string> {
  return Object.fromEntries(
    Object.entries(variables)
      .map(([name, value]): [string, string] => [name, value.trim()])
      .filter(([name, value]) => value !== "" && value !== base[name])
  );
}

export function themeVariableGroup(name: string): string {
  if (name.startsWith("--text-color")) return "Text";
  if (name.startsWith("--background")) return "Background";
  if (name.startsWith("--border-color")) return "Border";
  if (name.startsWith("--color")) return "Palette";
  if (/^--(border-(width|radius)|radius|height|box-shadow)/.test(name)) {
    return "Shape";
  }
  return "Other";
}

function splitDeclarations(block: string): string[] {
  const declarations: string[] = [];
  let depth = 0;
  let quote = "";
  let current = "";
  for (const char of block) {
    if (quote) {
      quote = char === quote ? "" : quote;
    } else if (char === '"' || char === "'") {
      quote = char;
    } else if (char === "(") {
      depth++;
    } else if (char === ")") {
      depth--;
    } else if (char === ";" && depth === 0) {
      declarations.push(current);
      current = "";
      continue;
    }
    current += char;
  }
  declarations.push(current);
  return declarations;
}

export function parseRootVariables(css: string): Record<string, string> {
  const source = css.replace(/\/\*[\s\S]*?\*\//g, "");
  const variables: Record<string, string> = {};
  for (const match of source.matchAll(/:root(?:\[[^\]]*\])?\s*\{/g)) {
    const start = match.index + match[0].length;
    let end = start;
    for (let depth = 1; end < source.length; end++) {
      if (source[end] === "{") depth++;
      if (source[end] === "}") depth--;
      if (depth === 0) break;
    }
    for (const declaration of splitDeclarations(source.slice(start, end))) {
      const separator = declaration.indexOf(":");
      const name = declaration.slice(0, separator).trim();
      if (separator > 0 && VARIABLE_NAME.test(name)) {
        variables[name] = declaration
          .slice(separator + 1)
          .replace(/\s+/g, " ")
          .trim();
      }
    }
  }
  return variables;
}

function channelToHex(channel: string): string {
  return Math.min(255, Number(channel)).toString(16).padStart(2, "0");
}

export function toHexColor(
  value: string | undefined,
  variables: Record<string, string>
): string | undefined {
  let current = value?.trim();
  for (let hops = 0; current && hops < 10; hops++) {
    const reference = current.match(/^var\((--[\w-]+)\)$/);
    if (!reference) break;
    current = variables[reference[1] as string]?.trim();
  }
  if (!current) {
    return undefined;
  }
  const hex = current.match(/^#([0-9a-f]{3,8})$/i)?.[1]?.toLowerCase();
  if (hex?.length === 3 || hex?.length === 4) {
    return `#${[...hex.slice(0, 3)].map((digit) => digit + digit).join("")}`;
  }
  if (hex?.length === 6 || hex?.length === 8) {
    return `#${hex.slice(0, 6)}`;
  }
  const rgb = current.match(/^rgba?\(\s*(\d+)[\s,]+(\d+)[\s,]+(\d+)/i);
  if (rgb) {
    return `#${rgb.slice(1, 4).map(channelToHex).join("")}`;
  }
  return undefined;
}

export function applyThemeOverrides(
  element: HTMLElement,
  overrides: Record<string, string>,
  previouslyApplied: string[]
): string[] {
  const safeOverrides = sanitizeVariables(overrides);
  previouslyApplied
    .filter((name) => !(name in safeOverrides))
    .forEach((name) => element.style.removeProperty(name));
  Object.entries(safeOverrides).forEach(([name, value]) =>
    element.style.setProperty(name, value)
  );
  return Object.keys(safeOverrides);
}
