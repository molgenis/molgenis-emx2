<template>
  <div class="space-y-4">
    <div
      v-if="loading"
      class="bg-form rounded-lg border border-color-theme p-4 text-sm text-definition-list-term"
    >
      Loading artifact...
    </div>
    <Message v-else-if="error" id="artifact-detail-error" invalid>
      {{ error }}
    </Message>
    <template v-else-if="artifact">
      <section class="bg-form rounded-lg border border-color-theme p-6">
        <div class="flex items-start justify-between mb-6">
          <div>
            <p class="text-lg font-semibold text-title">
              Artifact
              <code class="text-base bg-content px-1.5 py-0.5 rounded">{{
                shortId(artifact.id)
              }}</code>
            </p>
            <p class="text-sm text-definition-list-term">
              {{ artifact.type || "blob" }} /
              {{ artifact.residence || "managed" }} &bull; created
              {{ formatDate(artifact.created_at) }}
            </p>
          </div>
          <div class="flex items-center gap-2">
            <StatusBadge :status="artifact.status" />
            <Button
              type="outline"
              size="small"
              icon="trash"
              label="Delete"
              title="Delete artifact"
              :disabled="deleting"
              @click="onDelete"
            />
          </div>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div class="space-y-1">
            <span
              class="text-xs font-medium text-table-column-header uppercase tracking-wider"
              >Artifact ID</span
            >
            <span class="block text-sm text-title"
              ><code>{{ artifact.id }}</code></span
            >
          </div>
          <div class="space-y-1">
            <span
              class="text-xs font-medium text-table-column-header uppercase tracking-wider"
              >Name</span
            >
            <span class="block text-sm text-title">{{
              artifact.name || "-"
            }}</span>
          </div>
          <div class="space-y-1">
            <span
              class="text-xs font-medium text-table-column-header uppercase tracking-wider"
              >Type</span
            >
            <span class="block text-sm text-title">{{
              artifact.type || "-"
            }}</span>
          </div>
          <div class="space-y-1">
            <span
              class="text-xs font-medium text-table-column-header uppercase tracking-wider"
              >Residence</span
            >
            <span class="block text-sm text-title">{{
              artifact.residence || "-"
            }}</span>
          </div>
          <div class="space-y-1">
            <span
              class="text-xs font-medium text-table-column-header uppercase tracking-wider"
              >SHA-256</span
            >
            <span class="block text-sm text-title">
              <code v-if="artifact.sha256" class="text-xs break-all">{{
                artifact.sha256
              }}</code>
              <span v-else>-</span>
            </span>
          </div>
          <div class="space-y-1">
            <span
              class="text-xs font-medium text-table-column-header uppercase tracking-wider"
              >Size</span
            >
            <span class="block text-sm text-title">{{
              formatSize(artifact.size_bytes)
            }}</span>
          </div>
          <div v-if="artifact.content_url" class="space-y-1">
            <span
              class="text-xs font-medium text-table-column-header uppercase tracking-wider"
              >Content URL</span
            >
            <span class="block text-sm text-title">{{
              artifact.content_url
            }}</span>
          </div>
          <div class="space-y-1">
            <span
              class="text-xs font-medium text-table-column-header uppercase tracking-wider"
              >Committed</span
            >
            <span class="block text-sm text-title">{{
              formatDate(artifact.committed_at)
            }}</span>
          </div>
        </div>
      </section>

      <section
        v-if="provenance"
        class="bg-form rounded-lg border border-color-theme p-6"
      >
        <div class="mb-6">
          <p class="text-lg font-semibold text-title">Provenance</p>
          <p class="text-sm text-definition-list-term">
            Lineage information captured when this artifact was produced.
          </p>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div v-if="provenance.job_id" class="space-y-1">
            <span
              class="text-xs font-medium text-table-column-header uppercase tracking-wider"
              >Producing Job</span
            >
            <span class="block text-sm text-title">
              <NuxtLink
                :to="`/jobs/${provenance.job_id}`"
                class="text-link hover:underline"
              >
                <code>{{ shortId(provenance.job_id) }}</code>
              </NuxtLink>
              <span v-if="provenance.artifact_role" class="text-definition-list-term ml-1"
                >({{ provenance.artifact_role }})</span
              >
            </span>
          </div>
          <div v-if="provenance.processor" class="space-y-1">
            <span
              class="text-xs font-medium text-table-column-header uppercase tracking-wider"
              >Processor</span
            >
            <span class="block text-sm text-title"
              >{{ provenance.processor
              }}{{
                provenance.profile ? ` / ${provenance.profile}` : ""
              }}</span
            >
          </div>
          <div v-if="provenance.worker_id" class="space-y-1">
            <span
              class="text-xs font-medium text-table-column-header uppercase tracking-wider"
              >Worker</span
            >
            <span class="block text-sm text-title">{{
              provenance.worker_id
            }}</span>
          </div>
          <div v-if="provenance.created_by" class="space-y-1">
            <span
              class="text-xs font-medium text-table-column-header uppercase tracking-wider"
              >Created By</span
            >
            <span class="block text-sm text-title">{{
              provenance.created_by
            }}</span>
          </div>
          <div v-if="provenance.parameters_hash" class="space-y-1">
            <span
              class="text-xs font-medium text-table-column-header uppercase tracking-wider"
              >Parameters Hash</span
            >
            <span class="block text-sm text-title"
              ><code>{{
                provenance.parameters_hash.substring(0, 16)
              }}...</code></span
            >
          </div>
        </div>

        <div
          v-if="provenance.input_artifact_ids?.length"
          class="mt-6"
        >
          <p class="text-sm font-semibold text-title mb-1">
            Input Artifacts
          </p>
          <p class="text-xs text-definition-list-term mb-2">
            Artifacts used as input to the producing job.
          </p>
          <ul class="flex flex-wrap gap-2">
            <li
              v-for="inputArtifact in provenanceInputArtifacts"
              :key="inputArtifact.id"
            >
              <HpcPill :to="`/artifacts/${inputArtifact.id}`">
                <template v-if="inputArtifact.name && inputArtifact.id">
                  <span>{{ inputArtifact.name }}</span>
                  <code class="font-mono text-[0.85em] text-definition-list-term"
                    >[{{ shortId(inputArtifact.id) }}]</code
                  >
                </template>
                <template v-else>
                  {{ inputArtifactChipLabel(inputArtifact) }}
                </template>
              </HpcPill>
            </li>
          </ul>
        </div>
      </section>

      <section
        v-if="hasExtraMetadata"
        class="bg-form rounded-lg border border-color-theme p-6"
      >
        <div class="mb-4">
          <p class="text-lg font-semibold text-title">Metadata</p>
          <p class="text-sm text-definition-list-term">
            Additional metadata stored with this artifact.
          </p>
        </div>
        <pre
          class="bg-code-output border border-color-theme rounded-lg p-4 text-sm font-mono text-code-output overflow-x-auto"
        ><code>{{ formatJson(extraMetadata) }}</code></pre>
      </section>

      <section class="bg-form rounded-lg border border-color-theme">
        <div
          class="flex items-center justify-between p-4 border-b border-color-theme"
        >
          <div>
            <p class="text-sm font-semibold text-title">Files</p>
            <p class="text-xs text-definition-list-term">
              Content files stored in this artifact.
            </p>
          </div>
          <span class="text-xs text-definition-list-term">{{ files.length }} files</span>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-sm text-table-row">
            <thead>
              <tr class="border-b border-color-theme">
                <th class="px-4 py-2 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  Path
                </th>
                <th class="px-4 py-2 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  SHA-256
                </th>
                <th class="px-4 py-2 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  Size
                </th>
                <th class="px-4 py-2 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  Content-Type
                </th>
                <th class="px-4 py-2 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"></th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="f in files"
                :key="f.id"
                class="border-b border-color-theme"
              >
                <td class="px-4 py-2">
                  <code class="text-xs">{{ f.path }}</code>
                </td>
                <td class="px-4 py-2">
                  <code v-if="f.sha256" class="text-xs"
                    >{{ f.sha256?.substring(0, 12) }}...</code
                  >
                  <span v-else class="text-definition-list-term">-</span>
                </td>
                <td class="px-4 py-2">{{ formatSize(f.size_bytes) }}</td>
                <td class="px-4 py-2">{{ f.content_type || "-" }}</td>
                <td class="px-4 py-2">
                  <Button
                    v-if="
                      artifact.residence === 'managed' &&
                      artifact.status === 'COMMITTED'
                    "
                    type="outline"
                    size="tiny"
                    @click="onDownload(f.path)"
                  >
                    Download
                  </Button>
                  <span
                    v-else-if="artifact.residence === 'posix'"
                    class="text-definition-list-term text-xs"
                  >
                    {{ artifact.content_url }}/{{ f.path }}
                  </span>
                </td>
              </tr>
              <tr v-if="!files.length">
                <td colspan="5" class="px-4 py-8 text-center text-definition-list-term">
                  No files
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
    <div
      v-else
      class="bg-yellow-200/20 border border-yellow-200/40 text-yellow-800 p-4 rounded-lg"
    >
      Artifact not found.
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRoute, navigateTo } from "#app/composables/router";
import {
  fetchArtifactDetail,
  fetchArtifactSummary,
  downloadArtifactFile,
  deleteArtifact,
} from "../../composables/useHpcApi";
import type {
  ArtifactFileRow,
  ArtifactSummary,
  NormalizedArtifact,
} from "../../composables/useArtifactsApi";
import { formatDate } from "../../utils/jobs";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import Message from "../../../../tailwind-components/app/components/Message.vue";
import HpcPill from "../../components/HpcPill.vue";

