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
          <Button
            type="primary"
            size="tiny"
            @click="showBootstrap = !showBootstrap"
          >
            {{ showBootstrap ? "Close Add Worker" : "+ Add Worker" }}
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
            <Button
              type="outline"
              size="small"
              :disabled="bootstrapBusy"
              @click="onBootstrapRotate"
            >
              Rotate
            </Button>
          </div>
        </div>
      </form>
    </section>

    <Message v-if="error" id="workers-page-error" invalid>
      {{ error }}
    </Message>

    <section
      v-if="revealedSecret"
      class="bg-form rounded-lg border border-color-theme p-4 space-y-3"
    >
      <div
        class="flex flex-col gap-2 md:flex-row md:items-start md:justify-between"
      >
        <div>
          <p class="text-sm font-semibold text-title">Credential Issued</p>
          <p class="text-xs text-definition-list-term">
            Worker <code>{{ revealedSecret.workerId }}</code> • shown once
          </p>
        </div>
        <Button type="outline" size="tiny" @click="dismissRevealedSecret"
          >Dismiss</Button
        >
      </div>

      <div class="rounded-md border border-color-theme bg-content p-3">
        <div
          class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between"
        >
          <div class="min-w-0">
            <p
              class="text-[11px] font-semibold text-table-column-header uppercase tracking-wider mb-1"
            >
              Secret
            </p>
            <code class="block text-xs leading-relaxed break-all">{{
              revealedSecret.secret
            }}</code>
          </div>
          <Button
            type="primary"
            size="tiny"
            class="sm:shrink-0"
            @click="copySecretValue"
          >
            {{ copyState === "secret" ? "Copied" : "Copy Secret" }}
          </Button>
        </div>
      </div>

      <p class="text-xs text-definition-list-term">
        Store this secret now. It cannot be retrieved later.
      </p>

      <div class="grid grid-cols-1 gap-3 lg:grid-cols-2">
        <div class="rounded-md border border-color-theme bg-content p-3">
          <div class="flex items-start justify-between gap-2 mb-2">
            <p
              class="text-xs text-table-column-header font-semibold uppercase tracking-wider"
            >
              Write `.secret`
            </p>
            <Button type="outline" size="tiny" @click="copyWriteSecretCommand">
              {{ copyState === "write" ? "Copied" : "Copy" }}
            </Button>
          </div>
          <pre class="text-xs whitespace-pre-wrap break-all leading-relaxed">{{
            secretWriteSnippet(revealedSecret.secret)
          }}</pre>
        </div>

        <div class="rounded-md border border-color-theme bg-content p-3">
          <div class="flex items-start justify-between gap-2 mb-2">
            <p
              class="text-xs text-table-column-header font-semibold uppercase tracking-wider"
            >
              Daemon Config
            </p>
            <Button type="outline" size="tiny" @click="copyDaemonConfig">
              {{ copyState === "config" ? "Copied" : "Copy" }}
            </Button>
          </div>
          <pre class="text-xs whitespace-pre-wrap break-all leading-relaxed">{{
            daemonConfigSnippet(revealedSecret.workerId)
          }}</pre>
        </div>
      </div>

      <p v-if="copyMessage" class="text-xs text-definition-list-term">
        {{ copyMessage }}
      </p>
    </section>

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
                :class="[
                  'border-b border-color-theme transition-colors',
                  expandedWorkerId === worker.worker_id
                    ? 'bg-content/40'
                    : 'hover:bg-hover',
                ]"
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
                    <Button
                      type="outline"
                      size="tiny"
                      :disabled="credentialsLoadingFor === worker.worker_id"
                      @click="toggleCredentials(worker.worker_id)"
                    >
                      {{
                        expandedWorkerId === worker.worker_id
                          ? "Close"
                          : "Manage"
                      }}
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

              <tr
                v-if="expandedWorkerId === worker.worker_id"
                class="border-b border-color-theme last:border-b-0 bg-content/40"
              >
                <td class="px-4 py-4 lg:px-5" colspan="7">
                  <div
                    class="flex flex-wrap items-start justify-between gap-3 mb-4"
                  >
                    <div>
                      <p class="text-sm font-semibold text-title">
                        Manage Worker <code>{{ worker.worker_id }}</code>
                      </p>
                      <p class="text-xs text-definition-list-term mt-1">
                        Issue creates the first credential when none exists.
                        Revoke active credentials in the table below.
                      </p>
                    </div>
                    <div class="flex flex-wrap items-center gap-2">
                      <Button
                        v-if="!hasActiveCredential(worker.worker_id)"
                        type="outline"
                        size="tiny"
                        :disabled="credentialActionFor === worker.worker_id"
                        @click="onIssueCredential(worker.worker_id)"
                      >
                        Issue
                      </Button>
                    </div>
                  </div>

                  <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
                    <div
                      class="rounded-md border border-color-theme bg-form p-3"
                    >
                      <p
                        class="text-xs font-semibold text-table-column-header uppercase tracking-wider mb-2"
                      >
                        Worker Info
                      </p>
                      <div class="space-y-2 text-sm">
                        <div class="flex justify-between gap-3">
                          <span class="text-definition-list-term"
                            >Hostname</span
                          >
                          <span class="text-title">{{
                            worker.hostname || "-"
                          }}</span>
                        </div>
                        <div class="flex justify-between gap-3">
                          <span class="text-definition-list-term"
                            >Registered</span
                          >
                          <span class="text-title">{{
                            formatDate(worker.registered_at)
                          }}</span>
                        </div>
                        <div class="flex justify-between gap-3">
                          <span class="text-definition-list-term"
                            >Heartbeat</span
                          >
                          <span
                            :class="
                              heartbeatTextClass(worker.last_heartbeat_at)
                            "
                          >
                            {{ formatDate(worker.last_heartbeat_at) }}
                          </span>
                        </div>
                      </div>
                    </div>

                    <div
                      class="rounded-md border border-color-theme bg-form p-3 lg:col-span-2"
                    >
                      <div class="flex items-center justify-between gap-3 mb-2">
                        <p
                          class="text-xs font-semibold text-table-column-header uppercase tracking-wider"
                        >
                          Credentials
                        </p>
                        <span
                          v-if="getActiveCredential(worker.worker_id)"
                          class="inline-flex items-center px-2 py-0.5 rounded-full text-xs border border-green-500/40 bg-green-500/10 text-green-700"
                        >
                          Active:
                          {{
                            shortCredentialId(
                              getActiveCredential(worker.worker_id)?.id
                            )
                          }}
                        </span>
                      </div>

                      <div
                        v-if="credentialsLoadingFor === worker.worker_id"
                        class="rounded-md border border-color-theme bg-content p-3 text-xs text-definition-list-term"
                      >
                        Loading credentials...
                      </div>

                      <div
                        v-else-if="
                          !getWorkerCredentials(worker.worker_id).length
                        "
                        class="rounded-md border border-color-theme bg-content p-3 text-xs text-definition-list-term"
                      >
                        No credentials issued.
                      </div>

                      <div
                        v-else
                        class="rounded-md border border-color-theme bg-content"
                      >
                        <div class="overflow-x-auto">
                          <table class="w-full text-xs text-table-row">
                            <thead>
                              <tr
                                class="border-b border-color-theme text-table-column-header"
                              >
                                <th
                                  class="px-3 py-2 text-left font-semibold uppercase tracking-wider"
                                >
                                  Status
                                </th>
                                <th
                                  class="px-3 py-2 text-left font-semibold uppercase tracking-wider"
                                >
                                  Credential
                                </th>
                                <th
                                  class="px-3 py-2 text-left font-semibold uppercase tracking-wider"
                                >
                                  Created
                                </th>
                                <th
                                  class="px-3 py-2 text-left font-semibold uppercase tracking-wider"
                                >
                                  Last Used
                                </th>
                                <th
                                  class="px-3 py-2 text-left font-semibold uppercase tracking-wider"
                                >
                                  Expires
                                </th>
                                <th
                                  class="px-3 py-2 text-left font-semibold uppercase tracking-wider"
                                >
                                  Label
                                </th>
                                <th
                                  class="px-3 py-2 text-left font-semibold uppercase tracking-wider"
                                >
                                  Action
                                </th>
                              </tr>
                            </thead>
                            <tbody>
                              <tr
                                v-for="credential in getWorkerCredentials(
                                  worker.worker_id
                                )"
                                :key="credential.id"
                                :class="[
                                  'border-b border-color-theme last:border-b-0',
                                  credential.status === 'ACTIVE'
                                    ? 'bg-green-500/5'
                                    : '',
                                ]"
                              >
                                <td class="px-3 py-2 align-top">
                                  <span
                                    class="inline-flex items-center px-2 py-0.5 rounded-full border"
                                    :class="
                                      credentialStatusClass(credential.status)
                                    "
                                  >
                                    {{ credential.status }}
                                  </span>
                                </td>
                                <td class="px-3 py-2 align-top">
                                  <code
                                    class="bg-form px-1.5 py-0.5 rounded"
                                    :title="credential.id"
                                  >
                                    {{ shortCredentialId(credential.id) }}
                                  </code>
                                </td>
                                <td class="px-3 py-2 align-top">
                                  {{ formatDate(credential.created_at) }}
                                </td>
                                <td class="px-3 py-2 align-top">
                                  {{ formatDate(credential.last_used_at) }}
                                </td>
                                <td class="px-3 py-2 align-top">
                                  {{ formatDate(credential.expires_at) }}
                                </td>
                                <td class="px-3 py-2 align-top">
                                  {{ credential.label || "-" }}
                                </td>
                                <td class="px-3 py-2 align-top">
                                  <Button
                                    v-if="credential.status === 'ACTIVE'"
                                    type="outline"
                                    size="tiny"
                                    :disabled="
                                      credentialActionFor === worker.worker_id
                                    "
                                    @click="
                                      onRevokeCredential(
                                        worker.worker_id,
                                        credential.id
                                      )
                                    "
                                  >
                                    Revoke
                                  </Button>
                                  <span v-else class="text-definition-list-term"
                                    >-</span
                                  >
                                </td>
                              </tr>
                            </tbody>
                          </table>
                        </div>
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
import Button from "../../../tailwind-components/app/components/Button.vue";
import Message from "../../../tailwind-components/app/components/Message.vue";
import InputString from "../../../tailwind-components/app/components/input/String.vue";
import HpcPill from "../components/HpcPill.vue";

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
const copyState = ref<"secret" | "write" | "config" | null>(null);
const copyMessage = ref<string | null>(null);

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

