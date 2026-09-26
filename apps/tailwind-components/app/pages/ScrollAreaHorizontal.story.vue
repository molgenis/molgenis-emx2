<template>
  <div class="flex flex-col gap-8">
    <h4 class="text-lg">Scrollable: both edges fade while content is hidden</h4>
    <ScrollAreaHorizontal
      class="bg-table border border-theme border-color-theme rounded-base"
      fade-color="var(--background-color-table)"
    >
      <div class="flex gap-4 p-4 w-max">
        <div
          v-for="index in 20"
          :key="index"
          class="w-40 shrink-0 p-4 text-table-row"
        >
          Column {{ index }}
        </div>
      </div>
    </ScrollAreaHorizontal>

    <h4 class="text-lg">startInset=48: the pinned column stays opaque</h4>
    <ScrollAreaHorizontal
      class="bg-table border border-theme border-color-theme rounded-base"
      :startInset="48"
    >
      <div class="flex gap-4 p-4 w-max">
        <div class="sticky left-0 w-12 shrink-0 bg-table text-table-row">
          pin
        </div>
        <div
          v-for="index in 20"
          :key="index"
          class="w-40 shrink-0 p-4 text-table-row"
        >
          Column {{ index }}
        </div>
      </div>
    </ScrollAreaHorizontal>

    <h4 class="text-lg">Content that fits: no fade</h4>
    <ScrollAreaHorizontal
      class="bg-table border border-theme border-color-theme rounded-base"
    >
      <div class="p-4 text-table-row">Nothing is hidden here.</div>
    </ScrollAreaHorizontal>
  </div>
</template>
