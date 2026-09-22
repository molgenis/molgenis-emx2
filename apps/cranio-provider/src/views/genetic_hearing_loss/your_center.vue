<script lang="ts" setup>
import { ref, onMounted } from "vue";
import {
  DashboardRow,
  DashboardChart,
  ColumnChart,
  GroupedColumnChart,
  DataTable,
  LoadingScreen,
  // @ts-expect-error
} from "molgenis-viz";

import ProviderDashboard from "../../components/ProviderDashboard.vue";
import { generateAxisTickData } from "../../../../tailwind-components/app/utils/viz.js";
import { getGeneticLossData } from "../../utils/getGeneticHearingLossData";
import {
  ernYourCenterPalette,
  columnHoverFillColor,
} from "../../utils/variables";

import type {
  ICharts,
  IChartData,
} from "../../../../metadata-utils/src/viz/UiDashboard.js";
import type { IAppPage } from "../../types";

const props = defineProps<IAppPage>();
const loading = ref<boolean>(true);
const hearingLossTypeLeftChart = ref<ICharts>();
const hearingLossTypeRightChart = ref<ICharts>();
const hearingLossSeverityChart = ref<ICharts>();
const hearingLossOnsetChart = ref<ICharts>();
const geneticDiagnosisTypeChart = ref<ICharts>();
const geneticDiagnosisGenesChart = ref<ICharts>();
const etiologyChart = ref<ICharts>();
const syndromicClassifcationChart = ref<ICharts>();
const rehabilitationTypeChart = ref<ICharts>();

