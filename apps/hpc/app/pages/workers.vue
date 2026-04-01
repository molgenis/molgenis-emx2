<template>
  <div class="space-y-4">
    <section class="bg-form rounded-lg border border-color-theme p-6">
      <div class="flex items-start justify-between">
        <div>
          <p class="text-lg font-semibold text-title">Workers</p>
          <p class="text-sm text-definition-list-term flex items-center gap-3">
            <span>
              Monitor worker availability and manage worker credentials.
            </span>
            <span class="text-[11px] tracking-wide opacity-70">
              {{ refreshing ? "Refreshing now" : "Auto-refresh every 15 seconds" }}
            </span>
          </p>
        </div>

        <div class="flex items-center gap-3">
          <span class="text-sm font-medium text-title">
            Registered {{ workers.length }}
          </span>
          <Button
            type="primary"
            size="small"
            @click="showBootstrap = !showBootstrap"
          >
            {{ showBootstrap ? "Hide Form" : "+ Add Worker" }}
          </Button>
        </div>
      </div>
    </section>

    <section
      v-if="showBootstrap"
      class="bg-form rounded-lg border border-color-theme p-5"
    >
      <div class="mb-4">
        <p class="text-sm font-semibold text-title">Add Worker Credential</p>
        <p class="text-xs text-definition-list-term">
          Create or replace a worker secret before daemon registration.
        </p>
      </div>

      <form class="space-y-4" @submit.prevent="onBootstrapIssue">
        <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
          <label class="flex flex-col gap-1">
            <span
              class="text-xs text-table-column-header uppercase tracking-wider"
              >Worker ID</span
            >
            <InputString
              id="bootstrap-worker-id"
              v-model="bootstrapWorkerId"
              autocomplete="off"
              placeholder="e.g. hpc-headnode-01"
            />
          </label>

          <label class="flex flex-col gap-1">
            <span
              class="text-xs text-table-column-header uppercase tracking-wider"
              >Label (Optional)</span
            >
            <InputString
              id="bootstrap-worker-label"
              v-model="bootstrapLabel"
              autocomplete="off"
              placeholder="optional label"
            />
          </label>
        </div>

        <div
          class="flex flex-col gap-3 border-t border-color-theme pt-3 sm:flex-row sm:items-center sm:justify-between"
        >
          <p class="text-xs text-definition-list-term">
            Requires <code>_SYSTEM_.MOLGENIS_HPC_CREDENTIALS_KEY</code>.
            Capabilities and heartbeat appear after
            <code>/workers/register</code>.
          </p>
          <div class="flex gap-2 sm:shrink-0">
            <Button type="primary" size="small" :disabled="bootstrapBusy"
              >Issue</Button
            >
          </div>
        </div>
      </form>
    </section>

    <Message v-if="error" id="workers-page-error" invalid>
      {{ error }}
    </Message>

    <WorkerManageModal
      v-if="managingWorkerId"
      v-model:visible="showManageModal"
      :worker-id="managingWorkerId"
      :active-jobs="managingWorkerJobs"
    />

    <div
      v-if="loading && !workers.length"
      class="bg-form rounded-lg border border-color-theme p-6 text-sm text-definition-list-term"
    >
      Loading workers...
    </div>

    <div
      v-else-if="!workers.length"
      class="bg-form rounded-lg border border-color-theme p-6 text-center"
    >
      <p class="text-title font-medium">No workers registered</p>
      <p class="text-sm text-definition-list-term mt-1">
        Add a worker credential, then start the daemon to register it.
      </p>
    </div>

    <section v-else class="bg-form rounded-lg border border-color-theme">
      <div class="overflow-x-auto">
        <table class="w-full text-sm text-table-row">
          <thead>
            <tr class="border-b border-color-theme">
              <th
                class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
              >
                Worker
              </th>
              <th
                class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
              >
                Hostname
              </th>
              <th
                class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
              >
                Capabilities
              </th>
              <th
                class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
              >
                Registered
              </th>
              <th
                class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
              >
                Heartbeat
              </th>
              <th
                class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
              >
                Actions
              </th>
            </tr>
          </thead>

          <tbody>
            <template v-for="worker in workers" :key="worker.worker_id">
              <tr
                class="border-b border-color-theme transition-colors hover:bg-hover"
              >
                <td class="px-4 py-3 align-top">
                  <code class="text-xs bg-content px-1.5 py-0.5 rounded">{{
                    worker.worker_id
                  }}</code>
                </td>

                <td class="px-4 py-3 align-top">
                  {{ worker.hostname || "-" }}
                </td>

                <td class="px-4 py-3 align-top">
                  <div
                    v-if="worker.capabilities?.length"
                    class="flex flex-wrap gap-1"
                  >
                    <HpcPill
                      v-for="(capability, index) in worker.capabilities"
                      :key="`${worker.worker_id}-${index}`"
                      compact
                    >
                      {{ capability.processor }}:{{ capability.profile }}
                    </HpcPill>
                  </div>
                  <span v-else class="text-definition-list-term">-</span>
                </td>

                <td class="px-4 py-3 align-top">
                  {{ formatDate(worker.registered_at) }}
                </td>

                <td class="px-4 py-3 align-top">
                  <div class="flex items-center gap-2">
                    <span
                      class="h-2 w-2 rounded-full"
                      :class="heartbeatDotClass(worker.last_heartbeat_at)"
                    />
                    <span :class="heartbeatTextClass(worker.last_heartbeat_at)">
                      {{ formatDate(worker.last_heartbeat_at) }}
                    </span>
                  </div>
                </td>

                <td class="px-4 py-3 align-top whitespace-nowrap">
                  <div class="flex items-center gap-2">
                    <Button
                      type="outline"
                      size="tiny"
                      @click="openManageModal(worker)"
                    >
                      Manage
                    </Button>
                    <Button
                      type="outline"
                      size="tiny"
                      :disabled="deletingWorker === worker.worker_id"
                      @click="onDeleteWorker(worker.worker_id)"
                    >
                      Remove
                    </Button>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import Button from "../../../tailwind-components/app/components/Button.vue";
