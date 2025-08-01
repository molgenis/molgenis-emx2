<template>
  <div class="flex flex-col gap-8">
    <div>
      <h3 class="text-title">Refback where source keys is empty</h3>
      <InputRefBack
        id="ref-back-story-item"
        v-model="modelValue"
        :refSchemaId="refSchemaId"
        :refTableId="refTableId"
        :refLabel="refLabel"
        :refBackColumn="refBackColumn"
        :canEdit="canEdit"
      />
    </div>

    <div>
      <h3 class="text-title">Refback where source keys set</h3>
      <InputRefBack
        id="ref-back-story-item"
        v-model="modelValue"
        :refSchemaId="refSchemaId"
        :refTableId="refTableId"
        :refLabel="refLabel"
        :refBackColumn="refBackColumn"
        :refBackPrimaryKey="[
          'ORDER:6fe7a528-2e97-48cc-91e6-a94c689b4919',
          'ORDER:7ztyXho2Qi',
        ]"
        :canEdit="canEdit"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { columnValueObject } from "../../../metadata-utils/src/types";

const refSchemaId = "pet store";
const refTableId = "Order";
const refLabel = "${orderId}";
const canEdit = true;

const refBackColumn = "pet";

const modelValue = defineModel<columnValueObject[]>();

modelValue.value = [
  {
    orderId: "ORDER:6fe7a528-2e97-48cc-91e6-a94c689b4919",
    pet: {
      name: "pooky",
    },
    quantity: "1",
    price: 9.99,
    complete: true,
    status: "delivered",
    mg_insertedBy: "admin",
    mg_insertedOn: "2025-01-23T14:40:09.638434",
    mg_updatedBy: "admin",
    mg_updatedOn: "2025-01-23T14:40:09.638434",
  },
  {
    decimalTest: 0,
    orderId: "ORDER:7ztyXho2Qi",
    pet: {
      name: "pooky",
    },
    quantity: "7",
    price: 898,
    status: "hallo",
    mg_draft: false,
    mg_insertedBy: "admin",
    mg_insertedOn: "2025-02-27T09:24:03.929774",
    mg_updatedBy: "admin",
    mg_updatedOn: "2025-02-27T09:24:03.929774",
  },
];
</script>
