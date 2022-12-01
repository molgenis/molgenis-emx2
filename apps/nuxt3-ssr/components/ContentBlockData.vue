<script setup lang="ts">
const { collectionEvents, useFlatCategories } = withDefaults(
  defineProps<{
    title: string;
    description: string;
    collectionEvents: ICollectionEvent[];
    useFlatCategories?: boolean;
  }>(),
  {
    useFlatCategories: false,
  }
);

function getFlatCategoriesOf(
  type: "dataCategories" | "sampleCategories" | "areasOfInformation"
) {
  const items = collectionEvents.reduce(
    (accumulator: string[], currentValue: ICollectionEvent) => {
      if (Array.isArray(currentValue[type])) {
        accumulator.push(
          ...currentValue[type].map((item: INameObject) => item.name)
        );
      }
      return accumulator;
    },
    []
  );
  return [...new Set(items)];
}

function collectCategories(
  type: "dataCategories" | "sampleCategories" | "areasOfInformation"
) {
  const items = collectionEvents.reduce(
    (
      accumulator: ICollectionEventCategory[],
      currentValue: ICollectionEvent
    ) => {
      if (Array.isArray(currentValue[type])) {
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
        if (
          !accumulator.find((item) => item.name == currentValue.parent?.name)
        ) {
          accumulator.push(currentValue.parent);
        }
      } else {
        if (!accumulator.find((item) => item.name == currentValue?.name)) {
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
      ...new Map(children.map((item) => [item.name, item])).values(),
    ];
    return {
      name: item.name,
      definition: item.definition,
      children: uniqueChildren,
    };
  });
}

function getCategoriesOf(
  type: "dataCategories" | "sampleCategories" | "areasOfInformation",
  pageIndex: number,
  pages: number
): ICollectionEventCategorySet[] {
  const categories: ICollectionEventCategory[] = collectCategories(type);
  const parents: ICollectionEventCategory[] = findParentCategories(categories);
  const pageSize = parents.length / pages;
  return combineParentChildCategories(categories, parents).slice(
    (pageIndex - 1) * pageSize,
    pageIndex * pageSize
  );
}
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <div v-if="useFlatCategories" class="grid gap-[45px] mt-7.5">
      <List title="Data categories" :columnCount="2">
        <ListItem v-for="category in getFlatCategoriesOf('dataCategories')">{{
          category
        }}</ListItem>
      </List>
      <List title="Sample categories" :columnCount="3">
        <ListItem v-for="category in getFlatCategoriesOf('sampleCategories')">{{
          category
        }}</ListItem>
      </List>
      <List title="Areas of informations" :columnCount="2">
        <ListItem
          v-for="category in getFlatCategoriesOf('areasOfInformation')"
          >{{ category }}</ListItem
        >
      </List>
    </div>
    <div v-else class="grid gap-[45px] mt-7.5">
      <ListCollapsible title="Data categories" :columnCount="2">
        <ul class="text-body-base">
          <ListCollapsibleItemParent
            v-for="category in getCategoriesOf('dataCategories', 1, 2)"
            :title="category.name"
            :count="category.children?.length"
            :tooltip="category.definition"
          >
            <ListCollapsibleItemChild
              v-for="child in category.children"
              :title="child.name"
              :tooltip="child.definition"
            />
          </ListCollapsibleItemParent>
        </ul>
        <ul class="text-body-base">
          <ListCollapsibleItemParent
            v-for="category in getCategoriesOf('dataCategories', 2, 2)"
            :title="category.name"
            :count="category.children?.length"
            :tooltip="category.definition"
          >
            <ListCollapsibleItemChild
              v-for="child in category.children"
              :title="child.name"
              :tooltip="child.definition"
            />
          </ListCollapsibleItemParent>
        </ul>
      </ListCollapsible>
      <ListCollapsible title="Sample categories" :columnCount="3">
        <ul class="text-body-base">
          <ListCollapsibleItemParent
            v-for="category in getCategoriesOf('sampleCategories', 1, 3)"
            :title="category.name"
            :count="category.children?.length"
            :tooltip="category.definition"
          >
            <ListCollapsibleItemChild
              v-for="child in category.children"
              :title="child.name"
              :tooltip="child.definition"
            />
          </ListCollapsibleItemParent>
        </ul>
        <ul class="text-body-base">
          <ListCollapsibleItemParent
            v-for="category in getCategoriesOf('sampleCategories', 2, 3)"
            :title="category.name"
            :count="category.children?.length"
            :tooltip="category.definition"
          >
            <ListCollapsibleItemChild
              v-for="child in category.children"
              :title="child.name"
              :tooltip="child.definition"
            />
          </ListCollapsibleItemParent>
        </ul>
        <ul class="text-body-base">
          <ListCollapsibleItemParent
            v-for="category in getCategoriesOf('sampleCategories', 3, 3)"
            :title="category.name"
            :count="category.children?.length"
            :tooltip="category.definition"
          >
            <ListCollapsibleItemChild
              v-for="child in category.children"
              :title="child.name"
              :tooltip="child.definition"
            />
          </ListCollapsibleItemParent>
        </ul>
      </ListCollapsible>
      <ListCollapsible title="Areas of informations" :columnCount="2">
        <ul class="text-body-base">
          <ListCollapsibleItemParent
            v-for="category in getCategoriesOf('areasOfInformation', 1, 2)"
            :title="category.name"
            :count="category.children?.length"
            :tooltip="category.definition"
          >
            <ListCollapsibleItemChild
              v-for="child in category.children"
              :title="child.name"
              :tooltip="child.definition"
            />
          </ListCollapsibleItemParent>
        </ul>
        <ul class="text-body-base">
          <ListCollapsibleItemParent
            v-for="category in getCategoriesOf('areasOfInformation', 2, 2)"
            :title="category.name"
            :count="category.children?.length"
            :tooltip="category.definition"
          >
            <ListCollapsibleItemChild
              v-for="child in category.children"
              :title="child.name"
              :tooltip="child.definition"
            />
          </ListCollapsibleItemParent>
        </ul>
      </ListCollapsible>
    </div>

    <!--
        *
        *   Comment to clarify the list/column structure of the <ListCollapsible> component.
        *
        *   <ListCollapsible title="" columnCount="3">
        *     <ul class="text-body-base" v-for>  <== When columnCount="3" is defined you will need 3 <ul class="text-body-base"> elements to create 3 columns.
        *       <ListCollapsibleItemParent>
        *       <ListCollapsibleItemParent />
        *     </ul>
        *   <ListCollapsible />
        *
      -->
    <!--
    <div class="grid gap-[45px] mt-7.5">
      <ListCollapsible title="Areas of informations tree view" :columnCount="2">
        <ul class="text-body-base">
          <ListCollapsibleItemParent
            title="Administrative information"
            :count="6"
            tooltip="Tooltip text"
          >
            <ListCollapsibleItemChild title="Biochemistry" />
            <ListCollapsibleItemChild title="Genomics" />
            <ListCollapsibleItemChild title="Hermatology" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text"
            />
            <ListCollapsibleItemChild title="Immunology" />
            <ListCollapsibleItemChild title="Microbiology" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text"
            />
            <ListCollapsibleItemChild title="Virology" tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Other" />
          </ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Birth, pregnancy and reproductive health history"
            :count="5"
            tooltip="Tooltip text"
          >
            <ListCollapsibleItemChild title="Biochemistry" />
            <ListCollapsibleItemChild title="Genomics" />
            <ListCollapsibleItemChild title="Hermatology" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Immunology" />
            <ListCollapsibleItemChild title="Microbiology" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Virology" tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Other"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Cognition, personality and psychological measures measures and assessments"
            :count="4"
            tooltip="Tooltip text"
          >
            <ListCollapsibleItemChild title="Biochemistry" />
            <ListCollapsibleItemChild title="Genomics" />
            <ListCollapsibleItemChild title="Hermatology" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Immunology" />
            <ListCollapsibleItemChild title="Microbiology" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Virology" tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Other"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Death"
            :count="3"
            tooltip="Tooltip text"
          >
            <ListCollapsibleItemChild title="Biochemistry" />
            <ListCollapsibleItemChild title="Genomics" />
            <ListCollapsibleItemChild title="Hermatology" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Immunology" />
            <ListCollapsibleItemChild title="Microbiology" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Virology" tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Other"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Health and community care services utilization"
            :count="4"
            tooltip="Tooltip text"
          >
            <ListCollapsibleItemChild title="Biochemistry" />
            <ListCollapsibleItemChild title="Genomics" />
            <ListCollapsibleItemChild title="Hermatology" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Immunology" />
            <ListCollapsibleItemChild title="Microbiology" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Virology" tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Other"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Laboratory measures"
            :count="9"
            tooltip="Tooltip text"
          >
            <ListCollapsibleItemChild title="Biochemistry" />
            <ListCollapsibleItemChild title="Genomics" />
            <ListCollapsibleItemChild title="Hermatology" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Immunology" />
            <ListCollapsibleItemChild title="Microbiology" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Virology" tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Other"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Life events, life plans, beliefs and values"
            :count="9"
            tooltip="Tooltip text"
          >
            <ListCollapsibleItemChild title="Biochemistry" />
            <ListCollapsibleItemChild title="Genomics" />
            <ListCollapsibleItemChild title="Hermatology" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Immunology" />
            <ListCollapsibleItemChild title="Microbiology" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Virology" tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Other"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Lifestyle and behaviours"
            :count="9"
            tooltip="Tooltip text"
          >
            <ListCollapsibleItemChild title="Biochemistry" />
            <ListCollapsibleItemChild title="Genomics" />
            <ListCollapsibleItemChild title="Hermatology" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Immunology" />
            <ListCollapsibleItemChild title="Microbiology" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Virology" tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Other"
          /></ListCollapsibleItemParent>
        </ul>
        <ul class="text-body-base">
          <ListCollapsibleItemParent
            title="Administrative information"
            :count="9"
            tooltip="Tooltip text"
          >
            <ListCollapsibleItemChild title="Biochemistry" />
            <ListCollapsibleItemChild title="Genomics" />
            <ListCollapsibleItemChild title="Hermatology" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text"
            />
            <ListCollapsibleItemChild title="Immunology" />
            <ListCollapsibleItemChild title="Microbiology" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text"
            />
            <ListCollapsibleItemChild title="Virology" tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Other" />
          </ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Birth, pregnancy and reproductive health history"
            :count="9"
            tooltip="Tooltip text"
          >
            <ListCollapsibleItemChild title="Biochemistry" />
            <ListCollapsibleItemChild title="Genomics" />
            <ListCollapsibleItemChild title="Hermatology" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Immunology" />
            <ListCollapsibleItemChild title="Microbiology" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Virology" tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Other"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Cognition, personality and psychological measures measures and assessments"
            :count="9"
            tooltip="Tooltip text"
          >
            <ListCollapsibleItemChild title="Biochemistry" />
            <ListCollapsibleItemChild title="Genomics" />
            <ListCollapsibleItemChild title="Hermatology" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Immunology" />
            <ListCollapsibleItemChild title="Microbiology" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Virology" tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Other"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Death"
            :count="9"
            tooltip="Tooltip text"
          >
            <ListCollapsibleItemChild title="Biochemistry" />
            <ListCollapsibleItemChild title="Genomics" />
            <ListCollapsibleItemChild title="Hermatology" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Immunology" />
            <ListCollapsibleItemChild title="Microbiology" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Virology" tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Other"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Health and community care services utilization"
            :count="9"
            tooltip="Tooltip text"
          >
            <ListCollapsibleItemChild title="Biochemistry" />
            <ListCollapsibleItemChild title="Genomics" />
            <ListCollapsibleItemChild title="Hermatology" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Immunology" />
            <ListCollapsibleItemChild title="Microbiology" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Virology" tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Other"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Laboratory measures"
            :count="9"
            tooltip="Tooltip text"
          >
            <ListCollapsibleItemChild title="Biochemistry" />
            <ListCollapsibleItemChild title="Genomics" />
            <ListCollapsibleItemChild title="Hermatology" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Immunology" />
            <ListCollapsibleItemChild title="Microbiology" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Virology" tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Other"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Life events, life plans, beliefs and values"
            :count="9"
            tooltip="Tooltip text"
          >
            <ListCollapsibleItemChild title="Biochemistry" />
            <ListCollapsibleItemChild title="Genomics" />
            <ListCollapsibleItemChild title="Hermatology" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Immunology" />
            <ListCollapsibleItemChild title="Microbiology" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Virology" tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Other"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Lifestyle and behaviours"
            :count="9"
            tooltip="Tooltip text"
          >
            <ListCollapsibleItemChild title="Biochemistry" />
            <ListCollapsibleItemChild title="Genomics" />
            <ListCollapsibleItemChild title="Hermatology" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Immunology" />
            <ListCollapsibleItemChild title="Microbiology" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Virology" tooltip="Tooltip text" />
            <ListCollapsibleItemChild title="Other"
          /></ListCollapsibleItemParent>
        </ul>
      </ListCollapsible>
      <ListCollapsible
        title="Areas of informations tree view links"
        :columnCount="2"
      >
        <ul class="text-body-base">
          <ListCollapsibleItemParent
            title="Administrative information"
            :count="9"
            tooltip="Tooltip text"
            url="#"
          >
            <ListCollapsibleItemChild title="Biochemistry" url="#" />
            <ListCollapsibleItemChild title="Genomics" url="#" />
            <ListCollapsibleItemChild title="Hermatology" url="#" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text"
              url="#"
            />
            <ListCollapsibleItemChild title="Immunology" url="#" />
            <ListCollapsibleItemChild title="Microbiology" url="#" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text"
              url="#"
            />
            <ListCollapsibleItemChild
              title="Virology"
              tooltip="Tooltip text"
              url="#"
            />
            <ListCollapsibleItemChild title="Other" url="#" />
          </ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Birth, pregnancy and reproductive health history"
            :count="6"
            tooltip="Tooltip text"
            url="#"
          >
            <ListCollapsibleItemChild title="Biochemistry" url="#" />
            <ListCollapsibleItemChild title="Genomics" url="#" />
            <ListCollapsibleItemChild title="Hermatology" url="#" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Immunology" url="#" />
            <ListCollapsibleItemChild title="Microbiology" url="#" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild
              title="Virology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Other" url="#"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Cognition, personality and psychological measures measures and assessments"
            :count="4"
            tooltip="Tooltip text"
            url="#"
          >
            <ListCollapsibleItemChild title="Biochemistry" url="#" />
            <ListCollapsibleItemChild title="Genomics" url="#" />
            <ListCollapsibleItemChild title="Hermatology" url="#" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Immunology" url="#" />
            <ListCollapsibleItemChild title="Microbiology" url="#" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild
              title="Virology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Other" url="#"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Death"
            :count="2"
            tooltip="Tooltip text"
            url="#"
          >
            <ListCollapsibleItemChild title="Biochemistry" url="#" />
            <ListCollapsibleItemChild title="Genomics" url="#" />
            <ListCollapsibleItemChild title="Hermatology" url="#" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Immunology" url="#" />
            <ListCollapsibleItemChild title="Microbiology" url="#" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild
              title="Virology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Other" url="#"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Health and community care services utilization"
            :count="7"
            tooltip="Tooltip text"
            url="#"
          >
            <ListCollapsibleItemChild title="Biochemistry" url="#" />
            <ListCollapsibleItemChild title="Genomics" url="#" />
            <ListCollapsibleItemChild title="Hermatology" url="#" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Immunology" url="#" />
            <ListCollapsibleItemChild title="Microbiology" url="#" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild
              title="Virology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Other" url="#"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Laboratory measures"
            :count="4"
            tooltip="Tooltip text"
            url="#"
          >
            <ListCollapsibleItemChild title="Biochemistry" url="#" />
            <ListCollapsibleItemChild title="Genomics" url="#" />
            <ListCollapsibleItemChild title="Hermatology" url="#" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Immunology" url="#" />
            <ListCollapsibleItemChild title="Microbiology" url="#" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild
              title="Virology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Other" url="#"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Life events, life plans, beliefs and values"
            :count="6"
            tooltip="Tooltip text"
            url="#"
          >
            <ListCollapsibleItemChild title="Biochemistry" url="#" />
            <ListCollapsibleItemChild title="Genomics" url="#" />
            <ListCollapsibleItemChild title="Hermatology" url="#" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Immunology" url="#" />
            <ListCollapsibleItemChild title="Microbiology" url="#" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild
              title="Virology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Other" url="#"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Lifestyle and behaviours"
            :count="6"
            tooltip="Tooltip text"
            url="#"
          >
            <ListCollapsibleItemChild title="Biochemistry" />
            <ListCollapsibleItemChild title="Genomics" />
            <ListCollapsibleItemChild title="Hermatology" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Immunology" />
            <ListCollapsibleItemChild title="Microbiology" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild
              title="Virology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Other"
          /></ListCollapsibleItemParent>
        </ul>
        <ul class="text-body-base">
          <ListCollapsibleItemParent
            title="Medication and supplements"
            :count="6"
            tooltip="Tooltip text"
            url="#"
          >
            <ListCollapsibleItemChild title="Biochemistry" url="#" />
            <ListCollapsibleItemChild title="Genomics" url="#" />
            <ListCollapsibleItemChild title="Hermatology" url="#" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text"
              url="#"
            />
            <ListCollapsibleItemChild title="Immunology" url="#" />
            <ListCollapsibleItemChild title="Microbiology" url="#" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text"
              url="#"
            />
            <ListCollapsibleItemChild
              title="Virology"
              tooltip="Tooltip text"
              url="#"
            />
            <ListCollapsibleItemChild title="Other" url="#" />
          </ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Non-pharmacological interventions"
            :count="6"
            tooltip="Tooltip text"
            url="#"
          >
            <ListCollapsibleItemChild title="Biochemistry" url="#" />
            <ListCollapsibleItemChild title="Genomics" url="#" />
            <ListCollapsibleItemChild title="Hermatology" url="#" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Immunology" url="#" />
            <ListCollapsibleItemChild title="Microbiology" url="#" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild
              title="Virology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Other" url="#"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Perception of health, guality of life, development and functional limitations"
            :count="6"
            tooltip="Tooltip text"
            url="#"
          >
            <ListCollapsibleItemChild title="Biochemistry" url="#" />
            <ListCollapsibleItemChild title="Genomics" url="#" />
            <ListCollapsibleItemChild title="Hermatology" url="#" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Immunology" url="#" />
            <ListCollapsibleItemChild title="Microbiology" url="#" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild
              title="Virology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Other" url="#"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Physical environment"
            :count="6"
            tooltip="Tooltip text"
            url="#"
          >
            <ListCollapsibleItemChild title="Biochemistry" url="#" />
            <ListCollapsibleItemChild title="Genomics" url="#" />
            <ListCollapsibleItemChild title="Hermatology" url="#" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Immunology" url="#" />
            <ListCollapsibleItemChild title="Microbiology" url="#" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild
              title="Virology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Other" url="#"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Physical measures and assessment"
            :count="6"
            tooltip="Tooltip text"
            url="#"
          >
            <ListCollapsibleItemChild title="Biochemistry" url="#" />
            <ListCollapsibleItemChild title="Genomics" url="#" />
            <ListCollapsibleItemChild title="Hermatology" url="#" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Immunology" url="#" />
            <ListCollapsibleItemChild title="Microbiology" url="#" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild
              title="Virology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Other" url="#"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Preschool, school and worklife"
            :count="6"
            tooltip="Tooltip text"
            url="#"
          >
            <ListCollapsibleItemChild title="Biochemistry" url="#" />
            <ListCollapsibleItemChild title="Genomics" url="#" />
            <ListCollapsibleItemChild title="Hermatology" url="#" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Immunology" url="#" />
            <ListCollapsibleItemChild title="Microbiology" url="#" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild
              title="Virology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Other" url="#"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Social environment and relationships"
            :count="6"
            tooltip="Tooltip text"
            url="#"
          >
            <ListCollapsibleItemChild title="Biochemistry" url="#" />
            <ListCollapsibleItemChild title="Genomics" url="#" />
            <ListCollapsibleItemChild title="Hermatology" url="#" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Immunology" url="#" />
            <ListCollapsibleItemChild title="Microbiology" url="#" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild
              title="Virology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Other" url="#"
          /></ListCollapsibleItemParent>
          <ListCollapsibleItemParent
            title="Socio-demographic and economic characteristics"
            :count="6"
            tooltip="Tooltip text"
            url="#"
          >
            <ListCollapsibleItemChild title="Biochemistry" />
            <ListCollapsibleItemChild title="Genomics" />
            <ListCollapsibleItemChild title="Hermatology" />
            <ListCollapsibleItemChild
              title="Histology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Immunology" />
            <ListCollapsibleItemChild title="Microbiology" />
            <ListCollapsibleItemChild
              title="Toxicology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild
              title="Virology"
              tooltip="Tooltip text"
              url="#" />
            <ListCollapsibleItemChild title="Other"
          /></ListCollapsibleItemParent>
        </ul>
      </ListCollapsible>
    </div>
    -->
  </ContentBlock>
</template>
