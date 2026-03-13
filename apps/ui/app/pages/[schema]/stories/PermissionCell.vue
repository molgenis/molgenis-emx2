<template>
  <Story title="PermissionCell" :description="spec">
    <div class="space-y-8 p-4">
      <div>
        <h3 class="text-lg font-semibold mb-2">Select dropdown</h3>
        <PermissionCell v-model="selectValue" :options="SELECT_OPTIONS" />
        <p class="mt-1 text-sm">Value: {{ selectValue ?? "null" }}</p>
      </div>

      <div>
        <h3 class="text-lg font-semibold mb-2">Modify dropdown</h3>
        <PermissionCell v-model="modifyValue" :options="MODIFY_OPTIONS" />
        <p class="mt-1 text-sm">Value: {{ modifyValue ?? "null" }}</p>
      </div>

      <div>
        <h3 class="text-lg font-semibold mb-2">With inherited value</h3>
        <PermissionCell
          v-model="inheritedValue"
          :options="SELECT_OPTIONS"
          inherited="ROW"
        />
        <p class="mt-1 text-sm">
          Value: {{ inheritedValue ?? "null (showing inherited)" }}
        </p>
      </div>

      <div>
        <h3 class="text-lg font-semibold mb-2">Disabled</h3>
        <PermissionCell
          v-model="disabledValue"
          :options="SELECT_OPTIONS"
          :disabled="true"
        />
      </div>

      <div>
        <h3 class="text-lg font-semibold mb-2">Grant column</h3>
        <PermissionCell
          v-model="grantValue"
          :options="['Yes']"
          :isGrant="true"
        />
        <p class="mt-1 text-sm">Value: {{ grantValue ?? "null" }}</p>
      </div>
    </div>
  </Story>
</template>

<script setup>
import { ref } from "vue";
import PermissionCell from "~/components/PermissionCell.vue";
import { SELECT_OPTIONS, MODIFY_OPTIONS } from "~/util/roleUtils";

const selectValue = ref(null);
const modifyValue = ref(null);
const inheritedValue = ref(null);
const disabledValue = ref("TABLE");
const grantValue = ref(null);

const spec = `
## Features
- Dropdown for permission level selection
- Shows inherited values in grey/italic when own value is null
- Grant column shows Yes/— options
- Disabled state for system roles

## Props
| Prop | Type | Default |
|------|------|---------|
| modelValue | string/null | null |
| options | string[] | required |
| inherited | string/null | null |
| disabled | boolean | false |
| isGrant | boolean | false |

## Test Checklist
- [ ] Select dropdown shows all 6 levels
- [ ] Modify dropdown shows TABLE/ROW
- [ ] Inherited value shown when modelValue is null
- [ ] Disabled prevents interaction
- [ ] Grant shows Yes/— only
`;
</script>
