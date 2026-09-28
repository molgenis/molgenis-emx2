import { computed, ref, watch, type Ref } from "vue";

export function useOntologyItemPaging(
  itemCount: Ref<number>,
  maxItems: Ref<number | undefined>,
  itemStep: Ref<number>
) {
  const visibleCount = ref(maxItems.value);

  watch(maxItems, (value) => (visibleCount.value = value));

  const isBounded = computed(() => maxItems.value !== undefined);

  const isFullyExpanded = computed(
    () => !isBounded.value || (visibleCount.value ?? 0) >= itemCount.value
  );

  const showControl = computed(() => {
    const bound = maxItems.value;
    return bound !== undefined && itemCount.value > bound;
  });

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

  return {
    isFullyExpanded,
    showControl,
    controlLabel,
    isHidden,
    toggle,
  };
}