function shortCredentialId(idVal: string): string {
  if (!idVal) return "-";
  if (idVal.length <= 12) return idVal;
  return `${idVal.slice(0, 8)}...${idVal.slice(-4)}`;
}

function getWorkerCredentials(workerId: string): any[] {
  return credentialsByWorker.value[workerId] ?? [];
}

function getActiveCredential(workerId: string): any | null {
  return (
    getWorkerCredentials(workerId).find(
      (credential: any) => credential.status === "ACTIVE"
    ) ?? null
  );
}

function hasActiveCredential(workerId: string): boolean {
  return !!getActiveCredential(workerId);
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

async function runCredentialAction(
  workerId: string,
  action: CredentialAction,
  opts: { label?: string } = {}
) {
  credentialActionFor.value = workerId;
  error.value = null;

  try {
    const label = normalizeOptional(opts.label ?? "");

    const result =
      action === "issue"
        ? await issueWorkerCredential(workerId, { label })
        : await rotateWorkerCredential(workerId, { label });

    revealedSecret.value = { workerId, secret: result.secret };
    await loadCredentials(workerId, true);
  } catch (e: any) {
    const message = toErrorMessage(e);
    if (
      action === "issue" &&
      message.includes("already has an active credential")
    ) {
      error.value = `Worker ${workerId} already has an active credential. Use Rotate to replace it.`;
    } else {
      error.value = message;
    }
  } finally {
    credentialActionFor.value = null;
  }
}

async function onIssueCredential(workerId: string) {
  if (hasActiveCredential(workerId)) {
    error.value = `Worker ${workerId} already has an active credential. Use Rotate to replace it.`;
    return;
  }
  await runCredentialAction(workerId, "issue");
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
    });
    showBootstrap.value = false;
    await loadWorkers({ background: true });
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

