<template>
  <div class="card">
    <div class="card-body">
      <h5 class="card-title">
        <component
          :is="icon"
          :size="iconSize.toString()"
          class="mg-card-icon"
        />
        {{ cardTitle }}
      </h5>
      <slot></slot>
    </div>
    <div v-if="footerText" class="card-footer">
      <small class="text-muted">{{ footerText }}</small>
    </div>
  </div>
</template>

<script>
import {
  DatabaseIcon,
  UsersIcon,
  TestPipeIcon,
  CirclePlusIcon,
  NewsIcon,
  ToolsIcon,
  ClockIcon,
  AffiliateIcon,
} from "vue-tabler-icons";

export default {
  name: "IconCard",
  props: {
    cardTitle: {
      type: String,
      required: false,
    },
    footerText: {
      type: String,
      required: false,
    },
    icon: {
      // one of [DatabaseIcon, UsersIcon, TestPipeIcon, CirclePlusIcon, ToolsIcon, NewsIcon, ClockIcon, AffiliateIcon]
      type: String,
      required: false,
    },
    iconSize: {
      type: Number,
      required: false,
      default: () => 62,
    },
  },
  components: {
    DatabaseIcon,
    UsersIcon,
    TestPipeIcon,
    CirclePlusIcon,
    ToolsIcon,
    NewsIcon,
    ClockIcon,
    AffiliateIcon,
  },
};
</script>

<style scoped>
.mg-card-icon {
  color: var(--primary);
}
</style>
