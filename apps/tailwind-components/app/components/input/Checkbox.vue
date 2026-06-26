<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from "vue";

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

// Drive both `checked` and `indeterminate` imperatively from the model. A
// checkbox's DOM state is a poor fit for a reactive `:checked` binding when the
// value is driven externally and `preventClick` cancels the native toggle: the
// browser restores the pre-click checked/indeterminate state on a cancelled
// click, which a declarative binding does not reliably correct. Re-asserting
// here keeps all/some/none states in sync.
function syncDom() {
  const el = checkboxRef.value;
  if (!el) return;
  el.checked = isChecked.value;
  el.indeterminate = props.indeterminate ?? false;
}

onMounted(syncDom);
watch([isChecked, () => props.indeterminate], syncDom);

function onChange(event: Event) {
  modelValue.value = (event.target as HTMLInputElement).checked;
}

function onClick(event: MouseEvent) {
  if (props.preventClick) {
    event.preventDefault();
    // The browser may have toggled checked / cleared indeterminate during the
    // (now cancelled) click, and the external selection update lands a tick
    // later; re-assert the model-driven state once it has settled.
    nextTick(syncDom);
  }
}
</script>

<template>
  <input
    ref="checkboxRef"
    @change="onChange"
    @click="onClick"
    type="checkbox"
    class="w-5 h-5 hover:cursor-pointer accent-theme"
  />
</template>
