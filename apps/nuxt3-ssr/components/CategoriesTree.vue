<script setup lang="ts">
const { title, collectionEvents, columnCount, category } = withDefaults(
  defineProps<{
    title: string;
    category: string;
    collectionEvents: ICollectionEvent[];
    columnCount?: number;
  }>(),
  {
    columnCount: 2,
  }
);

function collectCategories(type: string, collectionEvents: ICollectionEvent[]) {
  const items = collectionEvents.reduce(
    (
      accumulator: ICollectionEventCategory[],
      currentValue: ICollectionEvent
    ) => {
      // @ts-ignore
      if (Array.isArray(currentValue[type])) {
        // @ts-ignore
        accumulator.push(...currentValue[type]);
      }
      return accumulator;
    },
    []
  );
  return [...new Set(items)];
}

function findParentCategories(categories: ICollectionEventCategory[]) {
  return categories.reduce(
    (
      accumulator: ICollectionEventCategory[],
      currentValue: ICollectionEventCategory
    ) => {
      if (currentValue.parent) {
        if (!accumulator.find(item => item.name == currentValue.parent?.name)) {
          accumulator.push(currentValue.parent);
        }
      } else {
        if (!accumulator.find(item => item.name == currentValue?.name)) {
          accumulator.push(currentValue);
        }
      }
      return accumulator;
    },
    []
  );
}

function combineParentChildCategories(
  categories: ICollectionEventCategory[],
  parents: ICollectionEventCategory[]
): ICollectionEventCategorySet[] {
  return parents.map((item: ICollectionEventCategory) => {
    const children = categories.filter((child: ICollectionEventCategory) => {
      return child?.parent?.name === item?.name;
    });
    const uniqueChildren = [
      ...new Map(children.map(item => [item.name, item])).values(),
    ];
    return {
      name: item.name,
      definition: item.definition,
      children: uniqueChildren,
    };
  });
}

function getCategoriesOf(
  type: string,
  collectionEvents: ICollectionEvent[]
): ICollectionEventCategorySet[] {
  const categories: ICollectionEventCategory[] = collectCategories(
    type,
    collectionEvents
  );
  const parents: ICollectionEventCategory[] = findParentCategories(categories);
  return combineParentChildCategories(categories, parents);
}

let treeItems: ICollectionEventCategorySet[][];
watch(
  collectionEvents,
  () => {
    treeItems = [];
    const allCategories = getCategoriesOf(category, collectionEvents);
    if (allCategories.length > columnCount) {
      const pageSize = allCategories.length / columnCount;
      for (let column = 1; column < columnCount + 1; column++) {
        treeItems.push(
          allCategories.slice((column - 1) * pageSize, column * pageSize)
        );
      }
    } else {
      treeItems.push(allCategories);
    }
  },
  {
    deep: true,
    immediate: true,
  }
);
</script>

<template>
  <ListCollapsible :title="title" :columnCount="columnCount">
    <ul class="text-body-base" v-for="page in treeItems">
      <ListCollapsibleItemParent
        v-for="category in page"
        :title="category.name"
        :count="category.children?.length"
        :tooltip="category.definition">
        <ListCollapsibleItemChild
          v-for="child in category.children"
          :title="child.name"
          :tooltip="child.definition" />
      </ListCollapsibleItemParent>
    </ul>
  </ListCollapsible>
</template>
