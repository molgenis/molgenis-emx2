<template>
  <SideModal :label="label" :isVisible="isVisible" @onClose="emit('onClose')">
    <MessageError v-if="errorMessage">{{ errorMessage }}</MessageError>
    {{ JSON.stringify(queryResult) }}
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
  client: {
    type: Object,
    required: true,
  },
});
const { client, isVisible, label, row, tableId } = toRefs(props);

let queryResult = ref({});
let errorMessage = ref("");
const emit = defineEmits(["onClose"]);
watch([tableId, label, row], () => {
  updateData();
});

async function updateData() {
  if (tableId.value !== "") {
    queryResult.value = await client.value
      .fetchRowData(tableId.value, { name: row.value })
      .catch(() => {
        errorMessage.value = "Failed to load reference data";
      });
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
