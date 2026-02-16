<script setup lang="ts">
import type { ITreeNodeState } from "../../../types/types";
import BaseIcon from "../BaseIcon.vue";
import CustomTooltip from "../CustomTooltip.vue";
import {
  computed,
  onMounted,
  onUnmounted,
  ref,
  useTemplateRef,
  watch,
} from "vue";
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
    enableAutoLoad?: boolean; // Whether to enable IntersectionObserver auto-loading
  }>(),
  {
    inverted: false,
    isRoot: false,
    multiselect: true,
    isSearching: false,
    scrollContainer: null,
    enableAutoLoad: true, // Default to enabled for backward compatibility
  }
);
const emit = defineEmits([
  "toggleSelect",
  "toggleExpand",
  "showOutsideResults",
  "loadMore",
  "showAllChildren", // New event for showing all children of a node
  "applyFilter", // New event for reapplying search filter
]);

function toggleSelect(node: ITreeNodeState) {
  emit("toggleSelect", node);
}

function toggleExpand(node: ITreeNodeState) {
  emit("toggleExpand", node);
}

function showAllChildren(node: ITreeNodeState) {
  emit("showAllChildren", node);
}

function applyFilter(node: ITreeNodeState) {
  emit("applyFilter", node);
}

function loadMore(node?: ITreeNodeState) {
  emit("loadMore", node);
}

function isNodeShowingAll(node: ITreeNodeState): boolean {
  return (node as any).showingAll || false;
}

const hasChildren = computed(
  () =>
    props.parentNode.children?.some((node) => node.children?.length) || false
);

const hiddenNodesCount = computed(
  () =>
    props.parentNode.children?.filter((child) => child.visible === false)
      .length || 0
);

const hiddenSelectedCount = computed(
  () =>
    props.parentNode.children?.filter(
      (node) => node.visible === false && node.selected === "selected"
    ).length || 0
);

const nodes = computed(() => props.parentNode.children || []);

const hasMoreTerms = computed(() => props.parentNode.loadMoreHasMore || false);

const remainingTermsCount = computed(
  () =>
    (props.parentNode.loadMoreTotal || 0) -
    (props.parentNode.children?.length || 0)
);

const isShowingAll = computed(() => isNodeShowingAll(props.parentNode));

const canShowAll = computed(() => {
  if (!props.isSearching || isShowingAll.value) return false;
  if (hiddenBySearchCount.value > 0) return true;
  const unfilteredTotal = (props.parentNode as any)?.unfilteredTotal;
  if (unfilteredTotal === undefined && props.isSearching) {
    if (
      (props.parentNode?.children?.length || 0) === 0 &&
      props.parentNode?.loadMoreTotal === 0
    ) {
      return false;
    }
    return true;
  }
  const filteredCount = props.parentNode?.loadMoreTotal || 0;
  if (unfilteredTotal !== undefined && filteredCount === unfilteredTotal) {
    return false;
  }
  if (props.parentNode?.children !== undefined && unfilteredTotal > 0)
    return true;

  return false;
});

const showAllMessage = computed(() => {
  const visibleCount =
    props.parentNode.children?.filter((c: any) => c.visible).length || 0;

  if (hiddenBySearchCount.value > 0) {
    return `${hiddenBySearchCount.value} term${
      hiddenBySearchCount.value !== 1 ? "s" : ""
    } hidden by filter`;
  }

  if (visibleCount === 0 && props.isSearching) {
    return "All children hidden by filter";
  }

  return "Some children may be hidden by filter";
});

const combinedLoadMessage = computed(() => {
  const hasMoreToLoad = hasMoreTerms.value;
  const hasHiddenBySearch = canShowAll.value && !isShowingAll.value;
  const isCurrentlyShowingAll = isShowingAll.value;

  if (isCurrentlyShowingAll && props.isSearching) {
    const visibleCount =
      props.parentNode.children?.filter((c: any) => c.visible).length || 0;
    const filteredCount = (props.parentNode as any)?.filteredCount || 0;

    return {
      show: true,
      message: `Showing all ${visibleCount} term${
        visibleCount !== 1 ? "s" : ""
      } (${filteredCount} match filter)`,
      showLoadMore: hasMoreToLoad,
      showShowAll: false,
      showApplyFilter: true,
    };
  }

  if (hasMoreToLoad && hasHiddenBySearch) {
    return {
      show: true,
      message: `${remainingTermsCount.value} more (${hiddenBySearchCount.value} hidden by filter)`,
      showLoadMore: true,
      showShowAll: true,
      showApplyFilter: false,
    };
  }

  if (hasMoreToLoad) {
    return {
      show: true,
      message: `${remainingTermsCount.value} more term${
        remainingTermsCount.value !== 1 ? "s" : ""
      }`,
      showLoadMore: true,
      showShowAll: false,
      showApplyFilter: false,
    };
  }

  if (hasHiddenBySearch) {
    return {
      show: true,
      message: showAllMessage.value,
      showLoadMore: false,
      showShowAll: true,
      showApplyFilter: false,
    };
  }

  return {
    show: false,
    message: "",
    showLoadMore: false,
    showShowAll: false,
    showApplyFilter: false,
  };
});

