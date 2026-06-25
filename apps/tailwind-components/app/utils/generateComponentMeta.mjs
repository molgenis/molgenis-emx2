import { fileURLToPath } from "url";
import { resolve, join, relative } from "path";
import fs from "fs/promises";
import { createRequire } from "module";

export function deriveComponentName(filePath, baseDir) {
  const relativeParts = relative(baseDir, filePath).split("/");
  const segments = relativeParts.map((part) => part.replace(".vue", ""));
  const lastSegment = segments[segments.length - 1];
  const parentSegment = segments[segments.length - 2];
  const collapsed =
    parentSegment !== undefined && lastSegment === parentSegment
      ? segments.slice(0, -1)
      : segments;
  return collapsed
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join("");
}

function normalizeType(rawType) {
  return rawType.replace(/\s*\|\s*undefined$/, "").trim() || rawType;
}

const PRIMITIVE_KINDS = new Set([
  "string",
  "number",
  "boolean",
  "null",
  "undefined",
  "any",
  "unknown",
  "never",
  "void",
  "symbol",
  "bigint",
]);

function isPrimitiveType(typeStr) {
  return PRIMITIVE_KINDS.has(typeStr.trim());
}

export function extractTypeDetail(schema) {
  if (!schema || typeof schema === "string") return null;

  if (schema.kind === "enum") {
    const rawOptions = Array.isArray(schema.schema) ? schema.schema : [];
    const options = rawOptions
      .map((entry) => (typeof entry === "string" ? entry : entry.type ?? ""))
      .filter(Boolean)
      .filter((opt) => opt !== "undefined");
    if (options.length === 0) return null;
    if (options.length === 1 && isPrimitiveType(options[0])) return null;
    return { kind: "union", options };
  }

  if (schema.kind === "array") {
    const firstChild = Array.isArray(schema.schema)
      ? schema.schema[0]
      : undefined;
    const elementType =
      typeof firstChild === "string"
        ? firstChild
        : firstChild?.type ?? schema.type.replace(/\[\]$/, "").trim();
    if (!elementType || isPrimitiveType(elementType)) return null;
    return { kind: "array", elementType };
  }

  if (schema.kind === "object") {
    const rawMembers = schema.schema ?? {};
    const members = {};
    for (const [memberName, memberMeta] of Object.entries(rawMembers)) {
      members[memberName] =
        typeof memberMeta === "string" ? memberMeta : memberMeta.type ?? "";
    }
    if (Object.keys(members).length === 0) return null;
    return { kind: "object", members };
  }

  return null;
}

function extractProps(rawProps) {
  return rawProps
    .filter((prop) => !prop.global)
    .map((prop) => {
      const typeDetail = extractTypeDetail(prop.schema) ?? undefined;
      const entry = {
        name: prop.name,
        type: normalizeType(prop.type),
        default: prop.default !== undefined ? prop.default : undefined,
        required: prop.required ?? false,
        description: prop.description ?? "",
      };
      if (typeDetail !== undefined) {
        entry.typeDetail = typeDetail;
      }
      return entry;
    });
}

function extractEvents(rawEvents) {
  return rawEvents.map((event) => ({
    name: event.name,
    type: event.type ?? "",
    description: event.description ?? "",
  }));
}

function extractSlots(rawSlots) {
  return rawSlots.map((slot) => ({
    name: slot.name,
    type: slot.type ?? "{}",
    description: slot.description ?? "",
  }));
}

async function collectVueFiles(dir, results = []) {
  const entries = await fs.readdir(dir);
  for (const entry of entries) {
    const entryPath = join(dir, entry);
    const stat = await fs.stat(entryPath);
    if (stat.isDirectory()) {
      await collectVueFiles(entryPath, results);
    } else if (entry.endsWith(".vue")) {
      results.push(entryPath);
    }
  }
  return results;
}

export function createMetaChecker(tsconfigPath) {
  const require = createRequire(import.meta.url);
  const { createChecker } = require("vue-component-meta");
  return createChecker(tsconfigPath, {
    forceUseTs: true,
    printer: { newLine: 1 },
  });
}

export function getComponentMetaEntry(
  checker,
  filePath,
  componentsDir,
  appRootDir
) {
  const componentName = deriveComponentName(filePath, componentsDir);
  const raw = checker.getComponentMeta(filePath);
  return {
    componentName,
    filePath: relative(appRootDir, filePath),
    props: extractProps(raw.props),
    events: extractEvents(raw.events),
    slots: extractSlots(raw.slots),
  };
}

async function generateComponentMeta() {
  const appDir = fileURLToPath(new URL("..", import.meta.url));
  const appRootDir = resolve(appDir, "..");
  const componentsDir = join(appDir, "components");
  const tsconfigPath = resolve(appRootDir, ".nuxt/tsconfig.app.json");
  const outputFile = resolve(appRootDir, "componentMetaMap.json");

  const tsconfigExists = await fs
    .access(tsconfigPath)
    .then(() => true)
    .catch(() => false);

  if (!tsconfigExists) {
    console.error(
      "❌ .nuxt/tsconfig.app.json not found — run `nuxt prepare` first"
    );
    process.exit(1);
  }

  const checker = createMetaChecker(tsconfigPath);
  const vueFiles = await collectVueFiles(componentsDir);
  const componentMetaMap = {};

  for (const filePath of vueFiles) {
    const componentName = deriveComponentName(filePath, componentsDir);
    try {
      if (componentMetaMap[componentName]) {
        console.warn(
          `⚠️  Key collision for "${componentName}" — skipping duplicate at ${filePath}`
        );
        continue;
      }
      componentMetaMap[componentName] = getComponentMetaEntry(
        checker,
        filePath,
        componentsDir,
        appRootDir
      );
    } catch (err) {
      console.warn(`⚠️  Skipped ${componentName}: ${err.message}`);
    }
  }

  await fs.writeFile(outputFile, JSON.stringify(componentMetaMap, null, 2));
  console.log(
    `✅ componentMetaMap.json written (${
      Object.keys(componentMetaMap).length
    } components)`
  );
}

const isMainModule =
  typeof import.meta.url === "string" &&
  import.meta.url.startsWith("file://") &&
  process.argv[1] === fileURLToPath(import.meta.url);

if (isMainModule) {
  generateComponentMeta();
}
