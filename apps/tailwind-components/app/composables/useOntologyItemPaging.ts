import { computed, reactive, ref, watch, type Ref } from "vue";

/**
 * Mirrors filter/Tree.vue's SHOW_MORE_THRESHOLD/STEP behaviour, but hides
 * rows past the bound with CSS instead of slicing them out of the DOM: a
 * crawler and a screen reader still see the whole level.
 */
export function useOntologyItemPaging(
  itemCount: Ref<number>,
  maxItems: Ref<number | undefined>,
  itemStep: Ref<number>
) {
  const visibleCount = ref(maxItems.value);

  watch(maxItems, (value) => (visibleCount.value = value));

  const isBounded = computed(() => maxItems.value != null);

  const isFullyExpanded = computed(
    () => !isBounded.value || (visibleCount.value ?? 0) >= itemCount.value
  );

  const showControl = computed(
    () => isBounded.value && itemCount.value > (maxItems.value as number)
  );

  const controlLabel = computed(() =>
    isFullyExpanded.value ? "Show less" : "Show more"
  );

  function isHidden(index: number): boolean {
    return (
      isBounded.value &&
      !isFullyExpanded.value &&
      index >= (visibleCount.value ?? 0)
    );
  }

  function toggle() {
    if (isFullyExpanded.value) {
      visibleCount.value = maxItems.value;
    } else {
      visibleCount.value = Math.min(
        (visibleCount.value ?? 0) + itemStep.value,
        itemCount.value
      );
    }
  }

  // reactive(), not a plain object: a v-if or attribute binding on a nested
  // computed ref reads the ref itself (always truthy), never its .value.
  return reactive({
    isFullyExpanded,
    showControl,
    controlLabel,
    isHidden,
    toggle,
  });
}
