<template>
  <div class="space-y-4">
    <div
      v-if="loading"
      class="bg-form rounded-lg border border-color-theme p-6 text-center text-definition-list-term"
    >
      Loading job...
    </div>
    <div
      v-else-if="error"
      class="bg-red-500/10 border border-red-500/20 text-red-700 p-4 rounded-lg"
    >
      {{ error }}
    </div>
    <template v-else-if="job">
      <section class="bg-form rounded-lg border border-color-theme p-6">
        <div class="flex items-start justify-between mb-6">
          <div>
            <p class="text-lg font-semibold text-title">
              Job
              <code class="text-base bg-content px-1.5 py-0.5 rounded">{{
                shortId(job.id)
              }}</code>
            </p>
            <p class="text-sm text-definition-list-term">
              {{ job.processor
              }}{{ job.profile ? ` / ${job.profile}` : "" }} &bull; created
              {{ formatDate(job.created_at) }}
            </p>
          </div>
          <div class="flex items-center gap-2">
            <StatusBadge :status="job.status" />
            <button
              class="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm border border-red-500 text-red-700 rounded-md hover:bg-red-500/10 disabled:opacity-50"
              title="Delete job"
              :disabled="deleting"
              @click="onDelete"
            >
              <HpcIconTrash class="w-4 h-4" />
              <span>Delete</span>
            </button>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div class="space-y-1">
            <span
              class="text-xs font-medium text-table-column-header uppercase tracking-wider"
              >Job ID</span
            >
            <span class="block text-sm text-title"
              ><code>{{ job.id }}</code></span
            >
          </div>
          <div class="space-y-1">
            <span
              class="text-xs font-medium text-table-column-header uppercase tracking-wider"
              >Submitted By</span
            >
            <span class="block text-sm text-title">{{
              job.submit_user || "-"
            }}</span>
          </div>
          <div class="space-y-1">
            <span
              class="text-xs font-medium text-table-column-header uppercase tracking-wider"
              >Processor</span
            >
            <span class="block text-sm text-title">{{
              job.processor || "-"
            }}</span>
          </div>
          <div class="space-y-1">
            <span
              class="text-xs font-medium text-table-column-header uppercase tracking-wider"
              >Profile</span
            >
            <span class="block text-sm text-title">{{
              job.profile || "-"
            }}</span>
          </div>
          <div class="space-y-1">
            <span
              class="text-xs font-medium text-table-column-header uppercase tracking-wider"
              >Worker</span
            >
            <span class="block text-sm text-title">{{
              job.worker_id || "-"
            }}</span>
          </div>
          <div class="space-y-1">
            <span
              class="text-xs font-medium text-table-column-header uppercase tracking-wider"
              >Slurm Job ID</span
            >
            <span class="block text-sm text-title">{{
              job.slurm_job_id || "-"
            }}</span>
          </div>
        </div>

        <div v-if="normalizedInputs.length" class="mt-6">
          <p class="text-sm font-semibold text-title mb-1">Input Artifacts</p>
          <p class="text-xs text-definition-list-term mb-2">
            Artifacts referenced by this job at submission time.
          </p>
          <ul class="flex flex-wrap gap-2">
            <li
              v-for="(input, idx) in normalizedInputs"
              :key="input.id || `${input.name || 'input'}-${idx}`"
            >
              <NuxtLink
                v-if="input.id"
                :to="`/artifacts/${input.id}`"
                class="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-content text-sm text-title hover:bg-hover transition-colors"
              >
                {{ inputArtifactChipLabel(input) }}
              </NuxtLink>
              <span
                v-else
                class="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-content text-sm text-title"
              >
                {{ inputArtifactChipLabel(input) }}
              </span>
            </li>
          </ul>
        </div>

        <div v-if="job.parameters" class="mt-6">
          <p class="text-sm font-semibold text-title mb-1">Parameters</p>
          <p class="text-xs text-definition-list-term mb-2">
            JSON payload stored with the job submission.
          </p>
          <pre
            class="bg-code-output border border-color-theme rounded-lg p-4 text-sm font-mono text-code-output overflow-x-auto"
          ><code>{{ formatJson(job.parameters) }}</code></pre>
        </div>
      </section>

      <section
        v-if="job.output_artifact_id || job.log_artifact_id"
        class="grid grid-cols-1 md:grid-cols-2 gap-4"
      >
        <div
          v-if="job.output_artifact_id"
          class="bg-form rounded-lg border border-color-theme p-6"
        >
          <div class="flex items-start justify-between mb-4">
            <div>
              <p class="text-sm font-semibold text-title">Output Artifact</p>
              <p class="text-xs text-definition-list-term">
                Primary produced artifact for this run.
              </p>
            </div>
            <StatusBadge :status="job.output_artifact_id.status" />
          </div>
          <div class="divide-y divide-color-theme text-sm">
            <div class="flex justify-between py-2">
              <span class="text-definition-list-term">Name</span>
              <span class="text-title">{{
                job.output_artifact_id.name || "-"
              }}</span>
            </div>
            <div class="flex justify-between py-2">
              <span class="text-definition-list-term">ID</span>
              <code class="text-xs">{{
                shortId(job.output_artifact_id.id)
              }}</code>
            </div>
            <div class="flex justify-between py-2">
              <span class="text-definition-list-term">Type</span>
              <span class="text-title">{{
                job.output_artifact_id.type || "-"
              }}</span>
            </div>
            <div class="flex justify-between py-2">
              <span class="text-definition-list-term">Residence</span>
              <span class="text-title">{{
                job.output_artifact_id.residence || "-"
              }}</span>
            </div>
          </div>
          <div class="mt-4">
            <NuxtLink
              :to="`/artifacts/${job.output_artifact_id.id}`"
              class="inline-flex items-center gap-1 px-3 py-1.5 text-sm font-medium border border-button-outline text-button-outline rounded-md hover:bg-button-outline-hover hover:text-button-outline-hover transition-colors"
            >
              View Details
            </NuxtLink>
          </div>
        </div>

        <div
          v-if="job.log_artifact_id"
          class="bg-form rounded-lg border border-color-theme p-6"
        >
          <div class="flex items-start justify-between mb-4">
            <div>
              <p class="text-sm font-semibold text-title">Log Artifact</p>
              <p class="text-xs text-definition-list-term">
                Execution logs captured and uploaded by the worker.
              </p>
            </div>
            <StatusBadge :status="job.log_artifact_id.status" />
          </div>
          <div class="divide-y divide-color-theme text-sm">
            <div class="flex justify-between py-2">
              <span class="text-definition-list-term">Name</span>
              <span class="text-title">{{
                job.log_artifact_id.name || "-"
              }}</span>
            </div>
            <div class="flex justify-between py-2">
              <span class="text-definition-list-term">ID</span>
              <code class="text-xs">{{ shortId(job.log_artifact_id.id) }}</code>
            </div>
            <div class="flex justify-between py-2">
              <span class="text-definition-list-term">Type</span>
              <span class="text-title">{{
                job.log_artifact_id.type || "-"
              }}</span>
            </div>
            <div class="flex justify-between py-2">
              <span class="text-definition-list-term">Residence</span>
              <span class="text-title">{{
                job.log_artifact_id.residence || "-"
              }}</span>
            </div>
          </div>
          <div class="mt-4">
            <NuxtLink
              :to="`/artifacts/${job.log_artifact_id.id}`"
              class="inline-flex items-center gap-1 px-3 py-1.5 text-sm font-medium border border-button-outline text-button-outline rounded-md hover:bg-button-outline-hover hover:text-button-outline-hover transition-colors"
            >
              View Details
            </NuxtLink>
          </div>
        </div>
      </section>

      <section class="bg-form rounded-lg border border-color-theme">
        <div
          class="flex items-center justify-between p-4 border-b border-color-theme"
        >
          <div>
            <p class="text-sm font-semibold text-title">Transition History</p>
            <p class="text-xs text-definition-list-term">
              Lifecycle events recorded for this job.
            </p>
          </div>
          <span class="text-xs text-definition-list-term"
            >{{ transitions.length }} entries</span
          >
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-sm text-table-row">
            <thead>
              <tr class="border-b border-color-theme">
                <th
                  class="px-4 py-2 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  Time
                </th>
                <th
                  class="px-4 py-2 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  From
                </th>
                <th
                  class="px-4 py-2 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  To
                </th>
                <th
                  class="px-4 py-2 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  Progress
                </th>
                <th
                  class="px-4 py-2 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  Detail
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="t in transitions"
                :key="t.id"
                class="border-b border-color-theme hover:bg-hover transition-colors"
              >
                <td class="px-4 py-2">{{ formatDate(t.timestamp) }}</td>
                <td class="px-4 py-2">
                  <StatusBadge v-if="t.from_status" :status="t.from_status" />
                  <span v-else class="text-definition-list-term">-</span>
                </td>
                <td class="px-4 py-2">
                  <StatusBadge :status="t.to_status" />
                </td>
                <td class="px-4 py-2 min-w-[220px]">
                  <div v-if="hasTransitionProgress(t)" class="space-y-1">
                    <div
                      class="flex items-center justify-between gap-2 text-xs text-definition-list-term"
                    >
                      <span class="truncate">{{
                        transitionProgressSummary(t)
                      }}</span>
                      <span>{{ formatProgressPercent(t.progress) }}</span>
                    </div>
                    <div class="h-1.5 bg-content rounded overflow-hidden">
                      <div
                        class="h-full bg-blue-500 transition-all duration-300"
                        :style="{
                          width: `${Math.max(
                            0,
                            Math.min(100, Math.round((t.progress ?? 0) * 100))
                          )}%`,
                        }"
                      />
                    </div>
                  </div>
                  <span v-else class="text-definition-list-term">-</span>
                </td>
                <td class="px-4 py-2 align-top">
                  <p class="whitespace-pre-wrap break-words">
                    {{ displayDetail(t) }}
                  </p>
                  <button
                    v-if="canExpandDetail(t.detail)"
                    class="mt-1 text-xs text-button-outline hover:text-button-outline-hover underline underline-offset-2"
                    @click.stop="toggleDetail(t.id)"
                  >
                    {{ isDetailExpanded(t.id) ? "Collapse" : "Expand" }}
                  </button>
                </td>
              </tr>
              <tr v-if="!transitions.length">
                <td
                  colspan="5"
                  class="px-4 py-8 text-center text-definition-list-term"
                >
                  No transitions recorded
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
      Job not found.
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useRoute } from "#app/composables/router";
import { fetchJobDetail, deleteJob } from "../../composables/useHpcApi";
import { formatDate, formatProgressPercent } from "../../utils/jobs";
import { isTerminal } from "../../utils/protocol";
import { navigateTo } from "#app/composables/router";

