<template>
  <Modal v-model:visible="visible" :title="`Manage ${userName}'s tokens`">
    <Table>
      <template #head>
        <TableHeadRow>
            <TableHead></TableHead>
            <TableHead>Name</TableHead>
        </TableHeadRow>
      </template>
      <template #body>
        <TableRow v-for="token in userTokens">
          <TableCell>
            <Button
                  iconOnly
                  icon="trash"
                  type="secondary"
                  size="small"
                  label="Remove token"
                  @click="console.error('not implemented')"
                />
          </TableCell>

          <TableCell>
            {{ token }}
          </TableCell>
        </TableRow>
      </template>
    </Table>
  </Modal>
</template>

<script setup lang="ts">
import type { Modal } from "#build/components";
import { computed, ref } from "vue";
import type { IUser } from "~/util/adminUtils";

const props = defineProps<{
  user: IUser;
}>();

const visible = defineModel("visible", { required: true });

function closeModal() {
  visible.value = false;
}

const userName = ref<string>(props.user.email);
const userTokens = ref<string[]>(props.user.tokens || ([] as string[]));
</script>
