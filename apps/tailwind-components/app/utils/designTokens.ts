import tailwindConfig from "../../tailwind.config.js";

const themeConfig: Record<string, any> = tailwindConfig.theme ?? {};
const themeExtend: Record<string, any> = themeConfig.extend ?? {};

export interface PaletteColorRecord {
  tokenName: string;
  family: string;
  shade: string;
  cssValue: string;
}

export interface SemanticColorRecord {
  tokenName: string;
  category: "background" | "text" | "border";
  cssVar: string;
}

export interface TypographyRecord {
  tokenName: string;
  sizeRem: string;
  lineHeight: string;
  category: "heading" | "body";
}

export interface SpacingRecord {
  tokenName: string;
  cssValue: string;
  displayLabel: string;
}

export interface BorderRadiusRecord {
  tokenName: string;
  cssVar: string;
  label: string;
}

export interface BoxShadowRecord {
  tokenName: string;
  cssVar: string;
  label: string;
}

const PALETTE_FAMILIES = [
  "blue",
  "gray",
  "yellow",
  "green",
  "orange",
  "red",
] as const;

export function getPaletteColorRecords(): PaletteColorRecord[] {
  const colors = themeConfig.colors as Record<string, Record<string, string>>;
  const records: PaletteColorRecord[] = [];

  for (const family of PALETTE_FAMILIES) {
    const shades = colors[family];
    if (!shades || typeof shades !== "object") continue;
    for (const [shade, cssValue] of Object.entries(shades)) {
      records.push({
        tokenName: `bg-${family}-${shade}`,
        family,
        shade,
        cssValue: cssValue as string,
      });
    }
  }
  return records;
}

export function getSemanticColorRecords(): SemanticColorRecord[] {
  const extend = themeExtend;
  const records: SemanticColorRecord[] = [];

  const backgroundColors = resolveThemeSection(extend.backgroundColor);
  for (const [name, cssVar] of Object.entries(backgroundColors)) {
    records.push({
      tokenName: `bg-${name}`,
      category: "background",
      cssVar: cssVar as string,
    });
  }

  const textColors = resolveThemeSection(extend.textColor);
  for (const [name, cssVar] of Object.entries(textColors)) {
    records.push({
      tokenName: `text-${name}`,
      category: "text",
      cssVar: cssVar as string,
    });
  }

  const borderColors = resolveThemeSection(extend.borderColor);
  for (const [name, cssVar] of Object.entries(borderColors)) {
    records.push({
      tokenName: `border-${name}`,
      category: "border",
      cssVar: cssVar as string,
    });
  }

  return records;
}

function resolveThemeSection(
  section: Record<string, string> | (() => Record<string, string>)
): Record<string, string> {
  return typeof section === "function" ? section() : section;
}

export function getTypographyRecords(): TypographyRecord[] {
  const fontSizes = themeConfig.fontSize as Record<string, [string, string]>;
  return Object.entries(fontSizes)
    .filter(([name]) => name.startsWith("heading-") || name.startsWith("body-"))
    .map(([name, [sizeRem, lineHeight]]) => ({
      tokenName: `text-${name}`,
      sizeRem,
      lineHeight,
      category: (name.startsWith("heading-") ? "heading" : "body") as
        | "heading"
        | "body",
    }));
}

export function getSpacingRecords(): SpacingRecord[] {
  const spacing = themeExtend.spacing as Record<string, string>;
  return Object.entries(spacing).map(([key, cssValue]) => ({
    tokenName: `spacing-${key}`,
    cssValue,
    displayLabel: `${key} (${cssValue})`,
  }));
}

export function getBorderRadiusRecords(): BorderRadiusRecord[] {
  const borderRadius = themeExtend.borderRadius as Record<string, string>;
  return Object.entries(borderRadius).map(([name, cssVar]) => ({
    tokenName: `rounded-${name}`,
    cssVar,
    label: name,
  }));
}

export function getBoxShadowRecords(): BoxShadowRecord[] {
  const boxShadow = resolveThemeSection(themeExtend.boxShadow);
  return Object.entries(boxShadow).map(([name, cssVar]) => ({
    tokenName: `shadow-${name}`,
    cssVar,
    label: name,
  }));
}

export function getIconNames(): string[] {
  const modules = import.meta.glob("../components/global/icons/*.vue", {
    eager: false,
  });
  return Object.keys(modules).map((path) => {
    const filename = path.split("/").at(-1) ?? "";
    return filename.replace(".vue", "");
  });
}
