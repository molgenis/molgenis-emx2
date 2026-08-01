const fs = require("fs");
const path = require("path");

const COMPONENTS_ROOT = path.resolve(__dirname, "src/components");
const GENERATED_DOCS_ROOT = path.resolve(__dirname, "gen-docs/components");
const DOCS_BLOCK_PATTERN = /<docs>[\s\S.]*<\/docs>/;

function extractDocsTemplate(sourceCode) {
  const docsBlock = sourceCode.match(DOCS_BLOCK_PATTERN);
  return docsBlock ? docsBlock[0].replace(/(<docs>|<\/docs>)/g, "") : null;
}

function resolveGeneratedDocPath(componentPath) {
  const pathInsideComponents = path.relative(COMPONENTS_ROOT, componentPath);
  if (pathInsideComponents.startsWith("..")) {
    return null;
  }
  return path.join(GENERATED_DOCS_ROOT, pathInsideComponents);
}

function writeGeneratedDoc(componentPath, sourceCode) {
  const generatedDocPath = resolveGeneratedDocPath(componentPath);
  const docsTemplate = extractDocsTemplate(sourceCode);
  if (generatedDocPath === null || docsTemplate === null) {
    return;
  }
  fs.mkdirSync(path.dirname(generatedDocPath), { recursive: true });
  fs.writeFileSync(generatedDocPath, docsTemplate);
}

function listComponentFiles(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const entryPath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      return listComponentFiles(entryPath);
    }
    return entry.name.endsWith(".vue") ? [entryPath] : [];
  });
}

function generateAllDocs() {
  fs.rmSync(GENERATED_DOCS_ROOT, { recursive: true, force: true });
  listComponentFiles(COMPONENTS_ROOT).forEach((componentPath) => {
    writeGeneratedDoc(componentPath, fs.readFileSync(componentPath, "utf8"));
  });
}

module.exports = { writeGeneratedDoc, generateAllDocs };
