import { readFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { builtJavascriptOf, relativeFilePathsUnder } from "./built-files.ts";

const consumerDirectory = resolve(process.argv[2] ?? ".");
const consumerPagesDirectory = join(consumerDirectory, "app/pages");
const playgroundPagesDirectory = fileURLToPath(
  new URL("../app/pages", import.meta.url)
);

const builtJavascript = readBuiltJavascript();
const builtRouteNames = routeNamesIn(builtJavascript);
const routablePlaygroundPages = routeNamesOf(playgroundPagesDirectory)
  .filter((name) => name !== "index")
  .filter((name) => builtRouteNames.has(name));
const missingConsumerRoutes = routeNamesOf(consumerPagesDirectory).filter(
  (name) => !builtRouteNames.has(name)
);
const bundledPlaygroundPages = pagesWithTheirTextIn(
  playgroundPagesDirectory,
  builtJavascript
);
const consumerPagesProvingTextSurvivesBundling = pagesWithTheirTextIn(
  consumerPagesDirectory,
  builtJavascript
);

if (builtRouteNames.size === 0) {
  fail(`no route table found in ${consumerDirectory}/.output — build first`);
}
if (consumerPagesProvingTextSurvivesBundling.length === 0) {
  fail("no own page text found in the bundle — this check cannot detect text");
}
if (routablePlaygroundPages.length > 0) {
  fail(
    `tailwind-components playground pages are routable: ${routablePlaygroundPages.join(
      ", "
    )}`
  );
}
if (bundledPlaygroundPages.length > 0) {
  fail(
    `tailwind-components playground pages are bundled: ${bundledPlaygroundPages.join(
      ", "
    )}`
  );
}
if (missingConsumerRoutes.length > 0) {
  fail(
    `own pages missing from the built route table: ${missingConsumerRoutes.join(
      ", "
    )}`
  );
}
console.log(
  `check-built-routes: ${builtRouteNames.size} routes, no playground pages routable or bundled`
);

function readBuiltJavascript(): string {
  return builtJavascriptOf(consumerDirectory).join("\n").replace(/\s+/g, " ");
}

function routeNamesIn(javascript: string): Set<string> {
  return new Set(
    [...javascript.matchAll(/name:(["'`])([^"'`]+)\1,path:["'`]/g)].map(
      ([, , name]) => name as string
    )
  );
}

function routeNamesOf(pagesDirectory: string): string[] {
  return relativeFilePathsUnder(pagesDirectory)
    .filter((file) => file.endsWith(".vue"))
    .map(routeNameOf);
}

function routeNameOf(pageFile: string): string {
  const withoutExtension = pageFile
    .slice(0, -".vue".length)
    .replace(/(^|\/)index$/, "");
  return (
    withoutExtension
      .replace(/\[\.\.\./g, "")
      .replace(/[[\]]/g, "")
      .replace(/\//g, "-") || "index"
  );
}

function pagesWithTheirTextIn(
  pagesDirectory: string,
  javascript: string
): string[] {
  return relativeFilePathsUnder(pagesDirectory)
    .filter((file) => file.endsWith(".vue"))
    .filter((file) => {
      const text = longestTemplateTextOf(join(pagesDirectory, file));
      return text !== null && javascript.includes(text);
    });
}

function longestTemplateTextOf(pageFile: string): string | null {
  const template = readFileSync(pageFile, "utf8").replace(
    /<script[\s\S]*?<\/script>/g,
    ""
  );
  return (
    [...template.matchAll(/>([^<>{}]+)</g)]
      .map(([, text]) => (text as string).replace(/\s+/g, " ").trim())
      .filter(
        (text) => text.length >= 40 && /^[A-Za-z0-9 ,.:;!?()'-]+$/.test(text)
      )
      .sort((first, second) => second.length - first.length)[0] ?? null
  );
}

function fail(message: string): never {
  console.error(`check-built-routes: ${message}`);
  process.exit(1);
}
