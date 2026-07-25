import { fileURLToPath } from "node:url";
import type { NuxtApp, NuxtPage } from "nuxt/schema";

const playgroundPagesDirectory = fileURLToPath(
  new URL("./app/pages/", import.meta.url)
);
const playgroundLayoutFile = fileURLToPath(
  new URL("./app/layouts/playground.vue", import.meta.url)
);
const layoutDemonstratedByPage: Record<string, string> = {
  "/layouts/defaultLayout": "default",
  "/layouts/wideLayout": "wide",
};

export function setPlaygroundLayout(pages: NuxtPage[]) {
  for (const page of pages) {
    if (!page.file?.startsWith(playgroundPagesDirectory)) {
      continue;
    }
    page.meta = {
      ...page.meta,
      layout: layoutDemonstratedByPage[page.path] ?? "playground",
    };
  }
}

export function removePlaygroundPages(pages: NuxtPage[]) {
  for (let index = pages.length - 1; index >= 0; index--) {
    const page = pages[index];
    if (!page) {
      continue;
    }
    if (page.file?.startsWith(playgroundPagesDirectory)) {
      pages.splice(index, 1);
    } else if (page.children) {
      removePlaygroundPages(page.children);
    }
  }
}

export function removePlaygroundLayout(app: NuxtApp) {
  app.layouts = Object.fromEntries(
    Object.entries(app.layouts).filter(
      ([, layout]) => layout.file !== playgroundLayoutFile
    )
  );
}
