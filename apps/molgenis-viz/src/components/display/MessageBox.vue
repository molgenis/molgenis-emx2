<template>
  <div :class="`message-box message-${type}`">
    <div class="message-icon" v-if="showIcon">
      <CheckCircleIcon v-if="type === 'success'" />
      <ExclamationTriangleIcon v-else-if="type === 'warning'" />
      <ExclamationCircleIcon v-else-if="type === 'error'" />
      <ChatBubbleBottomCenterIcon v-else />
    </div>
    <div class="message-text">
      <slot></slot>
    </div>
  </div>
</template>

<script>
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  ExclamationTriangleIcon,
  ChatBubbleBottomCenterIcon,
} from "@heroicons/vue/24/outline";

// Display an error, success, warning, or general text message.
export default {
  props: {
    type: {
      // `'default' / 'error' / 'success' / 'warning'`
      type: String,
      // `default`
      default: "default",
      validator: (value) => {
        return ["default", "error", "success", "warning"].includes(value);
      },
    },
    // If true (default), the message type icon will be displayed
    showIcon: {
      type: Boolean,
      // `true`
      default: true,
    },
  },
  components: {
    CheckCircleIcon,
    ExclamationCircleIcon,
    ExclamationTriangleIcon,
    ChatBubbleBottomCenterIcon,
  },
};
</script>

<style lang="scss">
.message-box {
  box-sizing: border-box;
  display: flex;
  border-radius: 6px;
  justify-content: flex-start;
  align-items: stretch;
  margin: 12px 0;

  $size: 32px;
  .message-icon {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 12px;
    border-radius: 6px 0 0 6px;
    svg {
      height: $size;
      width: $size;
      stroke-width: 2px;
    }
  }

  .message-text {
    padding: 0 12px;

    * {
      color: inherit;
    }
  }

  &.message-default {
    background-color: $gray-050;
    .message-icon {
      background-color: $gray-800;
      color: $gray-050;
    }
  }

  &.message-success {
    background-color: $green-100;
    color: $green-800;

    .message-icon {
      background-color: $green-800;
      color: $green-050;
    }
  }

  &.message-warning {
    background-color: $yellow-050;
    .message-icon {
      background-color: $yellow-400;
    }
  }

  &.message-error {
    background-color: $red-100;
    color: $red-900;

    .message-icon {
      background-color: $red-900;
      color: $red-050;
    }
  }
}
</style>
