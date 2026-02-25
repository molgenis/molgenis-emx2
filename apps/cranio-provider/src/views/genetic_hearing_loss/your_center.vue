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

import { generateAxisTickData } from "../../utils/generateAxisTicks";
import { getGeneticLossData } from "../../utils/getGeneticHearingLossData";
import type { ICharts, IChartData } from "../../types/schema";
import type { IAppPage } from "../../types/app";

const props = defineProps<IAppPage>();
const loading = ref<boolean>(true);

const hearingLossTypeChart = ref<ICharts>();
const hearingLossSeverityChart = ref<ICharts>();
const hearingLossOnsetChart = ref<ICharts>();
const geneticDiagnosisTypeChart = ref<ICharts>();
const geneticDiagnosisGenesChart = ref<ICharts>();
const etiologyChart = ref<ICharts>();
const syndromicClassifcationChart = ref<ICharts>();

onMounted(async () => {
  const yourCenter = await getGeneticLossData(
    props.api.graphql.current,
    "Your center"
  );

  hearingLossTypeChart.value = yourCenter.hearingLossTypes;
  hearingLossSeverityChart.value = yourCenter.severity;
  hearingLossOnsetChart.value = yourCenter.ageOfOnset;
  geneticDiagnosisGenesChart.value = yourCenter.diagnosisGenes;
  geneticDiagnosisTypeChart.value = yourCenter.diagnosisTypes;
  etiologyChart.value = yourCenter.etiology;
  syndromicClassifcationChart.value = yourCenter.syndromicClassification;

  // create data for type of hearing loss chart
  const hearingAxis = generateAxisTickData(
    hearingLossTypeChart.value?.dataPoints as IChartData[],
    "dataPointValue"
  );

  if (hearingLossTypeChart.value) {
    hearingLossTypeChart.value.yAxisMaxValue = hearingAxis.limit;
    hearingLossTypeChart.value.yAxisTicks = hearingAxis.ticks;
  }

  // prep chart for severity of hearing loss
  const serverityAxis = generateAxisTickData(
    hearingLossSeverityChart.value?.dataPoints as IChartData[],
    "dataPointValue"
  );

  if (hearingLossSeverityChart.value) {
    hearingLossSeverityChart.value.yAxisMaxValue = serverityAxis.limit;
    hearingLossSeverityChart.value.yAxisTicks = serverityAxis.ticks;
    hearingLossSeverityChart.value.dataPoints =
      hearingLossSeverityChart.value.dataPoints?.sort((a, b) => {
        return (
          (a.dataPointOrder as number) - (b.dataPointOrder as number) ||
          (b.dataPointPrimaryCategory as string).localeCompare(
            a.dataPointPrimaryCategory as string
          )
        );
      });
  }

  // genes
  if (geneticDiagnosisGenesChart.value.dataPoints) {
    geneticDiagnosisGenesChart.value.dataPoints =
      geneticDiagnosisGenesChart.value.dataPoints?.map((row: IChartData) => {
        return {
          ...row,
          Gene: row.dataPointName,
          "Count your center": row.dataPointValue,
        };
      });
  }

  // age of onset prep
  const onsetAxis = generateAxisTickData(
    hearingLossOnsetChart.value?.dataPoints as IChartData[],
    "dataPointValue"
  );
  if (hearingLossOnsetChart.value) {
    hearingLossOnsetChart.value.yAxisMaxValue = onsetAxis.limit;
    hearingLossOnsetChart.value.yAxisTicks = onsetAxis.ticks;

    hearingLossOnsetChart.value.dataPoints =
      hearingLossOnsetChart.value.dataPoints?.sort((a, b) => {
        return (
          (a.dataPointOrder as number) - (b.dataPointOrder as number) ||
          (a.dataPointName as string).localeCompare(b.dataPointName as string)
        );
      });
  }

  // prep genetic diagnosis type
  const dxTypeAxis = generateAxisTickData(
    geneticDiagnosisTypeChart.value?.dataPoints as IChartData[],
    "dataPointValue"
  );

  if (geneticDiagnosisTypeChart.value) {
    geneticDiagnosisTypeChart.value.yAxisMaxValue = dxTypeAxis.limit;
    geneticDiagnosisTypeChart.value.yAxisTicks = dxTypeAxis.ticks;
  }

  // prep etiology chart
  const etiologyAxis = generateAxisTickData(
    etiologyChart.value?.dataPoints as IChartData[],
    "dataPointValue"
  );
  if (etiologyChart.value) {
    etiologyChart.value.yAxisMaxValue = etiologyAxis.limit;
    etiologyChart.value.yAxisTicks = etiologyAxis.ticks;
  }

  // prep syndromic classification chart
  const syndomicAxis = generateAxisTickData(
    syndromicClassifcationChart.value?.dataPoints as IChartData[],
    "dataPointValue"
  );
  if (syndromicClassifcationChart.value) {
    syndromicClassifcationChart.value.yAxisMaxValue = syndomicAxis.limit;
    syndromicClassifcationChart.value.yAxisTicks = syndomicAxis.ticks;
  }

  loading.value = false;
});
</script>