onMounted(async () => {
  const yourCenter = await getGeneticLossData(
    props.api.graphql.current,
    "Your center"
  );

  hearingLossTypeLeftChart.value = yourCenter.hearingLossTypeLeft;
  hearingLossTypeRightChart.value = yourCenter.hearingLossTypeRight;
  hearingLossSeverityChart.value = yourCenter.severity;
  hearingLossOnsetChart.value = yourCenter.ageOfOnset;
  geneticDiagnosisGenesChart.value = yourCenter.diagnosisGenes;
  geneticDiagnosisTypeChart.value = yourCenter.diagnosisTypes;
  etiologyChart.value = yourCenter.etiology;
  syndromicClassifcationChart.value = yourCenter.syndromicClassification;
  rehabilitationTypeChart.value = yourCenter.rehabilitationChart;

  // create data for type of hearing loss chart left
  const hearingLossLeftAxis = generateAxisTickData(
    hearingLossTypeLeftChart.value?.dataPoints as IChartData[],
    "value"
  );

  if (hearingLossTypeLeftChart.value) {
    hearingLossTypeLeftChart.value.yAxisMaxValue = hearingLossLeftAxis.limit;
    hearingLossTypeLeftChart.value.yAxisTicks = hearingLossLeftAxis.ticks;
  }

  // create data for type of hearing loss chart right
  const hearingLossRightAxis = generateAxisTickData(
    hearingLossTypeRightChart.value?.dataPoints as IChartData[],
    "value"
  );

  if (hearingLossTypeRightChart.value) {
    hearingLossTypeRightChart.value.yAxisMaxValue = hearingLossRightAxis.limit;
    hearingLossTypeRightChart.value.yAxisTicks = hearingLossRightAxis.ticks;
  }

  // prep chart for severity of hearing loss
  const serverityAxis = generateAxisTickData(
    hearingLossSeverityChart.value?.dataPoints as IChartData[],
    "value"
  );

  if (hearingLossSeverityChart.value) {
    hearingLossSeverityChart.value.yAxisMaxValue = serverityAxis.limit;
    hearingLossSeverityChart.value.yAxisTicks = serverityAxis.ticks;
    hearingLossSeverityChart.value.dataPoints =
      hearingLossSeverityChart.value.dataPoints?.sort(
        (a: IChartData, b: IChartData) => {
          return (
            (a.sortOrder as number) - (b.sortOrder as number) ||
            (b.primaryCategory as string).localeCompare(
              a.primaryCategory as string
            )
          );
        }
      );
  }

  // genes
  if (geneticDiagnosisGenesChart.value.dataPoints) {
    geneticDiagnosisGenesChart.value.dataPoints =
      geneticDiagnosisGenesChart.value.dataPoints
        ?.map((row: IChartData) => {
          return {
            ...row,
            Gene: row.name,
            Total: row.value,
          };
        })
        .sort((a: IChartData, b: IChartData) => {
          return (a.name as string).localeCompare(b.name as string);
        });
  }

  // age of onset prep
  const onsetAxis = generateAxisTickData(
    hearingLossOnsetChart.value?.dataPoints as IChartData[],
    "value"
  );
  if (hearingLossOnsetChart.value) {
    hearingLossOnsetChart.value.yAxisMaxValue = onsetAxis.limit;
    hearingLossOnsetChart.value.yAxisTicks = onsetAxis.ticks;

    hearingLossOnsetChart.value.dataPoints =
      hearingLossOnsetChart.value.dataPoints?.sort((a, b) => {
        return (
          (a.sortOrder as number) - (b.sortOrder as number) ||
          (a.name as string).localeCompare(b.name as string)
        );
      });
  }

  // prep genetic diagnosis type
  const dxTypeAxis = generateAxisTickData(
    geneticDiagnosisTypeChart.value?.dataPoints as IChartData[],
    "value"
  );

  if (geneticDiagnosisTypeChart.value) {
    geneticDiagnosisTypeChart.value.yAxisMaxValue = dxTypeAxis.limit;
    geneticDiagnosisTypeChart.value.yAxisTicks = dxTypeAxis.ticks;
  }

  // prep etiology chart
  const etiologyAxis = generateAxisTickData(
    etiologyChart.value?.dataPoints as IChartData[],
    "value"
  );
  if (etiologyChart.value) {
    etiologyChart.value.yAxisMaxValue = etiologyAxis.limit;
    etiologyChart.value.yAxisTicks = etiologyAxis.ticks;
  }

  // prep syndromic classification chart
  const syndomicAxis = generateAxisTickData(
    syndromicClassifcationChart.value?.dataPoints as IChartData[],
    "value"
  );
  if (syndromicClassifcationChart.value) {
    syndromicClassifcationChart.value.yAxisMaxValue = syndomicAxis.limit;
    syndromicClassifcationChart.value.yAxisTicks = syndomicAxis.ticks;
  }

  // prep rehabilitation type chart
  const rehabilitationTypeAxis = generateAxisTickData(
    rehabilitationTypeChart.value?.dataPoints as IChartData[],
    "value"
  );
  if (rehabilitationTypeChart.value) {
    rehabilitationTypeChart.value.yAxisMaxValue = rehabilitationTypeAxis.limit;
    rehabilitationTypeChart.value.yAxisTicks = rehabilitationTypeAxis.ticks;
  }

  loading.value = false;
});
</script>

