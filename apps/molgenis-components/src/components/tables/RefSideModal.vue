<template>
  <SideModal :label="label" :isVisible="isVisible" @onClose="emit('onClose')">
    <MessageError v-if="errorMessage">{{ errorMessage }}</MessageError>

    <div v-for="queryResult in queryResults">
      <h5 v-if="queryResult?.name">{{ queryResult.name }}</h5>

      <table class="table table-sm">
        <tr v-for="(value, key) in filteredResults(queryResult)">
          <td class="key border-right">{{ key }}</td>
          <td class="value">{{ value }}</td>
        </tr>
      </table>

      <small class="text-black-50" v-if="showDataOwner">
        <div v-if="queryResult?.mg_insertedBy">
          Inserted by '{{ queryResult?.mg_insertedBy }}'
          <span v-if="queryResult?.mg_insertedOn">
            On {{ new Date(queryResult?.mg_insertedOn).toLocaleString() }}
          </span>
        </div>
        <div v-if="queryResult?.mg_updatedBy">
          Updated by '{{ queryResult?.mg_updatedBy }}'
          <span v-if="queryResult?.mg_updatedOn">
            On {{ new Date(queryResult?.mg_updatedOn).toLocaleString() }}
          </span>
        </div>
      </small>
      <div v-if="queryResult?.mg_draft">
        <span class="badge badge-secondary">Draft</span>
      </div>
    </div>

    <template v-slot:footer>
      <ButtonAction @click="emit('onClose')">Close</ButtonAction>
    </template>
  </SideModal>
</template>

<script lang="ts" setup>
import { ref, toRefs, watch } from "vue";
import MessageError from "../forms/MessageError.vue";
import ButtonAction from "../forms/ButtonAction.vue";
import SideModal from "./SideModal.vue";

const props = defineProps({
  tableId: {
    type: String,
    default: "",
  },
  label: {
    type: String,
    default: "",
  },
  rows: {
    type: Array,
    default: [],
  },
  isVisible: {
    type: Boolean,
    default: true,
  },
  showDataOwner: {
    type: Boolean,
    default: false,
  },
  client: {
    type: Object,
    required: true,
  },
});
const { client, isVisible, label, rows, tableId } = toRefs(props);

let queryResults = ref([{ name: String }]);
let errorMessage = ref("");
const emit = defineEmits(["onClose"]);
watch([tableId, label, rows], () => {
  updateData();
});

function filteredResults(queryResult) {
  const tableResult = { ...queryResult };
  delete tableResult.name;
  delete tableResult.mg_insertedBy;
  delete tableResult.mg_insertedOn;
  delete tableResult.mg_updatedBy;
  delete tableResult.mg_updatedOn;
  delete tableResult.mg_draft;
  return tableResult;
}

async function updateData() {
  errorMessage.value = "";
  queryResults.value = [];
  if (tableId.value !== "") {
    for (const row of rows.value) {
      const queryResult = await client.value
        .fetchRowData(tableId.value, { name: row })
        .catch(() => {
          errorMessage.value = "Failed to load reference data";
        });
      queryResults.value.push(queryResult);
    }
  }
}
</script>

<style scoped>
table .key {
  width: 0;
}
</style>

<docs>
<template>
  <RefSideModal
    label="Label"
    tableId="Pet"
    :row="{}"
    :isVisible="showModal"
    @onClose="showModal = false"
  />
  <br /><button @click="showModal = !showModal">Toggle modal</button>
</template>
<script>
export default {
  data: function () {
    return {
      showModal: false,
    };
  },
};
</script>
</docs>
