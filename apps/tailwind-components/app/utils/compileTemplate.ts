import { compile } from "@vue/compiler-dom";
import * as VueRuntime from "vue";
import { defineComponent } from "vue";
import type { Component } from "vue";

export type CompileResult =
  | { component: Component; error: null }
  | { component: null; error: string };

function pathToComponentName(globKey: string): string {
  const withoutPrefix = globKey.replace(/^\.\.\/components\//, "");
  const withoutExt = withoutPrefix.replace(/\.vue$/, "");
  const segments = withoutExt.split("/");
  return segments
    .map((seg) => seg.charAt(0).toUpperCase() + seg.slice(1))
    .join("");
}

const componentModules = import.meta.glob(
  [
    "../components/**/*.vue",
    "!../components/editor/**",
    "!../components/Demo.vue",
  ],
  { eager: true }
) as Record<string, { default: Component }>;

const designSystemComponents: Record<string, Component> = {};
for (const [path, mod] of Object.entries(componentModules)) {
  const name = pathToComponentName(path);
  designSystemComponents[name] = mod.default;
}

export function compileTemplate(template: string): CompileResult {
  try {
    const { code } = compile(template, { mode: "function" });
    const render = new Function("Vue", code)(VueRuntime);
    return {
      component: defineComponent({
        components: designSystemComponents,
        render,
      }),
      error: null,
    };
  } catch (err) {
    return {
      component: null,
      error: err instanceof Error ? err.message : String(err),
    };
  }
}
