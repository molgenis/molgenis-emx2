<template>
  <SideModal :label="label" :isVisible="isVisible" @onClose="emit('onClose')">
    {{ JSON.stringify(data) }}
    <template v-slot:footer>
      <ButtonAction @click="emit('onClose')">Close</ButtonAction>
    </template>
  </SideModal>
</template>

<script lang="ts" setup>
import { ref, watch } from "vue";

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

let data = ref({});
const emit = defineEmits(["onClose"]);
async function updateData() {
  if (props.tableId != "") {
    data.value = await props.client
      .fetchRowData(props.tableId, { name: props.row })
      .catch(() => {});
  }
}

watch(props, (newValue, oldValue) => {
  updateData();
});
</script>

<style scoped></style>

<docs>
      <template>
          <RefSideModal label="Label" tableId="Pet" :row="{}" />
      </template>
  </docs>
