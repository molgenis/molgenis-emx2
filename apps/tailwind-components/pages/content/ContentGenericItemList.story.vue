<template>
  <div class="flex">
    <div class="flex-1">
      <div>
        <ContentGenericItemList>
          <ContentGenericItem
            :field="{
              value: 'String value',
              meta: { columnType: 'STRING', label: 'String' },
            }"
          />
        </ContentGenericItemList>
      </div>
    </div>
  </div>
</template>
