<template>
  <div class="w-100">
    <div>
      <MessageSuccess v-if="successMessage">
        {{ successMessage }}
      </MessageSuccess>
    </div>
    <div>
      <MessageError v-if="errorMessage">{{ errorMessage }}</MessageError>
    </div>
    <div class="d-flex" style="gap: 0.5rem">
      <slot></slot>
      <ButtonAlt @click="$emit('cancel')"> Close</ButtonAlt>
      <Tooltip
        name="disabled-draft-tooltip"
        :value="saveDraftDisabledMessage ? saveDraftDisabledMessage : ''"
        placement="bottom"
      >
        <ButtonOutline
          @click="$emit('saveDraft')"
          :disabled="Boolean(saveDraftDisabledMessage)"
          class="mr-2 pr-3"
        >
          Save draft
        </ButtonOutline>
      </Tooltip>
      <Tooltip
        name="disabled-save-tooltip"
        :value="saveDisabledMessage ? saveDisabledMessage : ''"
        placement="bottom"
      >
        <ButtonAction
          @click="$emit('save')"
          :disabled="Boolean(saveDisabledMessage)"
        >
          Save {{ tableId }}
        </ButtonAction>
      </Tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import MessageError from "./MessageError.vue";
import MessageSuccess from "./MessageSuccess.vue";
import ButtonAlt from "./ButtonAlt.vue";
import ButtonOutline from "./ButtonOutline.vue";
import ButtonAction from "./ButtonAction.vue";
import Tooltip from "./Tooltip.vue";
import { toRefs } from "vue";

const props = withDefaults(
  defineProps<{
    tableName?: string;
    successMessage?: string;
    errorMessage?: string;
    saveDraftDisabledMessage?: string;
    saveDisabledMessage?: string;
  }>(),
  { tableName: "" }
);

const {
  tableName,
  successMessage,
  errorMessage,
  saveDraftDisabledMessage,
  saveDisabledMessage,
} = toRefs(props);
</script>

<docs>
<template>
  <div>
    <DemoItem>
      <label for="sample-plain">Plain</label>
      <RowEditFooter id="sample-plain" />
    </DemoItem>
    <DemoItem>
      <label for="sample-footer">Styled with class modal-footer</label>
      <RowEditFooter id="sample-footer" class="modal-footer" />
    </DemoItem>
    <DemoItem>
      <label for="sample-table-name">With table name</label>
      <RowEditFooter id="sample-table-name" tableId="Pets" />
    </DemoItem>
    <DemoItem>
      <label for="sample-success-msg">With success message</label>
      <RowEditFooter id="sample-success-msg" successMessage="All is well !" />
    </DemoItem>
    <DemoItem>
      <label for="sample-error-msg">With error message</label>
      <RowEditFooter id="sample-error-msg" errorMessage="We have a problem" />
    </DemoItem>
     <DemoItem>
      <label for="sample-error-msg">With disabled save tooltip</label>
      <RowEditFooter id="sample-error-msg" :saveDisabledMessage="'Disabled because it\'s an example'" />
    </DemoItem>
    <DemoItem>
      <label for="sample-error-msg">With disabled draft tooltip</label>
      <RowEditFooter id="sample-error-msg2" :saveDraftDisabledMessage="'Draft disabled because it\'s an example'" />
    </DemoItem>
  </div>
</template>
</docs>