const route = useRoute();
const id = computed(() => route.params.id as string);

const job = ref<any>(null);
const transitions = ref<any[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const deleting = ref(false);
const expandedTransitionDetails = ref<Record<string, boolean>>({});
const DETAIL_PREVIEW_LIMIT = 180;
let refreshInterval: ReturnType<typeof setInterval> | null = null;

const normalizedInputs = computed(() => {
  const inputs = job.value?.inputs;
  if (!inputs || !Array.isArray(inputs)) return [];
  return inputs.map((item: any) => {
    if (typeof item === "string") return { id: item };
    return item;
  });
});

async function onDelete() {
  if (!confirm(`Delete job ${id.value}?`)) return;
  deleting.value = true;
  try {
    await deleteJob(id.value);
    navigateTo("/");
  } catch (e: any) {
    error.value = e.message;
  } finally {
    deleting.value = false;
  }
}

function shortId(idVal: string): string {
  return idVal?.substring?.(0, 8) || idVal || "-";
}

function inputArtifactChipLabel(input: any): string {
  if (!input) return "-";
  if (input.name) return input.name;
  if (input.id) return shortId(input.id);
  if (typeof input === "string") return shortId(input);
  return "-";
}

function formatJson(val: any): string {
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

function canExpandDetail(detail?: string | null): boolean {
  return typeof detail === "string" && detail.length > DETAIL_PREVIEW_LIMIT;
}

function isDetailExpanded(transitionId: string): boolean {
  return !!expandedTransitionDetails.value[transitionId];
}

function toggleDetail(transitionId: string): void {
  expandedTransitionDetails.value[transitionId] =
    !expandedTransitionDetails.value[transitionId];
}

function displayDetail(transition: any): string {
  const detail = transition?.detail || "-";
  if (!canExpandDetail(detail) || isDetailExpanded(transition.id))
    return detail;
  return `${detail.slice(0, DETAIL_PREVIEW_LIMIT)}...`;
}

function hasTransitionProgress(transition: any): boolean {
  return (
    typeof transition?.progress === "number" ||
    typeof transition?.phase === "string" ||
    typeof transition?.message === "string"
  );
}

function transitionProgressSummary(transition: any): string {
  return transition.phase || transition.message || "In progress";
}

async function loadJobDetail() {
  try {
    const result = await fetchJobDetail(id.value);
    job.value = result.job;
    transitions.value = result.transitions;
  } catch (e: any) {
    error.value = e.message;
  }
}

onMounted(async () => {
  await loadJobDetail();
  loading.value = false;
  refreshInterval = setInterval(async () => {
    if (!job.value || isTerminal(job.value.status)) return;
    await loadJobDetail();
  }, 10000);
});

onUnmounted(() => {
  if (refreshInterval) clearInterval(refreshInterval);
});
</script>
