<script setup lang="ts">
import type { ITreeNodeState } from "../../../types/types";
import BaseIcon from "../BaseIcon.vue";
import CustomTooltip from "../CustomTooltip.vue";
import {computed, onMounted, onUnmounted, ref, useTemplateRef, watch} from "vue";
import InputCheckboxIcon from "../input/CheckboxIcon.vue";
import InputRadioIcon from "../input/RadioIcon.vue";
import InputLabel from "../input/Label.vue";
import ButtonText from "../button/Text.vue";

const props = withDefaults(
    defineProps<{
      id: string;
      parentNode: ITreeNodeState;
      inverted?: boolean;
      isRoot: boolean;
      multiselect?: boolean;
      valid?: boolean;
      invalid?: boolean;
      disabled?: boolean;
      isSearching?: boolean;
      scrollContainer?: HTMLElement | null;
    }>(),
    {
      inverted: false,
      isRoot: false,
      multiselect: true,
      isSearching: false,
      scrollContainer: null,
    }
);
const emit = defineEmits([
  "toggleSelect",
  "toggleExpand",
  "showOutsideResults",
  "loadMore",
]);

function toggleSelect(node: ITreeNodeState) {
  emit("toggleSelect", node);
}

function toggleExpand(node: ITreeNodeState) {
  emit("toggleExpand", node);
}

function loadMore(node?: ITreeNodeState) {
  emit("loadMore", node);
}

const hasChildren = computed(() =>
    props.parentNode?.children?.some((node) => node.children?.length) || false
);

const hiddenNodesCount = computed(() =>
    props.parentNode?.children?.filter((child) => child.visible === false).length || 0
);

const hiddenSelectedCount = computed(() =>
    props.parentNode?.children?.filter((node) => node.visible === false && node.selected === "selected").length || 0
);

const nodes = computed(() => props.parentNode?.children || []);

const hasMoreTerms = computed(() => props.parentNode?.loadMoreHasMore || false);

const remainingTermsCount = computed(() =>
    (props.parentNode?.loadMoreTotal || 0) - (props.parentNode?.children?.length || 0)
);

// Track if we're currently loading to prevent duplicate requests
const isLoading = ref(false);

// Template ref for the load more trigger element
const loadMoreTrigger = useTemplateRef<HTMLElement>('loadMoreTrigger');

// Set up intersection observer manually to handle dynamic scroll container
let observer: IntersectionObserver | null = null;

function setupObserver(trigger: HTMLElement, container: HTMLElement | null) {
  // Clean up previous observer safely
  if (observer) {
    try {
      if (typeof observer.disconnect === 'function') {
        observer.disconnect();
      }
    } catch (e) {
      console.warn('Error disconnecting observer:', e);
    }
    observer = null;
  }

  if (!trigger) return;

  try {
    observer = new IntersectionObserver(
        async (entries) => {
          for (const entry of entries) {
            // Only trigger if:
            // 1. Element is intersecting
            // 2. We have more terms to load
            // 3. Not currently searching
            // 4. Not already loading
            // 5. Parent node exists
            if (
                entry.isIntersecting &&
                hasMoreTerms.value &&
                !props.isSearching &&
                !isLoading.value &&
                props.parentNode
            ) {
              isLoading.value = true;

              // Disconnect observer immediately to prevent duplicate triggers
              if (observer && typeof observer.disconnect === 'function') {
                try {
                  observer.disconnect();
                } catch (e) {
                  console.warn('Error disconnecting during load:', e);
                }
              }

              await loadMore(props.parentNode);
              isLoading.value = false;

              // Re-setup observer after loading completes
              // Need to check if we still have more and the trigger still exists
              if (hasMoreTerms.value && loadMoreTrigger.value) {
                setupObserver(loadMoreTrigger.value, props.scrollContainer);
              }
            }
          }
        },
        {
          root: container || null,
          rootMargin: '200px', // Increased margin for earlier loading
          threshold: 0,
        }
    );

    // Only observe if we successfully created the observer
    if (observer && typeof observer.observe === 'function') {
      observer.observe(trigger);
    }
  } catch (e) {
    console.warn('Error setting up IntersectionObserver:', e);
    observer = null;
  }
}

onMounted(() => {
  // Set up observer when component mounts
  watch(
      [loadMoreTrigger, () => props.scrollContainer],
      ([trigger, container]) => {
        if (!trigger) return;
        setupObserver(trigger, container);
      },
      { immediate: true }
  );

  // Also watch hasMoreTerms to re-setup when it changes
  watch(hasMoreTerms, (newValue, oldValue) => {
    const trigger = loadMoreTrigger.value;
    const container = props.scrollContainer;
    // Only setup if transitioning from false to true (new data available)
    if (trigger && newValue && !oldValue) {
      setupObserver(trigger, container);
    }
  });
});

onUnmounted(() => {
  if (observer) {
    try {
      if (typeof observer.disconnect === 'function') {
        observer.disconnect();
      }
    } catch (e) {
      // Silently ignore cleanup errors in tests
    }
    observer = null;
  }
});
</script>

