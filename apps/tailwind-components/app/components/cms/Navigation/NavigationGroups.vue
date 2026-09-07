<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";

import NavigationCards from "./NavigationCards.vue";
import Button from "../../Button.vue";

import { addComponent, randomId } from "../../../utils/cms.ts";

import type {
  INavigationGroups,
  INavigationCards,
} from "../../../../types/cms";

import type { IPageComponent } from "../../../../types/CmsComponents";

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

const emit = defineEmits(["edit", "delete", "move"]);

async function createNewCard() {
  const cardId = `NavigationCard-${randomId()}`;
  const cardOrder = linksSorted.value?.length + 1 || 0;
  await addComponent(schema, cardId, props.id, cardOrder, "NavigationCards");
  emit("edit");
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
      class="w-full my-2.5 list-none flex justify-center items-center flex-col md:flex-row gap-5"
    >
      <li v-for="card in linksSorted" :key="card.id">
        <NavigationCards
          :id="card.id"
          :title="card.title"
          :description="card.description"
          :url="card.url"
          :url-is-external="card.urlIsExternal"
          :url-label="card.urlLabel"
          :order="card.order"
          class="group w-full md:w-80"
        >
          <Button
            v-if="isEditable"
            class="absolute top-2.5 right-2.5 opacity-0 group-hover:opacity-100 group-focus:opacity-100"
            iconOnly
            icon="edit"
            label="Edit Card"
            type="secondary"
            size="small"
            aria-haspopup="true"
            @click="emit('edit', 'Navigation cards', card)"
          />
        </NavigationCards>
      </li>
    </ul>
    <div class="my-5">
      <button
        class="text-title-contrast flex justify-start items-center gap-1 m-auto"
        @click="createNewCard"
      >
        <BaseIcon name="Plus" :width="18" />
        <span>Add a new navigation card</span>
      </button>
    </div>
    <slot></slot>
  </nav>
</template>
