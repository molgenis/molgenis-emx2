<template>
  <nav class="pt-4 pb-8 bg-form-legend">
    <ul class="list-none">
      <li class="py-1" v-for="(section, index) in sections" :key="section.id">
        <FormLegendHeader
          :id="section.id"
          :label="section.label"
          :isActive="
            section.isActive ? true : false || (noSectionsActive && index === 0)
          "
          :errorCount="section.errorCount"
          :collapsible="collapsible"
          :hasChildren="section.headers.length > 0"
          :expanded="
            collapsible ? expandAll || expandedIds.has(section.id) : true
          "
          @goToSection="emit('goToSection', $event)"
          @toggle="toggleExpanded(section.id)"
        />
        <ul
          v-if="!collapsible || expandAll || expandedIds.has(section.id)"
          class="list-none"
        >
          <template v-for="header in section.headers" :key="header.id">
            <li class="pl-4 py-1" v-if="collapsible || header.isVisible">
              <FormLegendHeader
                :id="header.id"
                :label="header.label"
                :isActive="header.isActive ? true : false"
                :errorCount="header.errorCount"
                :collapsible="collapsible"
                :hasChildren="(header.children?.length ?? 0) > 0"
                :expanded="
                  collapsible ? expandAll || expandedIds.has(header.id) : true
                "
                @goToSection="emit('goToSection', $event)"
                @toggle="toggleExpanded(header.id)"
              />
              <ul
                v-if="
                  header.children?.length &&
                  (!collapsible || expandAll || expandedIds.has(header.id))
                "
                class="list-none"
              >
                <template v-for="child in header.children" :key="child.id">
                  <li class="pl-8 py-1" v-if="collapsible || child.isVisible">
                    <FormLegendHeader
                      :id="child.id"
                      :label="child.label"
                      :isActive="child.isActive ? true : false"
                      :errorCount="child.errorCount"
                      @goToSection="emit('goToSection', $event)"
                    />
                  </li>
                </template>
              </ul>
            </li>
          </template>
        </ul>
      </li>
    </ul>
  </nav>
</template>

<script lang="ts" setup>
import type { LegendSection } from "../../../../metadata-utils/src/types";
import { computed, ref, unref } from "vue";
import FormLegendHeader from "./legend/Header.vue";

const props = defineProps<{
  sections: LegendSection[];
  collapsible?: boolean;
  expandAll?: boolean;
}>();
const emit = defineEmits(["goToSection"]);

const noSectionsActive = computed(() => {
  return !props.sections.some((section) => section.isActive);
});

function computeActivePath(): Set<string> {
  const active = new Set<string>();
  for (const section of props.sections) {
    let sectionHasActive = !!unref(section.isActive);
    for (const header of section.headers) {
      let headerHasActive = !!unref(header.isActive);
      for (const child of header.children ?? []) {
        if (unref(child.isActive)) {
          headerHasActive = true;
          sectionHasActive = true;
        }
      }
      if (headerHasActive) {
        active.add(header.id);
        sectionHasActive = true;
      }
    }
    if (sectionHasActive) {
      active.add(section.id);
    }
  }
  return active;
}

const expandedIds = ref<Set<string>>(
  props.collapsible ? computeActivePath() : new Set<string>()
);

function toggleExpanded(id: string) {
  const next = new Set(expandedIds.value);
  if (next.has(id)) {
    next.delete(id);
  } else {
    next.add(id);
  }
  expandedIds.value = next;
}
</script>
