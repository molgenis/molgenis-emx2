<script setup lang="ts">
defineProps<{
  columns: any[];
  rows: any[];
}>();

let scrollXAtEnd = ref(false);
let scrollYAtEnd = ref(false);
let scrollElement = ref();

function scroll(event: Event) {
  let target = event.target as HTMLInputElement;
  scrollXAtEnd.value =
    target.scrollTop + target.offsetHeight >= target.scrollHeight;
  scrollYAtEnd.value =
    target.scrollLeft + target.offsetWidth >= target.scrollWidth;
}

function calculateVerticalBarHeight(event: MouseEvent) {
  if (event.target === null) {
    return;
  } else {
    const td = (event.target as HTMLElement).closest("td");
    const table = (event.target as HTMLElement).closest("table");
    const bar = td?.querySelector(".vertical-hover-bar") as HTMLElement;

    if (!td || !table || !bar) {
      throw new Error(
        "Could not find expected elements in dom, expected td, table and bar"
      );
    }

    const offset = -(
      table.offsetHeight -
      (td.getBoundingClientRect().top - table.getBoundingClientRect().top) -
      td.offsetHeight
    );

    bar.style.bottom = Math.floor(offset) + "px";
  }
}
</script>

<template>
  <div
    role="region relative overflow-hidden"
    ref="scrollElement"
    @scroll.passive="scroll"
  >
    <table class="table-auto relative z-0">
      <thead>
        <tr>
          <th
            class="sticky left-0 top-0 z-30 bg-white max-w-title max-h-title overflow-hidden"
          >
            <div
              class="absolute inset-0 border-r border-b pointer-events-none"
            ></div>
          </th>
          <th
            v-for="(column, index) of columns"
            :key="`head-${index}`"
            class="sticky top-0 z-20 min-w-[2rem] bg-white text-center align-bottom"
          >
            <div
              class="absolute inset-0 border-r border-b pointer-events-none"
            ></div>
            <div
              class="max-h-title text-left min-h-title overflow-hidden hover:overflow-visible"
            >
              <slot name="column" :value="column">{{ column }}</slot>
            </div>
          </th>
        </tr>
      </thead>
      <tbody class="relative z-0">
        <tr
          v-for="(row, rowIndex) of rows"
          :key="`tr-${rowIndex}`"
          class="text-left hover:bg-gray-100"
        >
          <th class="sticky left-0 z-10 bg-white whitespace-nowrap max-w-title">
            <div class="absolute inset-0 border-b pointer-events-none"></div>
            <slot name="row" :value="{ row, rowIndex }">{{ row }}</slot>
          </th>
          <td
            class="relative whitespace-nowrap border-b"
            v-for="(column, columnIndex) of columns"
            :key="`td-${columnIndex}`"
            @mouseover="calculateVerticalBarHeight($event)"
          >
            <div class="vertical-hover-bar"></div>
            <div class="z-10">
              <slot name="cell" :value="{ row, column, rowIndex, columnIndex }">
              </slot>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <div
      v-if="!scrollYAtEnd"
      class="flex items-center justify-items-end absolute z-100 right-0 inset-y-0 w-10 bg-gradient-to-r from-[rgba(255,255,255,0)] to-white pointer-events-none transition-opacity"
    ></div>
    <div
      v-if="!scrollXAtEnd"
      class="flex items-center justify-items-end absolute z-100 bottom-0 inset-x-0 h-10 bg-gradient-to-b from-[rgba(255,255,255,0)] to-white pointer-events-none transition-opacity"
    ></div>
  </div>
</template>

<style scoped>
table.table-auto tbody tr:hover th {
  background-color: rgb(244, 244, 244);
}
td:hover .vertical-hover-bar {
  display: block;
}

.vertical-hover-bar {
  z-index: -1;
  position: absolute;
  pointer-events: none;
  left: 0;
  right: 0;
  top: -99999px;
  bottom: -99999px;
  background-color: rgb(244, 244, 244);
  border: 1px solid rgb(226, 226, 226);
  display: none;
}

th:first-child::before {
  z-index: -1;
  content: "";
  position: absolute;
  pointer-events: none;
  top: 0;
  bottom: 0;
  width: 1rem;
  right: -1rem;
  background-image: linear-gradient(to right, rgba(0, 0, 0, 0.1), transparent);
}
</style>
