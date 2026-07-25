import { existsSync, readFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { filesUnder } from "./built-files.mjs";

const consumerDirectory = resolve(process.argv[2] ?? ".");
const consumerPagesDirectory = join(consumerDirectory, "app/pages");
const builtDirectories = [
  join(consumerDirectory, ".output/public"),
  join(consumerDirectory, ".output/server"),
].filter(existsSync);
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
  `assert-built-routes: ${builtRouteNames.size} routes, no playground pages routable or bundled`
);

function readBuiltJavascript() {
  return builtDirectories
    .flatMap((directory) =>
      filesUnder(directory)
        .filter((file) => /\.(js|mjs)$/.test(file))
        .filter((file) => !file.includes("nuxt-monaco-editor/"))
        .map((file) => readFileSync(join(directory, file), "utf8"))
    )
    .join("\n")
    .replace(/\s+/g, " ");
}

function routeNamesIn(javascript) {
  return new Set(
    [...javascript.matchAll(/name:(["'`])([^"'`]+)\1,path:["'`]/g)].map(
      ([, , name]) => name
    )
  );
}

function routeNamesOf(pagesDirectory) {
  return filesUnder(pagesDirectory)
    .filter((file) => file.endsWith(".vue"))
    .map(routeNameOf);
}

function routeNameOf(pageFile) {
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

function pagesWithTheirTextIn(pagesDirectory, javascript) {
  return filesUnder(pagesDirectory)
    .filter((file) => file.endsWith(".vue"))
    .filter((file) => {
      const text = longestTemplateTextOf(join(pagesDirectory, file));
      return text !== null && javascript.includes(text);
    });
}

function longestTemplateTextOf(pageFile) {
  const template = readFileSync(pageFile, "utf8").replace(
    /<script[\s\S]*?<\/script>/g,
    ""
  );
  return (
    [...template.matchAll(/>([^<>{}]+)</g)]
      .map(([, text]) => text.replace(/\s+/g, " ").trim())
      .filter(
        (text) => text.length >= 40 && /^[A-Za-z0-9 ,.:;!?()'-]+$/.test(text)
      )
      .sort((first, second) => second.length - first.length)[0] ?? null
  );
}

function fail(message) {
  console.error(`assert-built-routes: ${message}`);
  process.exit(1);
}
