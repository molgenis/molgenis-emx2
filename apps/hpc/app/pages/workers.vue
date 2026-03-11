<template>
  <div class="space-y-4">
    <section class="bg-form rounded-lg border border-color-theme p-6">
      <div
        class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between"
      >
        <div>
          <p class="text-lg font-semibold text-title">Workers</p>
          <p class="text-sm text-definition-list-term">
            Monitor worker availability and manage worker credentials.
          </p>
        </div>

        <div class="flex flex-wrap items-center gap-2">
          <span
            class="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-content text-sm text-title"
          >
            <strong>Registered</strong>
            {{ workers.length }}
          </span>
          <span
            class="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs"
            :class="
              refreshing
                ? 'bg-blue-500/10 text-blue-700 border border-blue-500/30'
                : 'bg-content text-definition-list-term'
            "
          >
            {{ refreshing ? "Refreshing..." : "Auto-refresh: 15s" }}
          </span>
          <button
            class="px-3 py-1.5 text-sm font-medium bg-button-primary text-button-primary border border-button-primary rounded-md hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
            @click="showBootstrap = !showBootstrap"
          >
            {{ showBootstrap ? "Close Add Worker" : "+ Add Worker" }}
          </button>
        </div>
      </div>

      <div
        v-if="showBootstrap"
        class="mt-5 rounded-lg border border-color-theme bg-content/40 p-4"
      >
        <div class="mb-3">
          <p class="text-sm font-semibold text-title">Add Worker Credential</p>
          <p class="text-xs text-definition-list-term">
            Issue or rotate a credential for a worker before the daemon first
            registers.
          </p>
        </div>

        <form
          class="grid grid-cols-1 gap-3 md:grid-cols-3 md:items-end"
          @submit.prevent="onBootstrapIssue"
        >
          <label class="flex flex-col gap-1">
            <span
              class="text-xs text-table-column-header uppercase tracking-wider"
              >Worker ID</span
            >
            <input
              v-model="bootstrapWorkerId"
              type="text"
              autocomplete="off"
              class="rounded-md border border-input bg-input text-input px-3 py-2 text-sm focus:border-input-focused focus:ring-1 focus:ring-blue-500"
              placeholder="e.g. hpc-headnode-01"
            />
          </label>

          <label class="flex flex-col gap-1">
            <span
              class="text-xs text-table-column-header uppercase tracking-wider"
              >Label (Optional)</span
            >
            <input
              v-model="bootstrapLabel"
              type="text"
              autocomplete="off"
              class="rounded-md border border-input bg-input text-input px-3 py-2 text-sm focus:border-input-focused focus:ring-1 focus:ring-blue-500"
              placeholder="optional label"
            />
          </label>

          <div class="flex gap-2">
            <button
              type="submit"
              class="px-3 py-2 text-sm border border-color-theme rounded-md hover:bg-hover disabled:opacity-50"
              :disabled="bootstrapBusy"
            >
              Issue
            </button>
            <button
              type="button"
              class="px-3 py-2 text-sm border border-color-theme rounded-md hover:bg-hover disabled:opacity-50"
              :disabled="bootstrapBusy"
              @click="onBootstrapRotate"
            >
              Rotate
            </button>
          </div>
        </form>

        <p class="mt-3 text-xs text-definition-list-term">
          Requires <code>_SYSTEM_.MOLGENIS_HPC_CREDENTIALS_KEY</code>.
          Capability and heartbeat fields appear after successful
          <code>/workers/register</code>.
        </p>
      </div>
    </section>

    <div
      v-if="error"
      class="bg-red-500/10 border border-red-500/20 text-red-700 p-4 rounded-lg"
    >
      {{ error }}
    </div>

    <section
      v-if="revealedSecret"
      class="bg-green-500/10 border border-green-500/20 text-green-800 p-4 rounded-lg"
    >
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p class="text-sm font-semibold text-title">Credential Issued</p>
          <p class="text-xs text-definition-list-term mt-1">
            Worker: <code>{{ revealedSecret.workerId }}</code> (shown once)
          </p>
        </div>
        <div class="flex items-center gap-2">
          <button
            class="px-2.5 py-1.5 text-xs border border-color-theme rounded-md hover:bg-hover"
            @click="copyRevealedSecret"
          >
            Copy Secret
          </button>
          <button
            class="px-2.5 py-1.5 text-xs border border-color-theme rounded-md hover:bg-hover"
            @click="dismissRevealedSecret"
          >
            Dismiss
          </button>
        </div>
      </div>

      <code class="mt-3 block text-xs bg-form px-2 py-2 rounded break-all">
        {{ revealedSecret.secret }}
      </code>

      <details class="mt-3 rounded-md border border-color-theme bg-form p-3">
        <summary class="text-xs font-medium text-title cursor-pointer">
          Daemon setup commands
        </summary>
        <div class="mt-3 grid grid-cols-1 gap-3 lg:grid-cols-2">
          <div>
            <p class="text-xs text-definition-list-term mb-1">
              Write secret to <code>.secret</code>
            </p>
            <pre class="text-xs bg-content p-2 rounded overflow-x-auto">{{
              secretWriteSnippet(revealedSecret.secret)
            }}</pre>
          </div>
          <div>
            <p class="text-xs text-definition-list-term mb-1">
              Daemon config snippet
            </p>
            <pre class="text-xs bg-content p-2 rounded overflow-x-auto">{{
              daemonConfigSnippet(revealedSecret.workerId)
            }}</pre>
          </div>
        </div>
      </details>
    </section>

    <div
      v-if="loading && !workers.length"
      class="bg-form rounded-lg border border-color-theme p-6 text-center text-definition-list-term"
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
                Active Jobs
              </th>
              <th
                class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
              ></th>
            </tr>
          </thead>

          <tbody>
            <template v-for="worker in workers" :key="worker.worker_id">
              <tr
                class="border-b border-color-theme hover:bg-hover transition-colors"
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
                    <span
                      v-for="(capability, index) in worker.capabilities"
                      :key="`${worker.worker_id}-${index}`"
                      class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-content text-record-label"
                    >
                      {{ capability.processor }}:{{ capability.profile }}
                    </span>
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

                <td class="px-4 py-3 align-top min-w-[210px]">
                  <div v-if="worker.active_jobs?.length" class="space-y-1">
                    <div
                      v-for="activeJob in worker.active_jobs"
                      :key="activeJob.id"
                      class="flex items-center gap-2"
                    >
                      <NuxtLink
                        :to="`/jobs/${activeJob.id}`"
                        class="text-xs font-mono text-button-outline hover:text-button-outline-hover underline underline-offset-2"
                      >
                        {{ shortId(activeJob.id) }}
                      </NuxtLink>
                      <StatusBadge :status="activeJob.status" />
                    </div>
                  </div>
                  <span v-else class="text-definition-list-term">-</span>
                </td>

                <td class="px-4 py-3 align-top whitespace-nowrap">
                  <div class="flex items-center gap-2">
                    <button
                      class="px-2 py-1 text-xs border border-color-theme rounded-md hover:bg-hover disabled:opacity-50"
                      :disabled="credentialsLoadingFor === worker.worker_id"
                      @click="toggleCredentials(worker.worker_id)"
                    >
                      {{
                        expandedWorkerId === worker.worker_id
                          ? "Hide"
                          : "Manage"
                      }}
                    </button>
                    <button
                      class="px-2 py-1 text-xs border border-red-500 text-red-700 rounded-md hover:bg-red-500/10 disabled:opacity-50"
                      :disabled="deletingWorker === worker.worker_id"
                      @click="onDeleteWorker(worker.worker_id)"
                    >
                      Remove
                    </button>
                  </div>
                </td>
              </tr>

              <tr
                v-if="expandedWorkerId === worker.worker_id"
                class="border-b border-color-theme bg-content/40"
              >
                <td colspan="7" class="px-4 py-4">
                  <div
                    class="flex flex-wrap items-start justify-between gap-3 mb-3"
                  >
                    <div>
                      <p class="text-sm font-semibold text-title">
                        Credentials
                      </p>
                      <p class="text-xs text-definition-list-term">
                        Capabilities are read-only and reported by the worker at
                        registration.
                      </p>
                    </div>
                    <div class="flex flex-wrap items-center gap-2">
                      <button
                        class="px-2.5 py-1.5 text-xs border border-color-theme rounded-md hover:bg-hover disabled:opacity-50"
                        :disabled="credentialActionFor === worker.worker_id"
                        @click="onIssueCredential(worker.worker_id)"
                      >
                        Issue
                      </button>
                      <button
                        class="px-2.5 py-1.5 text-xs border border-color-theme rounded-md hover:bg-hover disabled:opacity-50"
                        :disabled="credentialActionFor === worker.worker_id"
                        @click="onRotateCredential(worker.worker_id)"
                      >
                        Rotate
                      </button>
                      <button
                        class="px-2.5 py-1.5 text-xs border border-color-theme rounded-md hover:bg-hover disabled:opacity-50"
                        :disabled="credentialsLoadingFor === worker.worker_id"
                        @click="loadCredentials(worker.worker_id, true)"
                      >
                        Refresh
                      </button>
                    </div>
                  </div>

                  <div
                    v-if="credentialsLoadingFor === worker.worker_id"
                    class="rounded-md border border-color-theme bg-form p-3 text-xs text-definition-list-term"
                  >
                    Loading credentials...
                  </div>

                  <div
                    v-else-if="!credentialsByWorker[worker.worker_id]?.length"
                    class="rounded-md border border-color-theme bg-form p-3 text-xs text-definition-list-term"
                  >
                    No credentials issued.
                  </div>

                  <div v-else class="space-y-2">
                    <div
                      v-for="credential in credentialsByWorker[
                        worker.worker_id
                      ]"
                      :key="credential.id"
                      class="rounded-md border border-color-theme bg-form p-3"
                    >
                      <div
                        class="flex flex-wrap items-center justify-between gap-2"
                      >
                        <div class="flex items-center gap-2 text-xs">
                          <code class="bg-content px-1.5 py-0.5 rounded">{{
                            credential.id
                          }}</code>
                          <span
                            class="inline-flex items-center px-2 py-0.5 rounded-full border"
                            :class="credentialStatusClass(credential.status)"
                          >
                            {{ credential.status }}
                          </span>
                          <span class="text-definition-list-term">
                            Created {{ formatDate(credential.created_at) }}
                          </span>
                        </div>

                        <button
                          v-if="credential.status === 'ACTIVE'"
                          class="px-2 py-1 text-xs border border-red-500 text-red-700 rounded-md hover:bg-red-500/10 disabled:opacity-50"
                          :disabled="credentialActionFor === worker.worker_id"
                          @click="
                            onRevokeCredential(worker.worker_id, credential.id)
                          "
                        >
                          Revoke
                        </button>
                      </div>
                    </div>
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
import { ref, onMounted, onUnmounted } from "vue";
import {
  fetchWorkers,
  deleteWorker,
  fetchWorkerCredentials,
  issueWorkerCredential,
  rotateWorkerCredential,
  revokeWorkerCredential,
} from "../composables/useHpcApi";
import { formatDate } from "../utils/jobs";

