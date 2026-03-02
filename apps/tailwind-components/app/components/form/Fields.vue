<script setup lang="ts">
import { vIntersectionObserver } from "@vueuse/components";
import type { IRow } from "../../../../metadata-utils/src/types";
import FormField from "./FormField.vue";
import type { UseForm } from "../../composables/useForm";
import { computed } from "vue";

const props = defineProps<{
  form: UseForm;
  constantValues?: IRow;
}>();

const observerOptions = {
  root: null,
  rootMargin: "-45% 0px -45% 0px",
  threshold: 0,
};

const columns = computed(() => props.form.visibleColumns.value);

const prefix = `${props.form.metadata.value.schemaId}-${props.form.metadata.value.id}`;

function onIntersectionObserver(entries: IntersectionObserverEntry[]) {
  const highest = entries.find((entry) => entry.isIntersecting);

  if (highest) {
    const col = columns.value.find(
      (c) => highest.target.id === `${prefix}-${c.id}-form-field`
    );
    if (col) {
      props.form.onViewColumn(col);
    }
  }

  const leaving = entries.find((entry) => entry.isIntersecting === false);
  if (leaving) {
    const col = columns.value.find(
      (c) => leaving.target.id === `${prefix}-${c.id}-form-field`
    );
    if (col) {
      props.form.onLeaveViewColumn(col);
    }
  }
}
</script>

<template>
  <template v-for="column in columns" :key="column.id">
    <div
      v-if="column.columnType === 'HEADING' || column.columnType === 'SECTION'"
      :id="`${prefix}-${column.id}-form-field`"
    >
      <h2
        class="first:pt-0 pt-10 font-display md:text-heading-5xl text-heading-5xl text-form-header pb-8"
        :class="
          column.columnType === 'HEADING'
            ? 'md:text-heading-4xl text-heading-4xl'
            : 'md:text-heading-5xl text-heading-5xl'
        "
        v-if="column.id != 'mg_top_of_form'"
      >
        {{ column.label }}
      </h2>
    </div>
    <FormField
      v-else-if="!Object.keys(constantValues || {}).includes(column.id)"
      class="pb-8 last:pb-64"
      v-intersection-observer="[onIntersectionObserver, observerOptions]"
      :form="form"
      :constantValues="constantValues"
      :column="column"
    />
  </template>
</template>
