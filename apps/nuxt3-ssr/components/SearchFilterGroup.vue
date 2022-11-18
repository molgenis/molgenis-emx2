<script setup>
import { ref } from "vue";

defineProps({
  title: {
    type: String,
  },
  json: {
    type: Object,
  },
});

let collapsedTitle = ref(true);
const toggleCollapseTitle = () => {
  collapsedTitle.value = !collapsedTitle.value;
};

let collapsed = ref(true);
const toggleCollapse = () => {
  collapsed.value = !collapsed.value;
};
</script>

<template>
  <hr class="mx-5 border-black opacity-10" />

  <div class="flex gap-1 p-5 items-center">
    <div class="inline-flex gap-1 group" @click="toggleCollapseTitle()">
      <h3
        class="
          text-white
          font-sans
          text-body-base
          font-bold
          mr-[5px]
          group-hover:underline group-hover:cursor-pointer
        "
      >
        {{ title }}
      </h3>
      <span
        :class="{ 'rotate-180': collapsedTitle }"
        class="
          rounded-full
          group-hover:bg-blue-800 group-hover:cursor-pointer
          w-8
          h-8
          text-white
          flex
          items-center
          justify-center
        "
      >
        <BaseIcon name="caret-up" width="26" />
      </span>
    </div>
    <div class="grow text-right">
      <span
        class="
          text-body-sm text-yellow-500
          hover:underline hover:cursor-pointer
        "
        >Remove 2 selected
      </span>
    </div>
  </div>

  <ul class="ml-5 mb-5 text-white" :class="{ hidden: collapsedTitle }">
    <li v-for="item in json" :key="item.code" class="mb-2.5">
      <div class="flex items-start">
        <span
          @click="toggleCollapse()"
          :class="{ 'rotate-180': collapsed }"
          class="
            text-white
            rounded-full
            hover:bg-blue-800 hover:cursor-pointer
            h-6
            w-6
            flex
            items-center
            justify-center
          "
        >
          <BaseIcon name="caret-up" width="20" />
        </span>
        <div class="flex items-center">
          <input
            type="checkbox"
            :id="item.code"
            :name="item.code"
            class="
              w-5
              h-5
              rounded-3px
              ml-[6px]
              mr-2.5
              mt-0.5
              text-yellow-500
              border-0
            "
          />
        </div>
        <label :for="item.code" class="hover:cursor-pointer text-body-sm group">
          <span class="group-hover:underline">{{ item.name }}</span>
          <div class="whitespace-nowrap inline-flex items-center">
            <span
              class="
                text-blue-200
                inline-block
                mr-2
                group-hover:underline
                decoration-blue-200
                fill-black
              "
              :hoverColor="white"
              >&nbsp;- 34
            </span>
            <div class="inline-block">
              <CustomTooltip
                label="Lees meer"
                hoverColor="white"
                content="tooltip"
              />
            </div>
          </div>
        </label>
      </div>

      <ul class="ml-[39px]" :class="{ hidden: collapsed }" v-if="item.children">
        <SearchFilterGroupChild :data="item.children" />
      </ul>
    </li>
  </ul>
</template>