import InputString from "../../../tailwind-components/app/components/input/String.vue";
import Message from "../../../tailwind-components/app/components/Message.vue";
import HpcPill from "../components/HpcPill.vue";
import WorkerManageModal from "../components/WorkerManageModal.vue";
import {
	deleteWorker,
	fetchWorkers,
	issueWorkerCredential,
	type WorkerSummary,
} from "../composables/useHpcApi";
import { formatDate } from "../utils/jobs";

const workers = ref<WorkerSummary[]>([]);
const loading = ref(false);
const refreshing = ref(false);
const error = ref<string | null>(null);
const deletingWorker = ref<string | null>(null);
const showBootstrap = ref(false);
const bootstrapWorkerId = ref("");
const bootstrapLabel = ref("");
const bootstrapBusy = ref(false);

// Manage modal state
const showManageModal = ref(false);
const managingWorkerId = ref<string | null>(null);
const managingWorkerJobs = ref<Array<{ id: string; status: string }>>([]);

function openManageModal(worker: WorkerSummary) {
  managingWorkerId.value = worker.worker_id;
  managingWorkerJobs.value = (worker.active_jobs ?? []).map((j) => ({
    id: j.id,
    status: j.status,
  }));
  showManageModal.value = true;
}

let refreshInterval: ReturnType<typeof setInterval> | null = null;
let initialLoadDone = false;