<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">General overview for your center</h2>
    <DashboardRow :columns="2">
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 250px" />
        <ColumnChart
          v-else
          :chartId="hearingLossTypeLeftChart?.chartId"
          :title="hearingLossTypeLeftChart?.chartTitle"
          :description="hearingLossTypeLeftChart?.chartSubtitle"
          :chartData="hearingLossTypeLeftChart?.dataPoints"
          xvar="name"
          yvar="value"
          :xAxisLabel="hearingLossTypeLeftChart?.xAxisLabel"
          :yAxisLabel="hearingLossTypeLeftChart?.yAxisLabel"
          :yMin="0"
          :yMax="hearingLossTypeLeftChart?.yAxisMaxValue"
          :yTickValues="hearingLossTypeLeftChart?.yAxisTicks"
          :columnFill="ernYourCenterPalette['Your center']"
          :columnHoverFill="columnHoverFillColor"
          :chartHeight="250"
          :chartMargins="{
            top: hearingLossTypeLeftChart?.topMargin,
            right: hearingLossTypeLeftChart?.rightMargin,
            bottom: hearingLossTypeLeftChart?.bottomMargin,
            left: hearingLossTypeLeftChart?.leftMargin,
          }"
        />
      </DashboardChart>
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 250px" />
        <ColumnChart
          v-else
          :chartId="hearingLossTypeRightChart?.chartId"
          :title="hearingLossTypeRightChart?.chartTitle"
          :description="hearingLossTypeRightChart?.chartSubtitle"
          :chartData="hearingLossTypeRightChart?.dataPoints"
          xvar="name"
          yvar="value"
          :xAxisLabel="hearingLossTypeRightChart?.xAxisLabel"
          :yAxisLabel="hearingLossTypeRightChart?.yAxisLabel"
          :yMin="0"
          :yMax="hearingLossTypeRightChart?.yAxisMaxValue"
          :yTickValues="hearingLossTypeRightChart?.yAxisTicks"
          :columnFill="ernYourCenterPalette['Your center']"
          :columnHoverFill="columnHoverFillColor"
          :chartHeight="250"
          :chartMargins="{
            top: hearingLossTypeRightChart?.topMargin,
            right: hearingLossTypeRightChart?.rightMargin,
            bottom: hearingLossTypeRightChart?.bottomMargin,
            left: hearingLossTypeRightChart?.leftMargin,
          }"
        />
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="1">
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 250px" />
        <GroupedColumnChart
          v-else
          :chartId="hearingLossSeverityChart?.chartId"
          :title="hearingLossSeverityChart?.chartTitle"
          :description="hearingLossSeverityChart?.chartSubtitle"
          :chartData="hearingLossSeverityChart?.dataPoints"
          xvar="name"
          yvar="value"
          group="primaryCategory"
          :xAxisLabel="hearingLossSeverityChart?.xAxisLabel"
          :yAxisLabel="hearingLossSeverityChart?.yAxisLabel"
          :yMin="0"
          :yMax="hearingLossSeverityChart?.yAxisMaxValue"
          :yTickValues="hearingLossSeverityChart?.yAxisTicks"
          :columnHoverFill="columnHoverFillColor"
          :columnColorPalette="{
            Mild: '#A7DCCB',
            Moderate: '#00a896',
            Severe: '#028090',
            Profound: '#05668d',
          }"
          :chartHeight="300"
          :chartMargins="{
            top: hearingLossSeverityChart?.topMargin,
            right: hearingLossSeverityChart?.rightMargin,
            bottom: hearingLossSeverityChart?.bottomMargin,
            left: hearingLossSeverityChart?.leftMargin,
          }"
        />
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="1">
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 250px" />
        <ColumnChart
          v-else
          :chartId="hearingLossOnsetChart?.chartId"
          :title="hearingLossOnsetChart?.chartTitle"
          :description="hearingLossOnsetChart?.chartSubtitle"
          :chartData="hearingLossOnsetChart?.dataPoints"
          xvar="name"
          yvar="value"
          :xAxisLabel="hearingLossOnsetChart?.xAxisLabel"
          :yAxisLabel="hearingLossOnsetChart?.yAxisLabel"
          :yMin="0"
          :yMax="hearingLossOnsetChart?.yAxisMaxValue"
          :yTickValues="hearingLossOnsetChart?.yAxisTicks"
          :columnFill="ernYourCenterPalette['Your center']"
          :columnHoverFill="columnHoverFillColor"
          :chartHeight="250"
          :chartMargins="{
            top: hearingLossOnsetChart?.topMargin,
            right: hearingLossOnsetChart?.rightMargin,
            bottom: hearingLossOnsetChart?.bottomMargin,
            left: hearingLossOnsetChart?.leftMargin,
          }"
        />
      </DashboardChart>
    </DashboardRow>
    <h3 class="dashboard-h3">Genetic diagnosis</h3>
    <DashboardRow :columns="1">
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 250px" />
        <DataTable
          v-else
          :tableId="geneticDiagnosisGenesChart?.chartId"
          :data="geneticDiagnosisGenesChart?.dataPoints"
          :columnOrder="['Gene', 'Total']"
          :caption="geneticDiagnosisGenesChart?.chartTitle"
        />
      </DashboardChart>
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 250px" />
        <ColumnChart
          v-else
          :chartId="geneticDiagnosisTypeChart?.chartId"
          :title="geneticDiagnosisTypeChart?.chartTitle"
          :description="geneticDiagnosisTypeChart?.chartSubtitle"
          :chartData="geneticDiagnosisTypeChart?.dataPoints"
          xvar="name"
          yvar="value"
          :xAxisLabel="geneticDiagnosisTypeChart?.xAxisLabel"
          :yAxisLabel="geneticDiagnosisTypeChart?.yAxisLabel"
          :yMin="0"
          :yMax="geneticDiagnosisTypeChart?.yAxisMaxValue"
          :yTickValues="geneticDiagnosisTypeChart?.yAxisTicks"
          :columnFill="ernYourCenterPalette['Your center']"
          :columnHoverFill="columnHoverFillColor"
          :chartHeight="250"
          :chartMargins="{
            top: geneticDiagnosisTypeChart?.topMargin,
            right: geneticDiagnosisTypeChart?.rightMargin,
            bottom: geneticDiagnosisTypeChart?.bottomMargin,
            left: geneticDiagnosisTypeChart?.leftMargin,
          }"
        />
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="2">
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 250px" />
        <ColumnChart
          v-else
          :chartId="etiologyChart?.chartId"
          :title="etiologyChart?.chartTitle"
          :description="etiologyChart?.chartSubtitle"
          :chartData="etiologyChart?.dataPoints"
          xvar="name"
          yvar="value"
          :xAxisLabel="etiologyChart?.xAxisLabel"
          :yAxisLabel="etiologyChart?.yAxisLabel"
          :yMin="0"
          :yMax="etiologyChart?.yAxisMaxValue"
          :yTickValues="etiologyChart?.yAxisTicks"
          :columnFill="ernYourCenterPalette['Your center']"
          :columnHoverFill="columnHoverFillColor"
          :chartHeight="250"
          :chartMargins="{
            top: etiologyChart?.topMargin,
            right: etiologyChart?.rightMargin,
            bottom: etiologyChart?.bottomMargin,
            left: etiologyChart?.leftMargin,
          }"
        />
      </DashboardChart>
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 250px" />
        <ColumnChart
          v-else
          class="chart-axis-x-angled-text"
          :chartId="syndromicClassifcationChart?.chartId"
          :title="syndromicClassifcationChart?.chartTitle"
          :description="syndromicClassifcationChart?.chartSubtitle"
          :chartData="syndromicClassifcationChart?.dataPoints"
          xvar="name"
          yvar="value"
          :xAxisLabel="syndromicClassifcationChart?.xAxisLabel"
          :yAxisLabel="syndromicClassifcationChart?.yAxisLabel"
          :yMin="0"
          :yMax="syndromicClassifcationChart?.yAxisMaxValue"
          :yTickValues="syndromicClassifcationChart?.yAxisTicks"
          :columnFill="ernYourCenterPalette['Your center']"
          :columnHoverFill="columnHoverFillColor"
          :chartHeight="250"
          :chartMargins="{
            top: syndromicClassifcationChart?.topMargin,
            right: syndromicClassifcationChart?.rightMargin,
            bottom: syndromicClassifcationChart?.bottomMargin,
            left: syndromicClassifcationChart?.leftMargin,
          }"
        />
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="2">
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 250px" />
        <ColumnChart
          v-else
          :chartId="rehabilitationTypeChart?.chartId"
          :title="rehabilitationTypeChart?.chartTitle"
          :description="rehabilitationTypeChart?.chartSubtitle"
          :chartData="rehabilitationTypeChart?.dataPoints"
          xvar="name"
          yvar="value"
          :xAxisLabel="rehabilitationTypeChart?.xAxisLabel"
          :yAxisLabel="rehabilitationTypeChart?.yAxisLabel"
          :yMin="0"
          :yMax="rehabilitationTypeChart?.yAxisMaxValue"
          :yTickValues="rehabilitationTypeChart?.yAxisTicks"
          :columnFill="ernYourCenterPalette['Your center']"
          :columnHoverFill="columnHoverFillColor"
          :chartHeight="250"
          :chartMargins="{
            top: rehabilitationTypeChart?.topMargin,
            right: rehabilitationTypeChart?.rightMargin,
            bottom: rehabilitationTypeChart?.bottomMargin,
            left: rehabilitationTypeChart?.leftMargin,
          }"
        />
      </DashboardChart>
    </DashboardRow>
  </ProviderDashboard>
</template>
