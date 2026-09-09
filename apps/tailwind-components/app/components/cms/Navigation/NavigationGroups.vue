<script setup lang="ts">
import { ref, computed } from "vue";
import { useRoute } from "vue-router";

import NavigationGroupItem from "./NavigationGroupItem.vue";
import NoResultsMessage from "../../text/NoResultsMessage.vue";

import { addComponent, randomId } from "../../../utils/cms.ts";

import type {
  INavigationGroups,
  INavigationCards,
} from "../../../../types/cms";

const props = withDefaults(
  defineProps<INavigationGroups & { isEditable?: boolean }>(),
  {
    isEditable: false,
  }
);

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? (route.params.schema[0] as string)
  : route.params.schema ?? "";

const linksSorted = computed<INavigationCards[]>(() => {
  return props.links?.sort((a: INavigationCards, b: INavigationCards) => {
    return (a.order ?? 0) - (b.order ?? 0);
  }) as INavigationCards[];
});

const emit = defineEmits(["edit", "delete", "move", "updatePage"]);

async function createNewCard() {
  const cardId = `NavigationCard-${randomId()}`;
  const cardOrder = linksSorted.value?.length + 1 || 0;
  await addComponent(schema, cardId, props.id, cardOrder, "NavigationCards");
  emit("updatePage");
}
</script>

<template>
  <nav
    aria-label="Go to page"
    :class="{
      'border p-7.5 px-2.5': !linksSorted,
    }"
  >
    <ul
      v-if="linksSorted"
      :id="id"
      class="w-full my-2.5 list-none grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 justify-center items-center gap-2.5 lg:gap-5"
    >
      <li v-for="card in linksSorted" :key="card.id">
        <NavigationGroupItem
          :id="card.id"
          :title="card.title"
          :description="card.description"
          :url="card.url"
          :urlIsExternal="card.urlIsExternal"
          :urlLabel="card.urlLabel"
          :order="card.order"
          :isEditable="isEditable"
          @edit="$emit('edit')"
        />
      </li>
    </ul>
    <div class="text-center" v-else>
      <NoResultsMessage
        label="No Navigation Cards found. Click the button below to create a new one"
      />
    </div>
    <div class="my-5">
      <button
        class="text-title-contrast flex justify-start items-center gap-1 m-auto"
        @click="createNewCard"
      >
        <BaseIcon name="Plus" :width="18" />
        <span>Add Navigation Card</span>
      </button>
    </div>
    <slot></slot>
  </nav>
</template>
