<template>
  <div class="d-flex" style="gap: 0.5rem">
    <MessageSuccess v-if="successMessage">{{ successMessage }}</MessageSuccess>
    <MessageError v-if="errorMessage">{{ errorMessage }}</MessageError>
    <ButtonAlt @click="$emit('cancel')">Close</ButtonAlt>
    <ButtonOutline @click="$emit('saveDraft')">Save draft</ButtonOutline>
    <ButtonAction @click="$emit('save')">Save {{ tableName }}</ButtonAction>
  </div>
</template>

<script>
import MessageError from "./MessageError.vue";
import MessageSuccess from "./MessageSuccess.vue";
import ButtonAlt from "./ButtonAlt.vue";
import ButtonOutline from "./ButtonOutline.vue";
import ButtonAction from "./ButtonAction.vue";

export default {
  name: "RowEditFooter",
  components: {
    ButtonAction,
    ButtonAlt,
    MessageError,
    MessageSuccess,
    ButtonOutline,
  },
  props: {
    tableName: {
      type: String,
      required: false,
      default: () => null,
    },
    successMessage: {
      type: String,
      required: false,
      default: () => null,
    },
    errorMessage: {
      type: String,
      required: false,
      default: () => null,
    },
  },
};
</script>

<docs>
  <template>
  <div>

   <DemoItem>
      <label for="sample-plain">Plain</label>
      <RowEditFooter id="sample-plain"  />
    </DemoItem>

    <DemoItem>
      <label for="sample-footer">Styled with class modal-footer</label>
      <RowEditFooter id="sample-footer" class="modal-footer" />
    </DemoItem>

    <DemoItem>
      <label for="sample-table-name">With table name</label>
      <RowEditFooter id="sample-table-name" tableName="Pets"  />
    </DemoItem>

    <DemoItem>
      <label for="sample-success-msg">With success message</label>
      <RowEditFooter id="sample-success-msg" successMessage="All is well !" />
    </DemoItem>

    <DemoItem>
      <label for="sample-error-msg">With error message</label>
      <RowEditFooter id="sample-error-msg" errorMessage="We have a problem" />
    </DemoItem>
  </div>
  </template>
</docs>
