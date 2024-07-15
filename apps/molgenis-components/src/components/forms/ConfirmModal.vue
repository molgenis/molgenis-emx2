<template>
  <LayoutModal :title="title" @close="$emit('close')">
    <template v-slot:body>
      <slot v-if="$slots.default"></slot>
      <div v-else>
        {{ actionLabel }}
        <strong>
          <span v-if="tableId">{{ tableLabel }}</span>
          <span v-if="pkey">({{ pkeyAsString }})</span>
        </strong>
        <br />Are you sure ?
        <br />
      </div>
    </template>
    <template v-slot:footer>
      <ButtonAlt @click="$emit('close')">Close</ButtonAlt>
      <ButtonDanger v-if="actionType === 'danger'" @click="$emit('confirmed')">
        {{ actionLabel }}
      </ButtonDanger>
      <ButtonAction v-else @click="$emit('confirmed')">
        {{ actionLabel }}
      </ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import LayoutModal from "../layout/LayoutModal.vue";
import ButtonAlt from "./ButtonAlt.vue";
import ButtonAction from "./ButtonAction.vue";
import ButtonDanger from "./ButtonDanger.vue";
import { flattenObject } from "../utils";

export default {
  name: "ConfirmModal",
  components: {
    LayoutModal,
    ButtonAlt,
    ButtonAction,
    ButtonDanger,
  },
  props: {
    title: {
      type: String,
      required: false,
    },
    actionLabel: {
      type: String,
      required: true,
    },
    tableId: {
      type: String,
      required: false,
    },
    tableLabel: {
      type: String,
      required: false,
    },
    pkey: Object,
    actionType: {
      type: String,
      required: false,
      default: () => "primary", // or danger
    },
  },
  methods: {
    flattenObject,
  },
  computed: {
    pkeyAsString() {
      return this.flattenObject(this.pkey);
    },
  },
};
</script>

<docs>
<template>
<div>
    <button @click="showDefaultConfirmModal = true">Show default conform modal</button>
    <ConfirmModal 
      v-if="showDefaultConfirmModal" 
      title="Confirm" 
      actionLabel="My action"
      @close="showDefaultConfirmModal = false"
      @confirmed="handleConfirm"
    />
    <br/>
    <button class="mt-3" @click="showConfirmModal = true">Show slots conform modal (with action type danger)</button>
    <ConfirmModal 
      v-if="showConfirmModal" 
      title="Confirm" 
      actionType="danger"
      actionLabel="My action"
      @close="showConfirmModal = false"
      @confirmed="handleConfirm"
    >This is my slot content</ConfirmModal>
    {{msg}}
</div>
</template>

<script>
export default {
    data () {
        return {
            showDefaultConfirmModal: false,
            showConfirmModal: false,
            msg: ''
        }
    },
    methods: {
        handleConfirm() {
            this.showDefaultConfirmModal = false,
            this.showConfirmModal = false,
            this.msg = 'confirmed'
        }
    }
}
</script>
</docs>
