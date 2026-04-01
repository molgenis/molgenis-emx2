<template>
  <Modal
    v-model:visible="visibleModel"
    title="Add Input Artifacts"
    max-width="max-w-3xl"
  >
    <div class="p-5">
      <!-- Tab bar -->
      <div class="flex gap-1 border-b border-color-theme mb-4">
        <button
          class="px-4 py-2 text-sm font-medium -mb-px"
          :class="activeTab === 'upload'
            ? 'border-b-2 border-blue-500 text-blue-600'
            : 'text-definition-list-term hover:text-title'"
          @click="activeTab = 'upload'"
        >
          Upload New
        </button>
        <button
          class="px-4 py-2 text-sm font-medium -mb-px"
          :class="activeTab === 'select'
            ? 'border-b-2 border-blue-500 text-blue-600'
            : 'text-definition-list-term hover:text-title'"
          @click="activeTab = 'select'"
        >
          Select Existing
        </button>
      </div>

      <!-- Select Existing tab -->
      <div v-if="activeTab === 'select'">
        <InputSearch
          id="artifact-picker-search"
          v-model="searchQuery"
          placeholder="Filter by name or type..."
          size="small"
          class="mb-3"
        />

        <p v-if="loading" class="text-sm text-definition-list-term py-4 text-center">
          Loading artifacts...
        </p>

        <ul
          v-else-if="filteredArtifacts.length"
          class="max-h-80 overflow-y-auto space-y-2"
        >
          <li
            v-for="artifact in filteredArtifacts"
            :key="artifact.id"
            class="flex items-center gap-3 p-3 rounded-lg border border-color-theme cursor-pointer"
            :class="{
              'bg-blue-50 dark:bg-blue-900/20 border-blue-300 dark:border-blue-700':
                pendingSelections.has(artifact.id),
              'opacity-50 cursor-default': alreadySelected.includes(artifact.id),
              'hover:bg-content': !alreadySelected.includes(artifact.id),
            }"
            @click="toggleSelection(artifact.id)"
          >
            <input
              type="checkbox"
              :checked="pendingSelections.has(artifact.id) || alreadySelected.includes(artifact.id)"
              :disabled="alreadySelected.includes(artifact.id)"
              class="h-4 w-4 rounded border-gray-300 accent-blue-500"
              @click.stop
              @change="toggleSelection(artifact.id)"
            />
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium text-title truncate">
                {{ artifact.name || artifact.id?.substring(0, 8) }}
              </p>
              <p class="text-xs text-definition-list-term">
                {{ artifact.type || "no type" }}
                · {{ formatSize(Number(artifact.size_bytes || 0)) }}
                · {{ formatDate(artifact.committed_at) }}
              </p>
            </div>
            <span
              v-if="alreadySelected.includes(artifact.id)"
              class="text-xs text-definition-list-term whitespace-nowrap"
            >
              already added
            </span>
          </li>
        </ul>

        <p
          v-else
          class="text-sm text-definition-list-term py-4 text-center"
        >
          No matching committed artifacts found.
        </p>
      </div>

      <!-- Upload New tab -->
      <div v-if="activeTab === 'upload'">
        <ArtifactUploadForm
          ref="uploadFormRef"
          embedded
          @created="onArtifactUploaded"
          @close="activeTab = 'select'"
        />
      </div>
    </div>

    <template #footer="{ hide }">
      <div class="flex justify-end gap-2 p-3">
        <Button type="outline" size="small" @click="hide()">Cancel</Button>
        <Button
          v-if="activeTab === 'upload' && uploadFormRef?.step === 'metadata'"
          type="primary"
          size="small"
          :disabled="!uploadFormRef?.canSubmit"
          @click="uploadFormRef?.startUpload()"
        >
          Upload Artifact
        </Button>
        <Button
          v-if="activeTab === 'select' && newSelections.length"
          type="primary"
          size="small"
          @click="confirmSelection"
        >
          Add {{ newSelections.length }} Artifact{{ newSelections.length !== 1 ? "s" : "" }}
        </Button>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { type ComponentPublicInstance, computed, ref, watch } from "vue";
import Button from "../../../tailwind-components/app/components/Button.vue";
import InputSearch from "../../../tailwind-components/app/components/input/Search.vue";
import Modal from "../../../tailwind-components/app/components/Modal.vue";
import type { NormalizedArtifact } from "../composables/useArtifactsApi";
import { fetchArtifacts } from "../composables/useHpcApi";
import ArtifactUploadForm from "./ArtifactUploadForm.vue";

const props = defineProps<{
	visible: boolean;
	alreadySelected: string[];
}>();

const emit = defineEmits<{
	(e: "update:visible", value: boolean): void;
	(e: "confirm", artifacts: NormalizedArtifact[]): void;
}>();

const uploadFormRef = ref<ComponentPublicInstance<{
  canSubmit: boolean;
  startUpload: () => void;
  step: string;
}> | null>(null);

const visibleModel = computed({
	get: () => props.visible,
	set: (val: boolean) => emit("update:visible", val),
});

const activeTab = ref<"select" | "upload">("upload");
const searchQuery = ref("");
const loading = ref(false);
const allArtifacts = ref<NormalizedArtifact[]>([]);
const pendingSelections = ref(new Set<string>());

const filteredArtifacts = computed(() => {
	const q = searchQuery.value.toLowerCase().trim();
	if (!q) return allArtifacts.value;
	return allArtifacts.value.filter((a) => {
		const name = (a.name || "").toLowerCase();
		const type = (a.type || "").toLowerCase();
		return name.includes(q) || type.includes(q);
	});
});

const newSelections = computed(() =>
	allArtifacts.value.filter(
		(a) =>
			pendingSelections.value.has(a.id) &&
			!props.alreadySelected.includes(a.id),
	),
);

function toggleSelection(id: string) {
	if (props.alreadySelected.includes(id)) return;
	const next = new Set(pendingSelections.value);
	if (next.has(id)) {
		next.delete(id);
	} else {
		next.add(id);
	}
	pendingSelections.value = next;
}

async function loadArtifacts() {
	loading.value = true;
	try {
		const result = await fetchArtifacts({ status: "COMMITTED", limit: 100 });
		allArtifacts.value = result.items;
	} catch {
		allArtifacts.value = [];
	} finally {
		loading.value = false;
	}
}

async function onArtifactUploaded(artifactId: string | null) {
	await loadArtifacts();
	if (artifactId) {
		const uploaded = allArtifacts.value.find((a) => a.id === artifactId);
		if (uploaded) {
			emit("confirm", [uploaded]);
			visibleModel.value = false;
			return;
		}
	}
	activeTab.value = "select";
}

function confirmSelection() {
	emit("confirm", newSelections.value);
	visibleModel.value = false;
}

function formatSize(bytes: number): string {
	const n = Number(bytes || 0);
	if (n < 1024) return `${n} B`;
	if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
	if (n < 1024 * 1024 * 1024) return `${(n / (1024 * 1024)).toFixed(1)} MB`;
	return `${(n / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}

function formatDate(dateStr: string | null | undefined): string {
	if (!dateStr) return "-";
	return new Date(dateStr).toLocaleDateString();
}

// Reload artifacts and reset state when modal opens
watch(
	() => props.visible,
	(open) => {
		if (open) {
			pendingSelections.value = new Set();
			searchQuery.value = "";
			activeTab.value = "upload";
			loadArtifacts();
		}
	},
);
</script>
