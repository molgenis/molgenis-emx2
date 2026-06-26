function dedent(content: string): string {
  const lines = content.split("\n");
  const nonEmptyLines = lines.filter((line) => line.trim().length > 0);
  if (nonEmptyLines.length === 0) return content.trim();

  const minIndent = Math.min(
    ...nonEmptyLines.map((line) => line.match(/^(\s*)/)?.[1]?.length ?? 0)
  );

  if (minIndent === 0) return content.trim();

  return lines
    .map((line) => line.slice(minIndent))
    .join("\n")
    .trim();
}

export function extractTemplateBody(rawVueSource: string): string {
  const openMatch = /^<template>/m.exec(rawVueSource);
  if (!openMatch) return "";

  const contentStart = openMatch.index + openMatch[0].length;
  const remaining = rawVueSource.slice(contentStart);

  let depth = 1;
  let pos = 0;

  while (pos < remaining.length) {
    const openIdx = remaining.indexOf("<template", pos);
    const closeIdx = remaining.indexOf("</template>", pos);

    if (closeIdx === -1) return "";

    if (openIdx !== -1 && openIdx < closeIdx) {
      depth++;
      pos = openIdx + "<template".length;
    } else {
      depth--;
      if (depth === 0) {
        return dedent(remaining.slice(0, closeIdx));
      }
      pos = closeIdx + "</template>".length;
    }
  }

  return "";
}

export function extractDemoSource(rawVueSource: string, id: string): string {
  const escapedId = id.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const openTagPattern = new RegExp(`<Demo\\b[^>]*\\sid="${escapedId}"[^>]*>`);

  const match = openTagPattern.exec(rawVueSource);
  if (!match) return "";

  const contentStart = match.index + match[0].length;
  const remaining = rawVueSource.slice(contentStart);

  const closeIndex = remaining.indexOf("</Demo>");
  if (closeIndex === -1) return "";

  return dedent(remaining.slice(0, closeIndex));
}