<template>
  <ProviderDashboard>
    <h2 class="dashboard-h2">General overview for your center</h2>
    <DashboardRow :columns="1">
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 250px" />
        <ColumnChart
          v-else
          :chartId="hearingLossTypeChart?.chartId"
          :title="hearingLossTypeChart?.chartTitle"
          :description="hearingLossTypeChart?.chartSubtitle"
          :chartData="hearingLossTypeChart?.dataPoints"
          xvar="dataPointName"
          yvar="dataPointValue"
          :xAxisLabel="hearingLossTypeChart?.xAxisLabel"
          :yAxisLabel="hearingLossTypeChart?.yAxisLabel"
          :yMin="0"
          :yMax="hearingLossTypeChart?.yAxisMaxValue"
          :yTickValues="hearingLossTypeChart?.yAxisTicks"
          columnFill="#A7DCCB"
          columnHoverFill="#EE7032"
          :chartHeight="250"
          :chartMargins="{
            top: hearingLossTypeChart?.topMargin,
            right: hearingLossTypeChart?.rightMargin,
            bottom: hearingLossTypeChart?.bottomMargin,
            left: hearingLossTypeChart?.leftMargin,
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
          xvar="dataPointName"
          yvar="dataPointValue"
          group="dataPointPrimaryCategory"
          :xAxisLabel="hearingLossSeverityChart?.xAxisLabel"
          :yAxisLabel="hearingLossSeverityChart?.yAxisLabel"
          :yMin="0"
          :yMax="hearingLossSeverityChart?.yAxisMaxValue"
          :yTickValues="hearingLossSeverityChart?.yAxisTicks"
          columnHoverFill="#EE7032"
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
          xvar="dataPointName"
          yvar="dataPointValue"
          :xAxisLabel="hearingLossOnsetChart?.xAxisLabel"
          :yAxisLabel="hearingLossOnsetChart?.yAxisLabel"
          :yMin="0"
          :yMax="hearingLossOnsetChart?.yAxisMaxValue"
          :yTickValues="hearingLossOnsetChart?.yAxisTicks"
          columnFill="#A7DCCB"
          columnHoverFill="#EE7032"
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
          :columnOrder="['Gene', 'Count your center']"
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
          xvar="dataPointName"
          yvar="dataPointValue"
          :xAxisLabel="geneticDiagnosisTypeChart?.xAxisLabel"
          :yAxisLabel="geneticDiagnosisTypeChart?.yAxisLabel"
          :yMin="0"
          :yMax="geneticDiagnosisTypeChart?.yAxisMaxValue"
          :yTickValues="geneticDiagnosisTypeChart?.yAxisTicks"
          columnFill="#A7DCCB"
          columnHoverFill="#EE7032"
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
          xvar="dataPointName"
          yvar="dataPointValue"
          :xAxisLabel="etiologyChart?.xAxisLabel"
          :yAxisLabel="etiologyChart?.yAxisLabel"
          :yMin="0"
          :yMax="etiologyChart?.yAxisMaxValue"
          :yTickValues="etiologyChart?.yAxisTicks"
          columnFill="#A7DCCB"
          columnHoverFill="#EE7032"
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
          xvar="dataPointName"
          yvar="dataPointValue"
          :xAxisLabel="syndromicClassifcationChart?.xAxisLabel"
          :yAxisLabel="syndromicClassifcationChart?.yAxisLabel"
          :yMin="0"
          :yMax="syndromicClassifcationChart?.yAxisMaxValue"
          :yTickValues="syndromicClassifcationChart?.yAxisTicks"
          columnFill="#A7DCCB"
          columnHoverFill="#EE7032"
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
  </ProviderDashboard>
</template>
