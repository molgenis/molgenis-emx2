<script lang="ts" setup>
import { ref, onMounted } from "vue";

import { groups } from "d3";
const d3 = { groups };

import {
  DashboardRow,
  DashboardChart,
  GroupedColumnChart,
  DataTable,
  LoadingScreen,
  // @ts-expect-error
} from "molgenis-viz";

import ProviderDashboard from "../../components/ProviderDashboard.vue";
import { generateAxisTickData } from "../../utils/generateAxisTicks";
import { getGeneticLossData } from "../../utils/getGeneticHearingLossData";
import { sortByDataPointName } from "../../utils";

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

interface GenesSummaryData {
  Gene: string;
  "Count your center": number;
  "Count ERN": number;
}
const genesTableData = ref<GenesSummaryData[]>();

const colorPalette = {
  ERN: "#B98DAF", // "#9f6491",
  "Your center": "#A7DCCB", //"#66c2a4",
};

onMounted(async () => {
  const yourCenter = await getGeneticLossData(
    props.api.graphql.current,
    "Your center"
  );
  const allCenters = await getGeneticLossData(
    props.api.graphql.providers,
    "ERN"
  );

  hearingLossTypeChart.value = yourCenter.hearingLossTypes;
  hearingLossSeverityChart.value = yourCenter.severity;
  hearingLossOnsetChart.value = yourCenter.ageOfOnset;
  geneticDiagnosisGenesChart.value = yourCenter.diagnosisGenes;
  geneticDiagnosisTypeChart.value = yourCenter.diagnosisTypes;
  etiologyChart.value = yourCenter.etiology;
  syndromicClassifcationChart.value = yourCenter.syndromicClassification;

  // combine data
  hearingLossTypeChart.value.dataPoints = sortByDataPointName([
    ...(hearingLossTypeChart.value.dataPoints as IChartData[]),
    ...(allCenters.hearingLossTypes.dataPoints as IChartData[]),
  ]);

  hearingLossSeverityChart.value.dataPoints = sortByDataPointName([
    ...(hearingLossSeverityChart.value.dataPoints as IChartData[]),
    ...(allCenters.severity.dataPoints as IChartData[]),
  ]);

  hearingLossOnsetChart.value.dataPoints = [
    ...(hearingLossOnsetChart.value.dataPoints as IChartData[]),
    ...(allCenters.ageOfOnset.dataPoints as IChartData[]),
  ].sort((a, b) => {
    return (
      (a.dataPointOrder as number) - (b.dataPointOrder as number) ||
      (b.dataPointSecondaryCategory as string).localeCompare(
        a.dataPointSecondaryCategory as string
      )
    );
  });

  geneticDiagnosisGenesChart.value.dataPoints = sortByDataPointName([
    ...(geneticDiagnosisGenesChart.value.dataPoints as IChartData[]),
    ...(allCenters.diagnosisGenes.dataPoints as IChartData[]),
  ]);

  geneticDiagnosisTypeChart.value.dataPoints = sortByDataPointName([
    ...(geneticDiagnosisTypeChart.value.dataPoints as IChartData[]),
    ...(allCenters.diagnosisTypes.dataPoints as IChartData[]),
  ]);

  etiologyChart.value.dataPoints = sortByDataPointName([
    ...(etiologyChart.value.dataPoints as IChartData[]),
    ...(allCenters.etiology.dataPoints as IChartData[]),
  ]);

  syndromicClassifcationChart.value.dataPoints = sortByDataPointName([
    ...(syndromicClassifcationChart.value.dataPoints as IChartData[]),
    ...(allCenters.syndromicClassification.dataPoints as IChartData[]),
  ]);
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
  }

  // age of onset prep
  const onsetAxis = generateAxisTickData(
    hearingLossOnsetChart.value?.dataPoints as IChartData[],
    "dataPointValue"
  );
  if (hearingLossOnsetChart.value) {
    hearingLossOnsetChart.value.yAxisMaxValue = onsetAxis.limit;
    hearingLossOnsetChart.value.yAxisTicks = onsetAxis.ticks;
  }

  // prep genetic diagnosis gene
  genesTableData.value = d3
    .groups(
      geneticDiagnosisGenesChart.value.dataPoints,
      (row) => row.dataPointName,
      (row) => row.dataPointSecondaryCategory
    )
    .map(([group, subgroup]) => {
      return {
        Gene: group,
        ...Object.fromEntries(
          subgroup.map(([entry, value]) => {
            return [`Count ${entry}`, value[0].dataPointValue];
          })
        ),
      };
    })
    .sort((a, b) => {
      return (b.Gene as string).localeCompare(a.Gene as string);
    }) as GenesSummaryData[];

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
    <h2 class="dashboard-h2">Overview for all centers</h2>
    <DashboardRow :columns="1">
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 250px" />
        <GroupedColumnChart
          v-else
          :chartId="hearingLossTypeChart?.chartId"
          :title="hearingLossTypeChart?.chartTitle"
          :description="hearingLossTypeChart?.chartSubtitle"
          :chartData="hearingLossTypeChart?.dataPoints"
          xvar="dataPointSecondaryCategory"
          yvar="dataPointValue"
          group="dataPointName"
          :xAxisLabel="hearingLossTypeChart?.xAxisLabel"
          :yAxisLabel="hearingLossTypeChart?.yAxisLabel"
          :yMin="0"
          :yMax="hearingLossTypeChart?.yAxisMaxValue"
          :yTickValues="hearingLossTypeChart?.yAxisTicks"
          :columnColorPalette="colorPalette"
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
    <DashboardRow :columns="2">
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 250px" />
        <GroupedColumnChart
          v-else
          :chartId="`${hearingLossSeverityChart?.chartId}-left`"
          title="Severity of hearing loss in the left ear"
          :chartData="hearingLossSeverityChart?.dataPoints?.filter((row: IChartData)=> row.dataPointPrimaryCategory === 'Left ear')"
          xvar="dataPointSecondaryCategory"
          yvar="dataPointValue"
          group="dataPointName"
          :yAxisLabel="hearingLossSeverityChart?.yAxisLabel"
          :yMin="0"
          :yMax="hearingLossSeverityChart?.yAxisMaxValue"
          :yTickValues="hearingLossSeverityChart?.yAxisTicks"
          :columnColorPalette="colorPalette"
          columnHoverFill="#EE7032"
          :chartHeight="250"
          :chartMargins="{
            top: hearingLossSeverityChart?.topMargin,
            right: hearingLossSeverityChart?.rightMargin,
            bottom: hearingLossSeverityChart?.bottomMargin,
            left: hearingLossSeverityChart?.leftMargin,
          }"
        />
      </DashboardChart>
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 250px" />
        <GroupedColumnChart
          v-else
          :chartId="`${hearingLossSeverityChart?.chartId}-right`"
          title="Severity of hearing loss in the right ear"
          :chartData="hearingLossSeverityChart?.dataPoints?.filter((row: IChartData)=> row.dataPointPrimaryCategory === 'Right ear')"
          xvar="dataPointSecondaryCategory"
          yvar="dataPointValue"
          group="dataPointName"
          :yAxisLabel="hearingLossSeverityChart?.yAxisLabel"
          :yMin="0"
          :yMax="hearingLossSeverityChart?.yAxisMaxValue"
          :yTickValues="hearingLossSeverityChart?.yAxisTicks"
          :columnColorPalette="colorPalette"
          columnHoverFill="#EE7032"
          :chartHeight="250"
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
        <GroupedColumnChart
          v-else
          :chartId="hearingLossOnsetChart?.chartId"
          :title="hearingLossOnsetChart?.chartTitle"
          :description="hearingLossOnsetChart?.chartSubtitle"
          :chartData="hearingLossOnsetChart?.dataPoints"
          xvar="dataPointSecondaryCategory"
          yvar="dataPointValue"
          group="dataPointName"
          :xAxisLabel="hearingLossOnsetChart?.xAxisLabel"
          :yAxisLabel="hearingLossOnsetChart?.yAxisLabel"
          :yMin="0"
          :yMax="hearingLossOnsetChart?.yAxisMaxValue"
          :yTickValues="hearingLossOnsetChart?.yAxisTicks"
          :columnColorPalette="colorPalette"
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
          :tableId="geneticDiagnosisGenesChart?.chartId"
          :caption="geneticDiagnosisGenesChart?.chartTitle"
          :data="genesTableData"
          :columnOrder="['Gene', 'Count Your center', 'Count ERN']"
        />
      </DashboardChart>
    </DashboardRow>
    <DashboardRow :columns="1">
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 250px" />
        <GroupedColumnChart
          v-else
          :chartId="geneticDiagnosisTypeChart?.chartId"
          :title="geneticDiagnosisTypeChart?.chartTitle"
          :description="geneticDiagnosisTypeChart?.chartSubtitle"
          :chartData="geneticDiagnosisTypeChart?.dataPoints"
          xvar="dataPointSecondaryCategory"
          yvar="dataPointValue"
          group="dataPointName"
          :xAxisLabel="geneticDiagnosisTypeChart?.xAxisLabel"
          :yAxisLabel="geneticDiagnosisTypeChart?.yAxisLabel"
          :yMin="0"
          :yMax="geneticDiagnosisTypeChart?.yAxisMaxValue"
          :yTickValues="geneticDiagnosisTypeChart?.yAxisTicks"
          :columnColorPalette="colorPalette"
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
    <DashboardRow :columns="1">
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 250px" />
        <GroupedColumnChart
          v-else
          :chartId="etiologyChart?.chartId"
          :title="etiologyChart?.chartTitle"
          :description="etiologyChart?.chartSubtitle"
          :chartData="etiologyChart?.dataPoints"
          xvar="dataPointSecondaryCategory"
          yvar="dataPointValue"
          group="dataPointName"
          :xAxisLabel="etiologyChart?.xAxisLabel"
          :yAxisLabel="etiologyChart?.yAxisLabel"
          :yMin="0"
          :yMax="etiologyChart?.yAxisMaxValue"
          :yTickValues="etiologyChart?.yAxisTicks"
          :columnColorPalette="colorPalette"
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
    </DashboardRow>
    <DashboardRow :columns="1">
      <DashboardChart>
        <LoadingScreen v-if="loading" style="height: 250px" />
        <GroupedColumnChart
          v-else
          :chartId="syndromicClassifcationChart?.chartId"
          :title="syndromicClassifcationChart?.chartTitle"
          :description="syndromicClassifcationChart?.chartSubtitle"
          :chartData="syndromicClassifcationChart?.dataPoints"
          xvar="dataPointSecondaryCategory"
          yvar="dataPointValue"
          group="dataPointName"
          :xAxisLabel="syndromicClassifcationChart?.xAxisLabel"
          :yAxisLabel="syndromicClassifcationChart?.yAxisLabel"
          :yMin="0"
          :yMax="syndromicClassifcationChart?.yAxisMaxValue"
          :yTickValues="syndromicClassifcationChart?.yAxisTicks"
          :columnColorPalette="colorPalette"
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