const route = useRoute();
const id = computed(() => route.params.id as string);

const artifact = ref<NormalizedArtifact | null>(null);
const files = ref<ArtifactFileRow[]>([]);
const provenanceInputArtifacts = ref<ArtifactSummary[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const deleting = ref(false);

const PROVENANCE_KEYS = new Set([
  "job_id",
  "processor",
  "profile",
  "worker_id",
  "artifact_role",
  "created_by",
  "input_artifact_ids",
  "parameters_hash",
]);

function toErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error ?? "Unknown error");
}

const provenance = computed(() => {
  const meta = artifact.value?.metadata;
  if (!meta || typeof meta !== "object" || !meta.job_id) return null;
  return meta;
});

const extraMetadata = computed(() => {
  const meta = artifact.value?.metadata;
  if (!meta || typeof meta !== "object") return meta;
  const extra: Record<string, unknown> = {};
  for (const [k, v] of Object.entries(meta)) {
    if (!PROVENANCE_KEYS.has(k)) extra[k] = v;
  }
  return extra;
});

const hasExtraMetadata = computed(() => {
  const extra = extraMetadata.value;
  if (!extra) return false;
  if (typeof extra === "object") return Object.keys(extra).length > 0;
  return true;
});

function shortId(idVal: string): string {
  return idVal?.substring?.(0, 8) || idVal || "-";
}

