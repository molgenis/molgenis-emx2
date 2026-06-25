import { defineNuxtModule } from "nuxt/kit";
import { resolve, join, relative, isAbsolute } from "path";
import fs from "fs/promises";

export default defineNuxtModule({
  meta: { name: "componentMetaWatch" },
  async setup(_, nuxt) {
    if (!nuxt.options.dev) return;

    const appRootDir = nuxt.options.rootDir;
    const srcDir = nuxt.options.srcDir;
    const componentsDir = join(srcDir, "components");
    const tsconfigPath = resolve(appRootDir, ".nuxt/tsconfig.app.json");
    const outputFile = resolve(appRootDir, "componentMetaMap.json");

    const generateMeta = await import("../app/utils/generateComponentMeta.mjs");
    const { createMetaChecker, getComponentMetaEntry } = generateMeta;

    let checker: unknown = null;
    let debounceTimer: ReturnType<typeof setTimeout> | null = null;

    const ensureChecker = (): unknown => {
      if (!checker) {
        checker = createMetaChecker(tsconfigPath);
        console.info(
          "[componentMetaWatch] watching app/components for .vue changes …"
        );
      }
      return checker;
    };

    const regenerateEntry = async (filePath: string): Promise<void> => {
      try {
        const activeChecker = ensureChecker();
        const entry = getComponentMetaEntry(
          activeChecker,
          filePath,
          componentsDir,
          appRootDir
        );
        const raw = await fs.readFile(outputFile, "utf-8").catch(() => "{}");
        const map = JSON.parse(raw) as Record<string, unknown>;
        map[entry.componentName] = entry;
        await fs.writeFile(outputFile, JSON.stringify(map, null, 2));
      } catch (err) {
        console.warn(
          `[componentMetaWatch] failed to regenerate ${filePath}: ${
            (err as Error).message
          }`
        );
      }
    };

    nuxt.hook("builder:watch", (event: string, changedPath: string) => {
      if (event !== "change" && event !== "add") return;
      if (!changedPath.endsWith(".vue")) return;
      const absPath = isAbsolute(changedPath)
        ? changedPath
        : resolve(nuxt.options.rootDir, changedPath);
      const relToComponents = relative(componentsDir, absPath);
      if (relToComponents.startsWith("..")) return;
      if (debounceTimer) clearTimeout(debounceTimer);
      debounceTimer = setTimeout(() => {
        regenerateEntry(absPath);
      }, 200);
    });
  },
});
