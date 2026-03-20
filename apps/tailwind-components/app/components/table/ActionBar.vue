<template>
  <div
    ref="root"
    class="flex items-center w-full min-w-0 justify-end"
    :class="{ 'gap-2': overflowItems.length }"
  >
    <div ref="visibleContainer" class="flex gap-2">
      <template v-for="(child, i) in visibleItems" :key="i">
        <component :is="child" />
      </template>
    </div>

    <div class="relative flex-shrink-0">
      <ButtonDropdown
        v-if="overflowItems.length"
        type="outline"
        icon="MoreVert"
        :icon-only="true"
        label="more table actions"
        placement="bottom-end"
      >
        <section
          class="flex flex-col gap-2 bg-form p-4 w-[200px] z-50 border rounded shadow mt-1"
        >
          <div v-for="(child, i) in overflowItems" :key="i">
            <component :is="child" />
          </div>
        </section>
      </ButtonDropdown>
    </div>

    <div
      ref="measure"
      class="absolute invisible pointer-events-none whitespace-nowrap"
    >
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, useSlots, onUpdated, type VNode } from "vue";
import ButtonDropdown from "../button/Dropdown.vue";

const root = ref<HTMLElement | undefined>();
const measure = ref<HTMLElement | undefined>();

const visibleItems = ref<VNode[]>([]);
const overflowItems = ref<VNode[]>([]);

const slots = useSlots();

function calculate() {
  const rootWidth = root.value?.offsetWidth ?? 0;
  const gap = 16;
  const globalOffsetWidth = 100;

  const children: HTMLElement[] = measure.value
    ? (Array.from(measure.value.children) as HTMLElement[])
    : [];

  let used = 0;
  const visible: VNode[] = [];
  const overflow: VNode[] = [];
  const slotNodes = slots.default?.() ?? [];

  children.forEach((el, i) => {
    const width = el.offsetWidth;

    const item = slotNodes[i];
    if (!item) return;

    if (
      used + width <
      rootWidth - (overflow.length ? 32 : 0) - globalOffsetWidth
    ) {
      visible.push(item);
      used += width + gap;
    } else {
      overflow.push(item);
    }
  });

  visibleItems.value = visible;
  overflowItems.value = overflow;
}

onMounted(async () => {
  await nextTick();

  requestAnimationFrame(() => {
    calculate();
  });

  const observer = new ResizeObserver(() => {
    calculate();
  });

  if (root.value) {
    observer.observe(root.value);
  }
});

onUpdated(() => {
  requestAnimationFrame(() => {
    calculate();
  });
});
</script>