const workers = ref<any[]>([]);
const loading = ref(false);
const refreshing = ref(false);
const error = ref<string | null>(null);
const deletingWorker = ref<string | null>(null);
const expandedWorkerId = ref<string | null>(null);
const credentialsByWorker = ref<Record<string, any[]>>({});
const credentialsLoadingFor = ref<string | null>(null);
const credentialActionFor = ref<string | null>(null);
const revealedSecret = ref<{ workerId: string; secret: string } | null>(null);
const showBootstrap = ref(false);
const bootstrapWorkerId = ref("");
const bootstrapLabel = ref("");
const bootstrapBusy = ref(false);

let refreshInterval: ReturnType<typeof setInterval> | null = null;
let initialLoadDone = false;
type CredentialAction = "issue" | "rotate";

function toErrorMessage(e: any): string {
  const detail =
    e?.data?.detail || e?.response?._data?.detail || e?.response?.data?.detail;
  const title =
    e?.data?.title || e?.response?._data?.title || e?.response?.data?.title;
  const status =
    e?.statusCode ??
    e?.status ??
    e?.response?.status ??
    e?.response?._data?.status;

  if (detail) {
    return status ? `${status}${title ? ` ${title}` : ""}: ${detail}` : detail;
  }
  if (e?.message) return e.message;
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

function credentialStatusClass(status: string): string {
  if (status === "ACTIVE")
    return "border-green-500/40 bg-green-500/10 text-green-700";
  if (status === "REVOKED")
    return "border-red-500/40 bg-red-500/10 text-red-700";
  if (status === "EXPIRED")
    return "border-yellow-500/40 bg-yellow-500/10 text-yellow-800";
  return "border-color-theme bg-content text-definition-list-term";
}

function shortId(idVal: string): string {
  return idVal?.substring?.(0, 8) || idVal || "-";
}

function mergeWorkers(nextWorkers: any[]) {
  const previousById = new Map(
    workers.value.map((worker: any) => [worker.worker_id, worker])
  );
  workers.value = nextWorkers.map((nextWorker: any) => {
    const previous = previousById.get(nextWorker.worker_id);
    if (!previous) return nextWorker;
    Object.assign(previous, nextWorker);
    return previous;
  });
}

async function loadCredentials(workerId: string, force = false) {
  if (!force && credentialsByWorker.value[workerId]) return;
  credentialsLoadingFor.value = workerId;
  try {
    credentialsByWorker.value[workerId] = await fetchWorkerCredentials(
      workerId
    );
  } catch (e: any) {
    error.value = toErrorMessage(e);
  } finally {
    if (credentialsLoadingFor.value === workerId) {
      credentialsLoadingFor.value = null;
    }
  }
}

async function toggleCredentials(workerId: string) {
  if (expandedWorkerId.value === workerId) {
    expandedWorkerId.value = null;
    return;
  }
  expandedWorkerId.value = workerId;
  await loadCredentials(workerId);
}

function normalizeOptional(value: string): string | undefined {
  const trimmed = value.trim();
  return trimmed ? trimmed : undefined;
}

function promptCredentialLabel(action: CredentialAction): string | undefined {
  const suggestion = `${action}d-${new Date().toISOString()}`;
  const value = prompt("Credential label (optional):", suggestion);
  if (value === null) return undefined;
  return normalizeOptional(value);
}

async function runCredentialAction(
  workerId: string,
  action: CredentialAction,
  opts: { label?: string; interactive?: boolean } = {}
) {
  credentialActionFor.value = workerId;
  error.value = null;

  try {
    const label =
      opts.interactive === false
        ? opts.label
        : opts.label ?? promptCredentialLabel(action);

    const result =
      action === "issue"
        ? await issueWorkerCredential(workerId, { label })
        : await rotateWorkerCredential(workerId, { label });

    revealedSecret.value = { workerId, secret: result.secret };
    await loadCredentials(workerId, true);
  } catch (e: any) {
    error.value = toErrorMessage(e);
  } finally {
    credentialActionFor.value = null;
  }
}

async function onIssueCredential(workerId: string) {
  await runCredentialAction(workerId, "issue");
}

async function onRotateCredential(workerId: string) {
  await runCredentialAction(workerId, "rotate");
}

async function runBootstrapAction(action: CredentialAction) {
  const workerId = bootstrapWorkerId.value.trim();
  if (!workerId) {
    error.value = "Worker ID is required to issue or rotate a credential.";
    return;
  }

  bootstrapBusy.value = true;
  try {
    await runCredentialAction(workerId, action, {
      label: normalizeOptional(bootstrapLabel.value),
      interactive: false,
    });
    showBootstrap.value = false;
    await loadWorkers({ background: true });

    if (workers.value.find((worker) => worker.worker_id === workerId)) {
      expandedWorkerId.value = workerId;
      await loadCredentials(workerId, true);
    }
  } finally {
    bootstrapBusy.value = false;
  }
}

async function onBootstrapIssue() {
  await runBootstrapAction("issue");
}

async function onBootstrapRotate() {
  await runBootstrapAction("rotate");
}

async function onRevokeCredential(workerId: string, credentialId: string) {
  if (!confirm(`Revoke credential ${credentialId}?`)) return;

  credentialActionFor.value = workerId;
  try {
    await revokeWorkerCredential(workerId, credentialId);
    await loadCredentials(workerId, true);
  } catch (e: any) {
    error.value = toErrorMessage(e);
  } finally {
    credentialActionFor.value = null;
  }
}

async function copyRevealedSecret() {
  if (!revealedSecret.value) return;
  try {
    await navigator.clipboard.writeText(revealedSecret.value.secret);
  } catch {}
}

function dismissRevealedSecret() {
  revealedSecret.value = null;
}

function secretWriteSnippet(secret: string): string {
  return `printf '%s' '${secret}' > .secret && chmod 600 .secret`;
}

function daemonConfigSnippet(workerId: string): string {
  return [
    "emx2:",
    '  base_url: "https://emx2.example.org"',
    `  worker_id: "${workerId}"`,
    '  worker_secret_file: ".secret"',
  ].join("\n");
}

async function onDeleteWorker(workerId: string) {
  if (
    !confirm(
      `Remove worker "${workerId}"? Jobs assigned to this worker will retain their history.`
    )
  ) {
    return;
  }

  deletingWorker.value = workerId;
  try {
    await deleteWorker(workerId);
    await loadWorkers();
  } catch (e: any) {
    error.value = toErrorMessage(e);
  } finally {
    deletingWorker.value = null;
  }
}

async function loadWorkers({
  background = false,
}: { background?: boolean } = {}) {
  if (!initialLoadDone && !background) loading.value = true;
  if (background) refreshing.value = true;
  if (!background) error.value = null;

  try {
    const fetched = await fetchWorkers();
    mergeWorkers(fetched);
  } catch (e: any) {
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
