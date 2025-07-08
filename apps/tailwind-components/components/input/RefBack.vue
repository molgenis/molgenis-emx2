<script lang="ts" setup>
import type { IInputProps } from "~/types/types";
import type { columnValueObject } from "../../../metadata-utils/src/types";

const props = withDefaults(
  defineProps<
    IInputProps & {
      refSchemaId: string;
      refTableId: string;
      refLabel: string;
      canEdit?: boolean;
    }
  >(),
  {
    canEdit: true,
  }
);

const modelValue = defineModel<columnValueObject[]>();
</script>
<template>
  <Button v-if="canEdit" icon="plus" type="text" size="small"
    >Add{{ props.refTableId }}</Button
  >
  <ul class="border divide-y divide-gray-200">
    <InputRefListItem
      v-for="ref in modelValue"
      :data="ref"
      :refLabel="props.refLabel"
      :canEdit="props.canEdit"
    />
  </ul>
</template>