function fallbackCopyText(value: string): boolean {
  try {
    const area = document.createElement("textarea");
    area.value = value;
    area.setAttribute("readonly", "true");
    area.style.position = "fixed";
    area.style.opacity = "0";
    area.style.pointerEvents = "none";
    document.body.appendChild(area);
    area.focus();
    area.select();
    const copied = document.execCommand("copy");
    document.body.removeChild(area);
    return copied;
  } catch {
    return false;
  }
}

async function copyText(value: string, key: "secret" | "write" | "config") {
  copyMessage.value = null;
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(value);
    } else if (!fallbackCopyText(value)) {
      throw new Error("Clipboard unavailable");
    }
    copyState.value = key;
    setTimeout(() => {
      if (copyState.value === key) copyState.value = null;
    }, 1500);
  } catch {
    const copied = fallbackCopyText(value);
    if (copied) {
      copyState.value = key;
      copyMessage.value = "Copied using fallback clipboard mode.";
      setTimeout(() => {
        if (copyState.value === key) copyState.value = null;
        copyMessage.value = null;
      }, 2000);
    } else {
      copyMessage.value =
        "Copy failed. Select text manually and press Cmd/Ctrl+C.";
    }
  }
}

async function copySecretValue() {
  if (!revealedSecret.value) return;
  await copyText(revealedSecret.value.secret, "secret");
}

async function copyWriteSecretCommand() {
  if (!revealedSecret.value) return;
  await copyText(secretWriteSnippet(revealedSecret.value.secret), "write");
}

async function copyDaemonConfig() {
  if (!revealedSecret.value) return;
  await copyText(daemonConfigSnippet(revealedSecret.value.workerId), "config");
}

function dismissRevealedSecret() {
  revealedSecret.value = null;
  copyState.value = null;
  copyMessage.value = null;
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
