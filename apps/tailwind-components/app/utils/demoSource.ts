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