<template>
  <ul
      :class="[
      inverted
        ? 'text-search-filter-group-title-inverted'
        : 'text-search-filter-group-title',
    ]"
  >
    <li
        v-for="node in nodes.filter((node2) => node2.visible === true)"
        :key="id + node.name"
        class="mt-2.5 relative"
    >
      <div class="flex items-center">
        <button
            v-if="node.children?.length"
            @click.stop="toggleExpand(node)"
            class="-left-[15px] top-0 rounded-full hover:cursor-pointer h-6 w-6 flex items-center justify-center absolute z-20"
            :class="{
            'text-search-filter-group-toggle-inverted hover:bg-search-filter-group-toggle-inverted':
              inverted,
            'text-button-tree-node-toggle hover:bg-button-tree-node-toggle hover:text-button-tree-node-toggle-hover':
              !inverted,
          }"
            :aria-expanded="node.expanded"
            :aria-controls="node.name"
        >
          <BaseIcon
              :name="node.expanded ? 'caret-down' : 'caret-right'"
              :width="20"
          />
          <span class="sr-only">expand {{ node.name }}</span>
        </button>
        <template v-if="!isRoot">
          <BaseIcon
              v-if="node.children?.length"
              name="collapsible-list-item-sub"
              :width="20"
              class="text-blue-200 absolute -top-[9px] -left-[5px]"
          />
          <BaseIcon
              v-else
              name="collapsible-list-item"
              :width="20"
              class="text-blue-200 absolute -top-[9px] -left-1"
          />
        </template>
      </div>
      <div
          class="flex justify-start items-center"
          :class="{ 'ml-4': !isRoot || hasChildren }"
      >
        <InputLabel
            :for="id + '-' + node.name + '-input'"
            class="group flex justify-center items-start"
            :class="{
            'text-disabled cursor-not-allowed': disabled,
            'text-title cursor-pointer ': !disabled,
          }"
        >
          <input
              v-if="node.selectable"
              type="checkbox"
              :indeterminate="node.selected === 'intermediate'"
              :id="id + '-' + node.name + '-input'"
              :name="node.name"
              :checked="node.selected === 'selected'"
              @click.stop="toggleSelect(node)"
              class="sr-only"
          />
          <InputCheckboxIcon
              v-if="node.selectable && multiselect"
              :indeterminate="node.selected === 'intermediate'"
              :checked="node.selected === 'selected'"
              class="min-w-[20px]"
              :class="{
              '[&>rect]:stroke-gray-400': inverted,
            }"
              :invalid="invalid"
              :valid="valid"
              :disabled="disabled"
          />
          <InputRadioIcon
              v-else-if="node.selectable"
              :indeterminate="node.selected === 'intermediate'"
              :checked="node.selected === 'selected'"
              class="min-w-[20px] mr-[6px] mt-[2px]"
              :class="{
              '[&>rect]:stroke-gray-400': inverted,
            }"
              :invalid="invalid"
              :valid="valid"
              :disabled="disabled"
          />
          <span
              class="block text-body-sm leading-normal pl-1"
              :class="inverted ? 'text-title-contrast' : 'text-title'"
          >
            {{ node.label || node.name }}
          </span>
        </InputLabel>
        <div
            class="inline-flex items-center whitespace-nowrap"
            v-if="node.description"
        >
          <div class="inline-block pl-1">
            <CustomTooltip
                label="Read more"
                :hoverColor="inverted ? 'none' : 'white'"
                :content="node.description"
            />
          </div>
        </div>
      </div>
      <template v-if="node.children?.length && node.expanded">
        <TreeNode
            :id="id"
            class="ml-[31px]"
            :parentNode="node"
            :isRoot="false"
            :inverted="inverted"
            :invalid="invalid"
            :valid="valid"
            :disabled="disabled"
            :multiselect="multiselect"
            :isSearching="isSearching"
            :scrollContainer="scrollContainer"
            @toggleSelect="toggleSelect"
            @toggleExpand="toggleExpand"
            @loadMore="loadMore"
        />
      </template>
    </li>

    <!-- Load More trigger - now auto-loads when visible -->
    <li
        v-if="hasMoreTerms && !isSearching"
        ref="loadMoreTrigger"
        class="mt-2.5 relative"
    >
      <div class="flex items-center">
        <template v-if="!isRoot">
          <BaseIcon
              name="collapsible-list-item"
              :width="20"
              class="text-blue-200 absolute -top-[9px] -left-1"
          />
        </template>
        <div class="ml-6 flex items-center gap-1">
          <span class="text-body-sm italic text-input-description">
            {{ remainingTermsCount }} more term{{ remainingTermsCount !== 1 ? 's' : '' }}
          </span>
          <ButtonText
              class="text-input underline"
              @click.stop="loadMore(parentNode)"
          >
            (load more)
          </ButtonText>
        </div>
      </div>
    </li>

    <!-- Search hidden terms -->
    <li
        v-if="hiddenNodesCount > 0"
        class="mt-2.5 relative"
    >
      <div class="flex items-center">
        <template v-if="!isRoot">
          <BaseIcon
              name="collapsible-list-item"
              :width="20"
              class="text-blue-200 absolute -top-[9px] -left-1"
          />
        </template>
        <div class="ml-6 flex items-center gap-1">
          <span class="text-body-sm italic text-input-description">
            {{ hiddenNodesCount }} term{{ hiddenNodesCount !== 1 ? 's' : '' }} hidden by search{{
              hiddenSelectedCount > 0
                  ? ` (including ${hiddenSelectedCount} selected)`
                  : ""
            }}
          </span>
          <ButtonText
              class="text-input underline"
              @click.stop="
              nodes.forEach((node) => (node.visible = true));
              emit('showOutsideResults');
            "
          >
            (show)
          </ButtonText>
        </div>
      </div>
    </li>
  </ul>
</template>