function inputArtifactChipLabel(input: ArtifactSummary | null): string {
  if (!input) return "-";
  if (input.name && input.id) return `${input.name} [${shortId(input.id)}]`;
  if (input.name) return input.name;
  if (input.id) return shortId(input.id);
  return "-";
}

function formatSize(bytes: number | null | undefined): string {
  if (bytes == null) return "-";
  const n = Number(bytes);
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  return `${(n / (1024 * 1024)).toFixed(1)} MB`;
}

function formatJson(val: unknown): string {
  if (!val) return "";
  if (typeof val === "string") {
    try {
      return JSON.stringify(JSON.parse(val), null, 2);
    } catch {
      return val;
    }
  }
  return JSON.stringify(val, null, 2);
}

async function onDownload(filePath: string) {
  try {
    await downloadArtifactFile(id.value, filePath);
  } catch (e: unknown) {
    error.value = toErrorMessage(e);
  }
}

async function onDelete() {
  if (!confirm(`Delete artifact ${id.value}?`)) return;
  deleting.value = true;
  try {
    await deleteArtifact(id.value);
    navigateTo("/artifacts");
  } catch (e: unknown) {
    error.value = toErrorMessage(e);
  } finally {
    deleting.value = false;
  }
}

onMounted(async () => {
  try {
    const result = await fetchArtifactDetail(id.value);
    artifact.value = result.artifact;
    files.value = result.files;

    const inputIds = Array.isArray(result.artifact?.metadata?.input_artifact_ids)
      ? result.artifact.metadata.input_artifact_ids.filter(
          (value: unknown): value is string =>
            typeof value === "string" && value.trim().length > 0
        )
      : [];
    provenanceInputArtifacts.value = await Promise.all(
      inputIds.map(async (inputId) => (await fetchArtifactSummary(inputId)) ?? { id: inputId })
    );
  } catch (e: unknown) {
    error.value = toErrorMessage(e);
  } finally {
    loading.value = false;
  }
});
</script>
