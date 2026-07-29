#!/usr/bin/env node
// Diagnostic only: prints what every gradle and turbo invocation of this job did, and asserts
// nothing. Reads the artifacts written by gradle/task-outcomes.gradle and turbo --summarize.
import { mkdirSync, readdirSync, readFileSync, statSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";

const gradleReportDir = process.env.TASK_OUTCOME_DIR ?? "build/reports/task-outcomes";
const overviewPath = process.env.BUILD_DIAGNOSTICS_OVERVIEW ?? "build/reports/build-diagnostics/overview.txt";
const turboRunDir = process.env.TURBO_RUN_DIR ?? "apps/.turbo/runs";
const maxExecutedListed = Number(process.env.MAX_EXECUTED_LISTED ?? 40);
const maxTurboTasksListed = Number(process.env.MAX_TURBO_TASKS_LISTED ?? 40);
const maxSlowestListed = Number(process.env.MAX_SLOWEST_LISTED ?? 15);
const maxHeadlineListed = Number(process.env.MAX_HEADLINE_LISTED ?? 25);
const maxReasonChars = 200;

const executedOutcome = "executed";
const executedWithoutActionsOutcome = "executedWithoutActions";
const outcomeOrder = [
  executedOutcome,
  executedWithoutActionsOutcome,
  "fromCache",
  "upToDate",
  "skipped",
  "noSource",
  "failed",
  "unknown",
];
const separator = " · ";
const indent = "  ";
const seeTheArtifact = "see the artifact";
const rootProjectModule = "(root project)";
const gradleTestStep = "test";
const turboMissStatus = "MISS";
const turboRebuildSteps = ["build", "test-ci"];

const lines: string[] = [];

function emit(line: string = ""): void {
  lines.push(line);
}

interface DiagnosticFile {
  path: string;
  name: string;
  modifiedAt: number;
}

function filesIn(dir: string, extension: string): DiagnosticFile[] {
  let names: string[];
  try {
    names = readdirSync(dir);
  } catch {
    return [];
  }
  return names
    .filter((name) => name.endsWith(extension))
    .map((name) => {
      const path = join(dir, name);
      return { path, name, modifiedAt: statSync(path).mtimeMs };
    })
    .sort((left, right) => left.modifiedAt - right.modifiedAt);
}

function count(amount: number, noun: string): string {
  return `${amount} ${noun}${amount === 1 ? "" : "s"}`;
}

function humanDuration(milliseconds: number): string {
  if (!Number.isFinite(milliseconds)) return "?";
  if (milliseconds >= 60000) {
    const minutes = Math.floor(milliseconds / 60000);
    return `${minutes}m${Math.round((milliseconds % 60000) / 1000)}s`;
  }
  return milliseconds >= 1000 ? `${(milliseconds / 1000).toFixed(1)}s` : `${milliseconds}ms`;
}

function shorten(text: string): string {
  if (text.length <= maxReasonChars) return text;
  return `${text.slice(0, maxReasonChars - 1)}…`;
}

interface GradleTask {
  task: string;
  outcome: string;
  durationMs: number;
  reasons: string;
}

function parseOutcomeFile(path: string): GradleTask[] {
  return readFileSync(path, "utf8")
    .split("\n")
    .filter((line) => line.trim() !== "")
    .map((line) => {
      const [task, outcome, duration, reasons] = line.split("\t");
      const trimmedReasons = (reasons ?? "").trim();
      const bucket =
        outcome === executedOutcome && trimmedReasons === "" ? executedWithoutActionsOutcome : outcome;
      return {
        task: task as string,
        outcome: bucket ?? "unknown",
        durationMs: Number(duration ?? 0),
        reasons: trimmedReasons,
      };
    });
}

function countByOutcome(tasks: GradleTask[]): string {
  const counts = new Map<string, number>();
  for (const task of tasks) {
    counts.set(task.outcome, (counts.get(task.outcome) ?? 0) + 1);
  }
  const known = outcomeOrder.filter((outcome) => counts.has(outcome));
  const extra = [...counts.keys()].filter((outcome) => !outcomeOrder.includes(outcome));
  return [...known, ...extra].map((outcome) => `${outcome} ${counts.get(outcome)}`).join(separator);
}

function byDurationDescending(
  left: { durationMs: number },
  right: { durationMs: number }
): number {
  return right.durationMs - left.durationMs;
}

const allTasks: (GradleTask & { invocation: string })[] = [];
const allTurboTasks: TurboTask[] = [];

interface GradleInvocation {
  path: string;
  invocation: string;
  tasks: GradleTask[];
}

function readGradleInvocation(file: DiagnosticFile): GradleInvocation {
  const invocation = file.name.replace(/\.tsv$/, "");
  const tasks = parseOutcomeFile(file.path);
  for (const task of tasks) {
    allTasks.push({ ...task, invocation });
  }
  return { path: file.path, invocation, tasks };
}

function reportGradleInvocation({
  path,
  invocation,
  tasks,
}: GradleInvocation): void {
  const totalMs = tasks.reduce((sum, task) => sum + task.durationMs, 0);
  emit(`== gradle ${invocation} == ${count(tasks.length, "task")}${separator}${humanDuration(totalMs)} of task time`);
  emit(`${indent}${countByOutcome(tasks)}`);

  const executed = tasks.filter((task) => task.outcome === executedOutcome).sort(byDurationDescending);
  if (executed.length === 0) {
    emit(`${indent}nothing executed`);
  } else {
    emit(`${indent}executed, slowest first, with gradle's own execution reason:`);
    for (const task of executed.slice(0, maxExecutedListed)) {
      emit(`${indent}${indent}${humanDuration(task.durationMs)} ${task.task} — ${shorten(task.reasons)}`);
    }
    const hidden = executed.length - maxExecutedListed;
    if (hidden > 0) {
      emit(`${indent}${indent}… ${hidden} more executed tasks, ${seeTheArtifact} ${path}`);
    }
  }
  emit();
}

interface TurboTask {
  taskId?: string;
  hash?: string;
  cache?: { status?: string };
  execution?: { startTime?: number; endTime?: number };
}

interface TurboSummary {
  tasks?: TurboTask[];
  execution?: {
    command?: string;
    cached?: number;
    attempted?: number;
    failed?: number;
    exitCode?: number;
    startTime?: number;
    endTime?: number;
  };
  globalCacheInputs?: {
    hashOfExternalDependencies?: string;
    hashOfInternalDependencies?: string;
    files?: Record<string, string>;
    environmentVariables?: { configured?: string[] };
  };
}

type TurboRunFile = DiagnosticFile & {
  summary?: TurboSummary;
  unreadableReason?: string;
};

function turboGlobalHash(summary: TurboSummary): string {
  const inputs = summary.globalCacheInputs ?? {};
  const externalDependencies = inputs.hashOfExternalDependencies || "(none)";
  const internalDependencies = inputs.hashOfInternalDependencies || "(none)";
  const fileCount = Object.keys(inputs.files ?? {}).length;
  const configuredEnv = (inputs.environmentVariables?.configured ?? []).length;
  return [
    `global cache inputs: externalDeps ${externalDependencies}`,
    `internalDeps ${internalDependencies}`,
    count(fileCount, "global file"),
    count(configuredEnv, "configured env var"),
  ].join(separator);
}

function readTurboRun(file: DiagnosticFile): TurboRunFile {
  try {
    const summary = JSON.parse(readFileSync(file.path, "utf8")) as TurboSummary;
    for (const task of summary.tasks ?? []) {
      allTurboTasks.push(task);
    }
    return { ...file, summary };
  } catch (error) {
    return { ...file, unreadableReason: (error as Error).message };
  }
}

function reportTurboRun(file: TurboRunFile): void {
  if (file.summary === undefined) {
    emit(`== turbo ${file.name} == unreadable run summary: ${file.unreadableReason}`);
    emit();
    return;
  }
  const summary = file.summary;
  const execution = summary.execution ?? {};
  const tasks = summary.tasks ?? [];
  emit(`== turbo ${execution.command ?? "unknown command"} == run ${file.name.replace(/\.json$/, "")}`);
  const durationMs = Number(execution.endTime ?? 0) - Number(execution.startTime ?? 0);
  emit(
    `${indent}${count(tasks.length, "task")}${separator}cached ${execution.cached ?? 0}${separator}attempted ${
      execution.attempted ?? 0
    }${separator}failed ${execution.failed ?? 0}${separator}exit ${execution.exitCode ?? "?"}${separator}${humanDuration(
      durationMs,
    )}`,
  );
  emit(`${indent}${turboGlobalHash(summary)}`);
  const listed = [...tasks].sort(
    (left, right) => taskDuration(right) - taskDuration(left),
  );
  for (const task of listed.slice(0, maxTurboTasksListed)) {
    const status = task.cache?.status ?? "unknown";
    emit(
      `${indent}${indent}${status} ${humanDuration(taskDuration(task))} ${task.taskId} hash ${task.hash ?? "?"}`,
    );
  }
  const hidden = listed.length - maxTurboTasksListed;
  if (hidden > 0) {
    emit(`${indent}${indent}… ${hidden} more turbo tasks, ${seeTheArtifact} ${file.path}`);
  }
  emit();
}

function taskDuration(task: TurboTask): number {
  const execution = task.execution ?? {};
  return Number(execution.endTime ?? 0) - Number(execution.startTime ?? 0);
}

function splitGradleTaskPath(
  taskPath: unknown
): { module: string; step: string } | null {
  if (typeof taskPath !== "string" || !taskPath.startsWith(":")) return null;
  const segments = taskPath.slice(1).split(":");
  if (segments.some((segment) => segment === "")) return null;
  return {
    module: segments.slice(0, -1).join(":") || rootProjectModule,
    step: segments[segments.length - 1] as string,
  };
}

function splitTurboTaskId(
  taskId: unknown
): { package: string; step: string } | null {
  if (typeof taskId !== "string") return null;
  const segments = taskId.split("#");
  if (segments.length !== 2 || segments.some((segment) => segment === "")) return null;
  return { package: segments[0] as string, step: segments[1] as string };
}

function sortedUnique(values: string[]): string[] {
  return [...new Set(values)].sort();
}

function retestedModules(): string[] {
  const modules: string[] = [];
  for (const task of allTasks) {
    const parsed = splitGradleTaskPath(task.task);
    if (parsed?.step === gradleTestStep && task.outcome === executedOutcome) {
      modules.push(parsed.module);
    }
  }
  return sortedUnique(modules);
}

function turboCacheMisses(): string[] {
  const taskIds: string[] = [];
  for (const task of allTurboTasks) {
    const parsed = splitTurboTaskId(task.taskId);
    if (parsed && turboRebuildSteps.includes(parsed.step) && task.cache?.status === turboMissStatus) {
      taskIds.push(task.taskId);
    }
  }
  return sortedUnique(taskIds);
}

function unclassifiedTaskIds(): string[] {
  const gradle = allTasks
    .filter((task) => splitGradleTaskPath(task.task) === null)
    .map((task) => `gradle ${JSON.stringify(task.task ?? null)}`);
  const turbo = allTurboTasks
    .filter((task) => splitTurboTaskId(task.taskId) === null)
    .map((task) => `turbo ${JSON.stringify(task.taskId ?? null)}`);
  return sortedUnique([...gradle, ...turbo]);
}

function emitHeadlineList(label: string, entries: string[]): void {
  emit(`${indent}${label}: ${entries.slice(0, maxHeadlineListed).join(separator)}`);
  const hidden = entries.length - maxHeadlineListed;
  if (hidden > 0) {
    emit(`${indent}${indent}… ${hidden} more, listed per invocation below`);
  }
}

function emitUnclassified(unclassified: string[]): void {
  if (unclassified.length === 0) return;
  emit(`${indent}${count(unclassified.length, "task id")} not attributed to a module or package:`);
  for (const taskId of unclassified.slice(0, maxHeadlineListed)) {
    emit(`${indent}${indent}${taskId}`);
  }
  const hidden = unclassified.length - maxHeadlineListed;
  if (hidden > 0) {
    emit(`${indent}${indent}… ${hidden} more unclassified task ids`);
  }
}

function reportHeadline(): void {
  const modules = retestedModules();
  const misses = turboCacheMisses();
  emit(
    `== what needed work == gradle test executed for ${count(
      modules.length,
      "module",
    )}${separator}turbo build/test-ci missed the cache for ${count(misses.length, "task")}`,
  );
  if (modules.length === 0) {
    emit(`${indent}no gradle test task executed`);
  } else {
    emitHeadlineList("gradle test executed", modules);
  }
  if (misses.length === 0) {
    emit(`${indent}no turbo build or test-ci cache miss`);
  } else {
    emitHeadlineList(`turbo build/test-ci ${turboMissStatus}`, misses);
  }
  emitUnclassified(unclassifiedTaskIds());
  emit();
}

function reportSlowest(): void {
  const slowest = allTasks
    .filter((task) => task.durationMs > 0)
    .sort(byDurationDescending)
    .slice(0, maxSlowestListed);
  if (slowest.length === 0) return;
  emit(`== slowest ${slowest.length} gradle tasks of this job ==`);
  for (const task of slowest) {
    emit(`${indent}${humanDuration(task.durationMs)} ${task.outcome} ${task.task} (${task.invocation})`);
  }
  emit();
}

function main(): void {
  const gradleFiles = filesIn(gradleReportDir, ".tsv");
  const turboFiles = filesIn(turboRunDir, ".json");
  const gradleInvocations = gradleFiles.map(readGradleInvocation);
  const turboRuns = turboFiles.map(readTurboRun);
  emit(
    `=== build diagnostics: ${count(gradleFiles.length, "gradle invocation")}, ${count(
      turboFiles.length,
      "turbo run",
    )} (diagnostic only, asserts nothing) ===`,
  );
  emit();
  reportHeadline();
  if (gradleFiles.length === 0) {
    emit(`no gradle task outcomes in ${gradleReportDir}`);
    emit();
  }
  gradleInvocations.forEach(reportGradleInvocation);
  if (turboFiles.length === 0) {
    emit(`no turbo run summaries in ${turboRunDir}`);
    emit();
  }
  turboRuns.forEach(reportTurboRun);
  reportSlowest();
}

try {
  main();
} catch (error) {
  emit(`build diagnostics could not be produced: ${(error as Error).stack}`);
}

const body = `${lines.join("\n")}\n`;
const report = `${body}=== end of build diagnostics: ${lines.length} lines, ${Buffer.byteLength(
  body,
)} bytes, also stored at ${overviewPath} ===\n`;

try {
  mkdirSync(dirname(overviewPath), { recursive: true });
  writeFileSync(overviewPath, report);
} catch (error) {
  console.error(
    `build diagnostics overview could not be stored at ${overviewPath}: ${
      (error as Error).message
    }`
  );
}

process.stdout.write(report);