function toErrorMessage(e: unknown): string {
	const errorLike = e as {
		data?: { detail?: string; title?: string };
		response?: {
			_data?: { detail?: string; title?: string; status?: number };
			data?: { detail?: string; title?: string };
			status?: number;
		};
		statusCode?: number;
		status?: number;
		message?: string;
	};
	const detail =
		errorLike?.data?.detail ||
		errorLike?.response?._data?.detail ||
		errorLike?.response?.data?.detail;
	const title =
		errorLike?.data?.title ||
		errorLike?.response?._data?.title ||
		errorLike?.response?.data?.title;
	const status =
		errorLike?.statusCode ??
		errorLike?.status ??
		errorLike?.response?.status ??
		errorLike?.response?._data?.status;

	if (detail) {
		return status ? `${status}${title ? ` ${title}` : ""}: ${detail}` : detail;
	}
	if (errorLike?.message) return errorLike.message;
	return "Request failed";
}

function heartbeatAgeMs(ts: string | null): number {
	if (!ts) return Number.POSITIVE_INFINITY;
	const parsed = new Date(ts).getTime();
	if (Number.isNaN(parsed)) return Number.POSITIVE_INFINITY;
	return Date.now() - parsed;
}

function heartbeatDotClass(ts: string | null): string {
	return heartbeatAgeMs(ts) > 5 * 60 * 1000 ? "bg-red-500" : "bg-green-500";
}

function heartbeatTextClass(ts: string | null): string {
	if (!ts) return "text-definition-list-term";
	return heartbeatAgeMs(ts) > 5 * 60 * 1000 ? "text-red-700" : "text-green-600";
}

function mergeWorkers(nextWorkers: WorkerSummary[]) {
	const previousById = new Map(
		workers.value.map((worker) => [worker.worker_id, worker]),
	);
	workers.value = nextWorkers.map((nextWorker) => {
		const previous = previousById.get(nextWorker.worker_id);
		if (!previous) return nextWorker;
		return { ...previous, ...nextWorker };
	});
}

function normalizeOptional(value: string): string | undefined {
	const trimmed = value.trim();
	return trimmed ? trimmed : undefined;
}

async function runBootstrapIssue() {
	const workerId = bootstrapWorkerId.value.trim();
	if (!workerId) {
		error.value = "Worker ID is required to issue a credential.";
		return;
	}

	bootstrapBusy.value = true;
	try {
		const label = normalizeOptional(bootstrapLabel.value);
		await issueWorkerCredential(workerId, { label });
		showBootstrap.value = false;
		await loadWorkers({ background: true });
		// Open manage modal so user can see the credential
		managingWorkerId.value = workerId;
		managingWorkerJobs.value = [];
		showManageModal.value = true;
	} catch (e: unknown) {
		error.value = toErrorMessage(e);
	} finally {
		bootstrapBusy.value = false;
	}
}

async function onBootstrapIssue() {
	await runBootstrapIssue();
}

async function onDeleteWorker(workerId: string) {
	if (
		!confirm(
			`Remove worker "${workerId}"? Jobs assigned to this worker will retain their history.`,
		)
	) {
		return;
	}

	deletingWorker.value = workerId;
	try {
		await deleteWorker(workerId);
		await loadWorkers();
	} catch (e: unknown) {
		error.value = toErrorMessage(e);
	} finally {
		deletingWorker.value = null;
	}
}

async function loadWorkers({
	background = false,
}: {
	background?: boolean;
} = {}) {
	if (!initialLoadDone && !background) loading.value = true;
	if (background) refreshing.value = true;
	if (!background) error.value = null;

	try {
		const fetched = await fetchWorkers();
		mergeWorkers(fetched);
	} catch (e: unknown) {
		error.value = toErrorMessage(e);
	} finally {
		if (!initialLoadDone) {
			loading.value = false;
			initialLoadDone = true;
		}
		if (background) refreshing.value = false;
	}
}

onMounted(() => {
	loadWorkers();
	refreshInterval = setInterval(() => loadWorkers({ background: true }), 15000);
});

onUnmounted(() => {
	if (refreshInterval) clearInterval(refreshInterval);
});
</script>
