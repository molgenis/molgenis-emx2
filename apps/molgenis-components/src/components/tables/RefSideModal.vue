<template>
  <div v-if="loading">
    <Spinner />
  </div>
  <SideModal
    v-else
    :label="label"
    :isVisible="isVisible"
    @onClose="emit('onClose')"
  >
    <MessageError v-if="errorMessage">{{ errorMessage }}</MessageError>

    <div v-for="queryResult in queryResults">
      <RefTable :reference="queryResult" :showDataOwner="showDataOwner" />
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

let loading = ref(true);
let queryResults = ref([{ name: String }]);
let errorMessage = ref("");
const emit = defineEmits(["onClose"]);
watch([tableId, label, rows], () => {
  updateData();
});

async function updateData() {
  errorMessage.value = "";
  queryResults.value = [];
  loading.value = true;
  if (tableId.value !== "") {
    for (const row of rows.value) {
      const metaData = await client.value.fetchTableMetaData(tableId.value);
      const queryResult = await client.value
        .fetchRowData(tableId.value, { name: row })
        .catch(() => {
          errorMessage.value = "Failed to load reference data";
        });
      queryResult.metaData = metaData;
      queryResults.value.push(queryResult);
    }
  }
  loading.value = false;
}
</script>

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
