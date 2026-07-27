const ALWAYS_FORBIDDEN = ["REFBACK", "PARTS", "ONTOLOGY_ARRAY"];

const FAMILY_MEMBERS = [
  "REF",
  "REF_ARRAY",
  "SELECT",
  "RADIO",
  "CHECKBOX",
  "MULTISELECT",
  "ONTOLOGY",
  "HEADING",
  "SECTION",
];

const LAYOUT_NAMES = ["HEADING", "SECTION"];

const COLUMN_TYPE_NAMES = [...ALWAYS_FORBIDDEN, ...FAMILY_MEMBERS];

const SUGGESTED_PREDICATE = {
  REFBACK: "isRefbackType",
  PARTS: "isPartsType",
  ONTOLOGY_ARRAY: "isOntologyArrayType",
  REF: "isSingleRefType / isRefType",
  REF_ARRAY: "isRefArrayType",
  SELECT: "isSingleRefType",
  RADIO: "isSingleRefType",
  CHECKBOX: "isRefArrayType",
  MULTISELECT: "isRefArrayType",
  ONTOLOGY: "isOntologyType / isSingleOntologyType",
  HEADING: "isLayoutColumnType / isPlainHeadingType",
  SECTION: "isLayoutColumnType / isSectionType",
};

const COLUMN_TYPE_VOCABULARY_OWNERS = [
  "metadata-utils/src/fieldHelpers.ts",
  "metadata-utils/src/types.ts",
  "schema/src/columnTypes.js",
  "no-column-type-literals.js",
];

const SCANNED_EXTENSIONS = [".ts", ".js", ".mjs", ".cjs", ".vue"];

const UNSCANNED_DIRECTORIES = new Set([
  "node_modules",
  "dist",
  "build",
  "coverage",
  "gen-docs",
  "showCase",
  "public",
  "playwright-report",
  "test-results",
  "test",
  "tests",
  "e2e",
  "__tests__",
  "__mocks__",
]);

const UNSCANNED_FILENAMES = /(\.spec\.|\.test\.|\.story\.|\.stories\.|mock)/i;

function isScannedFile(relativePath) {
  if (COLUMN_TYPE_VOCABULARY_OWNERS.includes(relativePath)) return false;
  const segments = relativePath.split("/");
  const fileName = segments.pop();
  if (!SCANNED_EXTENSIONS.some((suffix) => fileName.endsWith(suffix)))
    return false;
  if (UNSCANNED_FILENAMES.test(fileName)) return false;
  return segments.every(
    (segment) => !segment.startsWith(".") && !UNSCANNED_DIRECTORIES.has(segment)
  );
}

const quotedName = (name) => new RegExp(`(['"\`])${name}\\1`);

const blankExceptNewlines = (text) => text.replace(/[^\n]/g, " ");

function blankSfcBlocks(source) {
  return source.replace(
    /^<(docs|style)(\s[^>]*)?>[\s\S]*?^<\/\1>/gim,
    blankExceptNewlines
  );
}

function blankComments(source) {
  let result = "";
  let index = 0;
  let quote = null;

  while (index < source.length) {
    const character = source[index];
    const pair = source.slice(index, index + 2);

    if (quote) {
      if (character === "\\") {
        result += source.slice(index, index + 2);
        index += 2;
        continue;
      }
      if (character === quote || (quote !== "`" && character === "\n")) {
        quote = null;
      }
      result += character;
      index += 1;
      continue;
    }

    if (character === "'" || character === '"' || character === "`") {
      quote = character;
      result += character;
      index += 1;
      continue;
    }

    if (pair === "//") {
      const end = source.indexOf("\n", index);
      const stop = end === -1 ? source.length : end;
      result += blankExceptNewlines(source.slice(index, stop));
      index = stop;
      continue;
    }

    const blockComment = source.startsWith("/*", index)
      ? { open: "/*", close: "*/" }
      : source.startsWith("<!--", index)
      ? { open: "<!--", close: "-->" }
      : null;
    if (blockComment) {
      const end = source.indexOf(
        blockComment.close,
        index + blockComment.open.length
      );
      const stop = end === -1 ? source.length : end + blockComment.close.length;
      result += blankExceptNewlines(source.slice(index, stop));
      index = stop;
      continue;
    }

    result += character;
    index += 1;
  }

  return result;
}

function toExecutableSource(source, file) {
  const withoutDemoBlocks = file.endsWith(".vue")
    ? blankSfcBlocks(source)
    : source;
  return blankComments(withoutDemoBlocks);
}

const CONTINUES_EXPRESSION = /(\|\||&&|[=([?:])$/;

const ENDS_STATEMENT = /[;{}]$/;

function namesOnLine(line) {
  return COLUMN_TYPE_NAMES.filter((name) => quotedName(name).test(line));
}

function blankStringBodies(source) {
  return source.replace(
    /'(?:\\.|[^\\'\n])*'|"(?:\\.|[^\\"\n])*"|`(?:\\.|[^\\`])*`/g,
    (literal) =>
      literal[0] +
      blankExceptNewlines(literal.slice(1, -1)) +
      literal[literal.length - 1]
  );
}

