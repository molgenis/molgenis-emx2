import { parseRootVariables } from "./themes";

type RawCss = () => Promise<{ default: string }>;

// "light" has no file of its own: it is the base :root block in main.css
const THEME_SOURCES: Record<string, RawCss> = {
  dark: () => import("../assets/css/theme/dark.css?raw"),
  molgenis: () => import("../assets/css/theme/molgenis.css?raw"),
  umcg: () => import("../assets/css/theme/umcg.css?raw"),
  aumc: () => import("../assets/css/theme/aumc.css?raw"),
  hdsu: () => import("../assets/css/theme/hdsu.css?raw"),
  "uncan-connect": () => import("../assets/css/theme/uncan-connect.css?raw"),
};

export async function loadThemeVariables(
  themeId: string
): Promise<Record<string, string>> {
  const [base, theme] = await Promise.all([
    import("../assets/css/main.css?raw"),
    THEME_SOURCES[themeId]?.(),
  ]);
  return {
    ...parseRootVariables(base.default),
    ...(theme ? parseRootVariables(theme.default) : {}),
  };
}
