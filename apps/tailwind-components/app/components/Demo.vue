<template>
  <div>
    <h3 v-if="title" class="text-2xl text-title">{{ title }}</h3>

    <div v-if="isExpanded && compiledComponent">
      <component :is="compiledComponent" />
    </div>
    <slot v-else />

    <div class="mt-2">
      <div class="flex items-center gap-2">
        <Button
          :id="`${effectiveId}-demo-source-toggle`"
          type="text"
          size="small"
          :icon="isExpanded ? 'CaretUp' : 'CaretDown'"
          icon-position="right"
          :aria-controls="`${effectiveId}-demo-source-content`"
          :aria-expanded="isExpanded"
          @click.prevent="togglePanel"
        >
          <span v-if="!isExpanded">Show source</span>
          <span v-else>Hide source</span>
        </Button>

        <Button
          type="text"
          size="small"
          icon="Copy"
          :icon-only="true"
          label="Copy source"
          @click="copySource"
        />
        <span
          class="text-xs px-1 rounded transition-opacity"
          :class="copied ? 'opacity-100 text-valid' : 'opacity-0'"
          aria-live="polite"
        >
          Copied
        </span>
      </div>

      <div v-if="isExpanded" :id="`${effectiveId}-demo-source-content`">
        <ClientOnly>
          <CodeEditor v-model="editableSource" lang="html" />
        </ClientOnly>
        <p v-if="compileError || runtimeError" class="text-error text-sm mt-2">
          {{ compileError || runtimeError }}
        </p>
        <p v-else-if="!demoSource">No source found for demo "{{ effectiveId }}".</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  computed,
  defineAsyncComponent,
  ref,
  watch,
  onErrorCaptured,
  nextTick,
} from "vue";
import { useDebounceFn, useClipboard } from "@vueuse/core";
import { useRoute } from "vue-router";
import { globKeyToRouteKey } from "../utils/sourceCode";
import { extractDemoSource } from "../utils/demoSource";
import { compileTemplate } from "../utils/compileTemplate";
import type { Component } from "vue";

const CodeEditor = defineAsyncComponent(
  () => import("./editor/CodeEditor.vue")
);

const props = defineProps<{
  id?: string;
  title?: string;
  source?: string;
}>();

const effectiveId = computed(() => props.id ?? "page-demo");

const storyGlob = import.meta.glob("../pages/**/*.vue", {
  query: "?raw",
  import: "default",
  eager: true,
}) as Record<string, string>;

const route = useRoute();

const rawSource = computed<string>(() => {
  if (props.source !== undefined) return "";
  const routeKey = `${route.path}.vue`;
  const matchEntry = Object.entries(storyGlob).find(
    ([globKey]) => globKeyToRouteKey(globKey) === routeKey
  );
  return matchEntry ? matchEntry[1] : "";
});

const demoSource = computed<string>(() => {
  if (props.source !== undefined) return props.source;
  if (!props.id) return "";
  return extractDemoSource(rawSource.value, props.id);
});

const isExpanded = ref<boolean>(false);
const editableSource = ref<string>("");
const compiledComponent = ref<Component | null>(null);
const lastGoodComponent = ref<Component | null>(null);
const compileError = ref<string | null>(null);
const runtimeError = ref<string | null>(null);

const { copy } = useClipboard({ legacy: true });
const copied = ref(false);

async function copySource() {
  await copy(editableSource.value || demoSource.value);
  copied.value = true;
  setTimeout(() => {
    copied.value = false;
  }, 1500);
}

function compileSource(source: string) {
  if (!source) return;
  const result = compileTemplate(source);
  if (result.error) {
    compileError.value = result.error;
  } else {
    compileError.value = null;
    runtimeError.value = null;
    compiledComponent.value = result.component;
    nextTick(() => {
      if (!runtimeError.value && compiledComponent.value === result.component) {
        lastGoodComponent.value = result.component;
      }
    });
  }
}

const compileSourceDebounced = useDebounceFn(compileSource, 400);

watch(isExpanded, (expanded) => {
  if (expanded) {
    if (!editableSource.value) {
      editableSource.value = demoSource.value;
    }
    if (editableSource.value) {
      compileSource(editableSource.value);
    }
  }
});

watch(editableSource, (source) => {
  if (source) {
    compileSourceDebounced(source);
  }
});

onErrorCaptured((err) => {
  runtimeError.value = err instanceof Error ? err.message : String(err);
  compiledComponent.value = lastGoodComponent.value;
  return false;
});

function togglePanel() {
  isExpanded.value = !isExpanded.value;
}
</script>
