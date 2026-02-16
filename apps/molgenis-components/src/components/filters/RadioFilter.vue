<template>
  <InputRadio
    v-if="options?.length"
    :id="id"
    :modelValue="conditionLabel"
    :tableId="tableId"
    :schemaId="schemaId"
    :refLabel="refLabel"
    :options="options"
    @update:modelValue="onUpdateCondition"
  />
</template>

<script setup lang="ts">
import { ref } from "vue";
import Client, { convertRowToPrimaryKey } from "../../client/client";
import InputRadio from "../forms/InputRadio.vue";
import { IQueryMetaData } from "../../../../metadata-utils/src/IQueryMetaData";
import { IRow } from "../../Interfaces/IRow";
import { applyJsTemplate } from "../utils";

const { schemaId, tableId, refLabel, orderBy, filter, condition } =
  defineProps<{
    id: string;
    condition?: Record<string, any>;
    tableId: string;
    schemaId: string;
    refLabel: string;
    orderBy?: Record<string, string>;
    filter?: Object;
  }>();

const emit = defineEmits(["updateCondition", "clearCondition"]);
const client = Client.newClient(schemaId);

const options = ref();
const keysByLabel = ref<Record<string, any>>({});
const conditionLabel = ref(applyJsTemplate(condition || {}, refLabel));

loadOptions();

function onUpdateCondition(newValue: string) {
  if (newValue === null) {
    emit("clearCondition", newValue);
  } else {
    emit("updateCondition", keysByLabel.value[newValue]);
  }
}

async function loadOptions() {
  const queryOptions: IQueryMetaData = {
    orderby: orderBy,
    filter: filter,
  };
  const response = await client.fetchTableData(tableId, queryOptions);
  const rows = response[tableId] || [];
  const keyPromises = rows.map(async (row: IRow) =>
    convertRowToPrimaryKey(row, tableId, schemaId)
  );
  const keys = await Promise.all(keyPromises);

  keysByLabel.value = keys.reduce((accum: Record<string, any>, key: any) => {
    const label = applyJsTemplate(key, refLabel);
    accum[label] = key;
    return accum;
  }, {});

  options.value = Object.keys(keysByLabel.value).sort();
}
</script>