const hiddenBySearchCount = computed(() => {
  if (!props.isSearching || isShowingAll.value) return 0;
  const unfilteredTotal = (props.parentNode as any)?.unfilteredTotal;
  const filteredCount = props.parentNode?.loadMoreTotal || 0;
  if (unfilteredTotal === undefined) return 0;
  return Math.max(0, unfilteredTotal - filteredCount);
});

const isLoading = ref(false);
const loadMoreTrigger = useTemplateRef<HTMLElement>("loadMoreTrigger");
let observer: IntersectionObserver | null = null;

function setupObserver(trigger: HTMLElement, container: HTMLElement | null) {
  if (observer) {
    try {
      if (typeof observer.disconnect === "function") {
        observer.disconnect();
      }
    } catch (e) {
      console.warn("Error disconnecting observer:", e);
    }
    observer = null;
  }

  if (!trigger) return;

  try {
    observer = new IntersectionObserver(
      async (entries) => {
        for (const entry of entries) {
          if (
            entry.isIntersecting &&
            hasMoreTerms.value &&
            !isLoading.value &&
            props.parentNode
          ) {
            isLoading.value = true;
            if (observer && typeof observer.disconnect === "function") {
              try {
                observer.disconnect();
              } catch (e) {
                console.warn("Error disconnecting during load:", e);
              }
            }
            await loadMore(props.parentNode);
            isLoading.value = false;
            if (hasMoreTerms.value && loadMoreTrigger.value) {
              setupObserver(loadMoreTrigger.value, props.scrollContainer);
            }
          }
        }
      },
      {
        root: container || null,
        rootMargin: "200px",
        threshold: 0,
      }
    );

    if (observer && typeof observer.observe === "function") {
      observer.observe(trigger);
    }
  } catch (e) {
    console.warn("Error setting up IntersectionObserver:", e);
    observer = null;
  }
}

onMounted(() => {
  if (props.enableAutoLoad) {
    watch(
      [loadMoreTrigger, () => props.scrollContainer],
      ([trigger, container]) => {
        if (!trigger) return;
        setupObserver(trigger, container);
      },
      { immediate: true }
    );

    watch(hasMoreTerms, (newValue, oldValue) => {
      const trigger = loadMoreTrigger.value;
      const container = props.scrollContainer;
      if (trigger && newValue && !oldValue) {
        setupObserver(trigger, container);
      }
    });
  }
});

onUnmounted(() => {
  if (observer) {
    try {
      if (typeof observer.disconnect === "function") {
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
      <template v-if="node.expanded && node.children !== undefined">
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
          :enableAutoLoad="enableAutoLoad"
          @toggleSelect="toggleSelect"
          @toggleExpand="toggleExpand"
          @loadMore="loadMore"
          @showAllChildren="showAllChildren"
          @applyFilter="applyFilter"
        />
      </template>
    </li>
    <li
      v-if="combinedLoadMessage.show"
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
            {{ combinedLoadMessage.message }}
          </span>
          <ButtonText
            v-if="combinedLoadMessage.showLoadMore"
            class="text-input underline"
            @click.stop="loadMore(parentNode)"
          >
            (load more)
          </ButtonText>
          <ButtonText
            v-if="combinedLoadMessage.showShowAll"
            class="text-input underline"
            @click.stop="showAllChildren(parentNode)"
          >
            (show filtered)
          </ButtonText>
          <ButtonText
            v-if="combinedLoadMessage.showApplyFilter"
            class="text-input underline"
            @click.stop="applyFilter(parentNode)"
          >
            (apply filter)
          </ButtonText>
        </div>
      </div>
    </li>
    <li v-if="hiddenNodesCount > 0" class="mt-2.5 relative">
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
            {{ hiddenNodesCount }} term{{
              hiddenNodesCount !== 1 ? "s" : ""
            }}
            hidden by search{{
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
