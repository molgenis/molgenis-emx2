<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";

const props = withDefaults(
  defineProps<{
    indeterminate?: boolean;
    preventClick?: boolean;
  }>(),
  {
    indeterminate: false,
    preventClick: false,
  }
);
const modelValue = defineModel<string | boolean>();
const checkboxRef = ref<HTMLInputElement | null>(null);

const isChecked = computed(
  () => modelValue.value === true || modelValue.value === "true"
);

// Drive `checked` and `indeterminate` imperatively from the model. Two reasons a
// declarative `:checked` binding is not enough: `indeterminate` has no template
// binding, and when a click's resulting model value is unchanged (e.g. a
// partially-selected "select all" -> select-none keeps `checked` false) Vue would
// not re-patch the `checked` the browser just toggled. Running this on the `post`
// flush means it executes after the model-driven re-render has settled, so the
// model always has the final say over the native toggle.
function syncDom() {
  const el = checkboxRef.value;
  if (!el) return;
  el.checked = isChecked.value;
  el.indeterminate = props.indeterminate ?? false;
}

onMounted(syncDom);
watch([isChecked, () => props.indeterminate], syncDom, { flush: "post" });

function onChange(event: Event) {
  // In `preventClick` mode the parent owns the (de)selection — e.g. a Gmail-style
  // select-all that maps a single click to select-all / select-none. Ignore the
  // native toggle here and let the authoritative model flow back via `syncDom`.
  // Note: we deliberately do NOT call `preventDefault()` on the click. Cancelling
  // the activation makes the browser revert the checkbox's checkedness in a native
  // step that races with — and beats — our sync, leaving the DOM out of sync.
  if (props.preventClick) return;
  modelValue.value = (event.target as HTMLInputElement).checked;
}
</script>

<template>
  <input
    ref="checkboxRef"
    @change="onChange"
    type="checkbox"
    class="w-5 h-5 hover:cursor-pointer accent-theme"
  />
</template>
