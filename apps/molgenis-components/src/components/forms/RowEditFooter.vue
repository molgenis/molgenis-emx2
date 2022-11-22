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
    <div class="d-flex" style="gap: 0.5rem;">
      <slot></slot>
      <ButtonAlt @click="$emit('cancel')"> Close</ButtonAlt>
      <ButtonOutline @click="$emit('saveDraft')"> Save draft</ButtonOutline>
      <Tooltip
        name="disabled-tooltip"
        :value="
          isSaveDisabled ? 'Saving is only possible on the last chapter' : ''
        "
      >
        <ButtonAction @click="$emit('save')" :disabled="isSaveDisabled">
          Save {{ tableName }}
        </ButtonAction>
      </Tooltip>
    </div>
  </div>
</template>

<script>
import MessageError from "./MessageError.vue";
import MessageSuccess from "./MessageSuccess.vue";
import ButtonAlt from "./ButtonAlt.vue";
import ButtonOutline from "./ButtonOutline.vue";
import ButtonAction from "./ButtonAction.vue";
import Tooltip from "./Tooltip.vue";

export default {
  name: "RowEditFooter",
  components: {
    ButtonAction,
    ButtonAlt,
    MessageError,
    MessageSuccess,
    ButtonOutline,
    Tooltip,
  },
  props: {
    tableName: {
      type: String,
      default: () => null,
    },
    successMessage: {
      type: String,
      default: () => null,
    },
    errorMessage: {
      type: String,
      default: () => null,
    },
    isSaveDisabled: {
      type: Boolean,
      default: () => false,
    },
  },
};
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
      <RowEditFooter id="sample-table-name" tableName="Pets" />
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
      <label for="sample-error-msg">With disabled tooltip</label>
      <RowEditFooter id="sample-error-msg" :isSaveDisabled="true" />
    </DemoItem>
  </div>
</template>
</docs>