function bracketDelta(line) {
  let delta = 0;
  for (const character of line) {
    if (character === "(" || character === "[") delta += 1;
    if (character === ")" || character === "]") delta -= 1;
  }
  return delta;
}

function toLogicalLines(source) {
  const rawLines = source.split("\n");
  const depthLines = blankStringBodies(source).split("\n");
  const logicalLines = [];
  let group = null;
  let depth = 0;

  rawLines.forEach((rawLine, index) => {
    if (!group) group = { line: index + 1, parts: [] };
    group.parts.push(rawLine.trim());
    depth = Math.max(0, depth + bracketDelta(depthLines[index]));
    const withoutStrings = depthLines[index].trim();
    const continues =
      !ENDS_STATEMENT.test(withoutStrings) &&
      (depth > 0 || CONTINUES_EXPRESSION.test(withoutStrings));
    if (!continues) {
      logicalLines.push({
        line: group.line,
        code: group.parts.join(" ").replace(/\s+/g, " ").trim(),
        rawLines: group.parts,
      });
      group = null;
    }
  });

  if (group) {
    logicalLines.push({
      line: group.line,
      code: group.parts.join(" ").replace(/\s+/g, " ").trim(),
      rawLines: group.parts,
    });
  }

  return logicalLines;
}

function findColumnTypeLiterals(source, file) {
  const violations = [];

  for (const logicalLine of toLogicalLines(source)) {
    const names = [
      ...new Set(logicalLine.rawLines.flatMap((line) => namesOnLine(line))),
    ].sort();
    if (names.length === 0) continue;

    const forbidden = names.filter((name) => ALWAYS_FORBIDDEN.includes(name));
    const family = names.filter((name) => FAMILY_MEMBERS.includes(name));
    const layoutUsedAsColumnType =
      names.some((name) => LAYOUT_NAMES.includes(name)) &&
      logicalLine.code.includes("columnType");
    if (forbidden.length === 0 && family.length < 2 && !layoutUsedAsColumnType)
      continue;

    violations.push({
      file,
      line: logicalLine.line,
      code: logicalLine.code,
      names,
    });
  }

  return violations;
}

function scanWorkspace(workspaceRoot) {
  const fs = require("node:fs");
  const path = require("node:path");
  const violations = [];

  const visit = (directory) => {
    for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
      const absolute = path.join(directory, entry.name);
      const relative = path.relative(workspaceRoot, absolute);
      if (entry.isDirectory()) {
        if (
          !entry.name.startsWith(".") &&
          !UNSCANNED_DIRECTORIES.has(entry.name)
        )
          visit(absolute);
        continue;
      }
      if (!isScannedFile(relative)) continue;
      const source = toExecutableSource(
        fs.readFileSync(absolute, "utf8"),
        relative
      );
      violations.push(...findColumnTypeLiterals(source, relative));
    }
  };

  visit(workspaceRoot);
  return violations.sort(
    (left, right) =>
      left.file.localeCompare(right.file) || left.line - right.line
  );
}

function suggestPredicates(names) {
  return [...new Set(names.map((name) => SUGGESTED_PREDICATE[name]))].join(
    " or "
  );
}

function reconcileWithAllowlist(violations, allowlist) {
  const keyOf = (entry) => JSON.stringify([entry.file, entry.code]);
  const occurrences = new Map();
  for (const found of violations) {
    const seen = occurrences.get(keyOf(found));
    if (seen) {
      seen.count += 1;
      continue;
    }
    occurrences.set(keyOf(found), {
      file: found.file,
      line: found.line,
      code: found.code,
      names: found.names,
      count: 1,
    });
  }

  const problems = [];
  const accepted = new Map(allowlist.map((entry) => [keyOf(entry), entry]));

  for (const [key, found] of occurrences) {
    const entry = accepted.get(key);
    if (!entry) {
      problems.push(
        `${found.file}:${found.line} \`${
          found.code
        }\` — use ${suggestPredicates(
          found.names
        )} from metadata-utils/src/fieldHelpers.ts`
      );
    } else if (entry.count !== found.count) {
      problems.push(
        `${found.file}: \`${found.code}\` occurs ${found.count} times, allowlist accepts ${entry.count}`
      );
    }
  }

  for (const [key, entry] of accepted) {
    if (!occurrences.has(key)) {
      problems.push(
        `${entry.file}: stale allowlist entry, \`${entry.code}\` no longer occurs`
      );
    }
  }

  return problems;
}

module.exports = {
  ALWAYS_FORBIDDEN,
  FAMILY_MEMBERS,
  LAYOUT_NAMES,
  COLUMN_TYPE_NAMES,
  SUGGESTED_PREDICATE,
  COLUMN_TYPE_VOCABULARY_OWNERS,
  isScannedFile,
  toExecutableSource,
  findColumnTypeLiterals,
  reconcileWithAllowlist,
  scanWorkspace,
};
