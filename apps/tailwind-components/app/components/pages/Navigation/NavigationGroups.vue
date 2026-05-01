<script setup lang="ts">
import NavigationCards from "./NavigationCards.vue";
import type {
  INavigationGroups,
  INavigationCards,
} from "../../../../types/cms";
import type { IPageComponent } from "../../../../types/CmsComponents";

import Button from "../../Button.vue";

const props = withDefaults(
  defineProps<INavigationGroups & { isEditable?: boolean }>(),
  {
    isEditable: false,
  }
);

const linksSorted = props.links?.sort(
  (a: INavigationCards, b: INavigationCards) => {
    return (a.order ?? 0) - (b.order ?? 0);
  }
) as INavigationCards[];

const emit = defineEmits<{
  (e: "edit", component: string, metadata: IPageComponent): void;
}>();
</script>

<template>
  <nav aria-label="Go to page">
    <ul
      :id="id"
      class="w-full m-0 list-none flex justify-center items-center flex-col md:flex-row gap-5"
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
    <slot></slot>
  </nav>
</template>
