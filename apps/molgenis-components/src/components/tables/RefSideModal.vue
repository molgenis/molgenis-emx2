<template>
  <SideModal :label="label" :isVisible="isVisible" @onClose="emit('onClose')">
    <MessageError v-if="errorMessage">{{ errorMessage }}</MessageError>
    <h5 v-if="queryResult?.name">{{ queryResult.name }}</h5>

    <table class="table table-borderless table-sm">
      <tr v-for="(value, key) in tableResult">
        <td>{{ key }}</td>
        <td>{{ value }}</td>
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
  row: {
    type: String,
    default: "",
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
const { client, isVisible, label, row, tableId } = toRefs(props);

let queryResult = ref({});
let tableResult = {};
let errorMessage = ref("");
const emit = defineEmits(["onClose"]);
watch([tableId, label, row], () => {
  updateData();
});

async function updateData() {
  errorMessage.value = "";
  if (tableId.value !== "") {
    queryResult.value = await client.value
      .fetchRowData(tableId.value, { name: row.value })
      .catch(() => {
        errorMessage.value = "Failed to load reference data";
      });

    tableResult = { ...queryResult.value };
    delete tableResult.name;
    delete tableResult.mg_insertedBy;
    delete tableResult.mg_insertedOn;
    delete tableResult.mg_updatedBy;
    delete tableResult.mg_updatedOn;
    delete tableResult.mg_draft;
  }
}
</script>

<style scoped></style>

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
