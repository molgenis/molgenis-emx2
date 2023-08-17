import Client from "../src/client/client.ts";
import { request } from "../src/client/client.ts";
import QueryEMX2 from "../src/queryEmx2/queryEmx2.js";

//account
import MolgenisAccount from "../src/components/account/MolgenisAccount.vue";
import MolgenisSession from "../src/components/account/MolgenisSession.vue";
import MolgenisSignin from "../src/components/account/MolgenisSignin.vue";
import MolgenisSignup from "../src/components/account/MolgenisSignup.vue";
import TokenManager from "../src/components/account/TokenManager.vue";

//display
import ContactDisplay from "../src/components/display/ContactDisplay.vue";
import GridBlock from "../src/components/display/GridBlock.vue";
import ImageCard from "../src/components/display/ImageCard.vue";
import ImageDisplay from "../src/components/display/ImageDisplay.vue";
import KeyValueBlock from "../src/components/display/KeyValueBlock.vue";
import LinksList from "../src/components/display/LinksList.vue";
import PageHeader from "../src/components/display/PageHeader.vue";
import PersonDetails from "../src/components/display/PersonDetails.vue";
import TableDisplay from "../src/components/display/TableDisplay.vue";

//filters
import FilterContainer from "../src/components/filters/FilterContainer.vue";
import FilterInput from "../src/components/filters/FilterInput.vue";
import FilterSidebar from "../src/components/filters/FilterSidebar.vue";
import FilterWell from "../src/components/filters/FilterWell.vue";
import FilterWells from "../src/components/filters/FilterWells.vue";

//forms
import InputTextLocalized from "../src/components/forms/InputTextLocalized.vue";
import ArrayInput from "../src/components/forms/ArrayInput.vue";
import ButtonAction from "../src/components/forms/ButtonAction.vue";
import ButtonAlt from "../src/components/forms/ButtonAlt.vue";
import ButtonDanger from "../src/components/forms/ButtonDanger.vue";
import ButtonDropdown from "../src/components/forms/ButtonDropdown.vue";
import HtmlDropdown from "../src/components/forms/HtmlDropdown.vue";
import ButtonOutline from "../src/components/forms/ButtonOutline.vue";
import ButtonSubmit from "../src/components/forms/ButtonSubmit.vue";
import ConfirmModal from "../src/components/forms/ConfirmModal.vue";
import EditModal from "../src/components/forms/EditModal.vue";
import FormGroup from "../src/components/forms/FormGroup.vue";
import FormInput from "../src/components/forms/FormInput.vue";
import FormMolgenis from "../src/components/forms/FormMolgenis.vue";
import IconAction from "../src/components/forms/IconAction.vue";
import IconBar from "../src/components/forms/IconBar.vue";
import IconDanger from "../src/components/forms/IconDanger.vue";
import Info from "../src/components/forms/Info.vue";
import InlineInput from "../src/components/forms/InlineInput.vue";
import InputBoolean from "../src/components/forms/InputBoolean.vue";
import InputCheckbox from "../src/components/forms/InputCheckbox.vue";
import InputDate from "../src/components/forms/InputDate.vue";
import InputDateTime from "../src/components/forms/InputDateTime.vue";
import InputDecimal from "../src/components/forms/InputDecimal.vue";
import InputFile from "../src/components/forms/InputFile.vue";
import InputEmail from "../src/components/forms/InputEmail.vue";
import InputGroup from "../src/components/forms/InputGroup.vue";
import InputHeading from "../src/components/forms/InputHeading.vue";
import InputInt from "../src/components/forms/InputInt.vue";
import InputLong from "../src/components/forms/InputLong.vue";
import InputOntology from "../src/components/forms/InputOntology.vue";
import InputOntologySubtree from "../src/components/forms/InputOntologySubtree.vue";
import InputPassword from "../src/components/forms/InputPassword.vue";
import InputRadio from "../src/components/forms/InputRadio.vue";
import InputRangeDate from "../src/components/forms/InputRangeDate.vue";
import InputRangeDateTime from "../src/components/forms/InputRangeDateTime.vue";
import InputRangeDecimal from "../src/components/forms/InputRangeDecimal.vue";
import InputRangeInt from "../src/components/forms/InputRangeInt.vue";
import InputRangeLong from "../src/components/forms/InputRangeLong.vue";
import InputRef from "../src/components/forms/InputRef.vue";
import InputRefBack from "../src/components/forms/InputRefBack.vue";
import InputRefList from "../src/components/forms/InputRefList.vue";
import InputRefSelect from "../src/components/forms/InputRefSelect.vue";
import InputSearch from "../src/components/forms/InputSearch.vue";
import InputSelect from "../src/components/forms/InputSelect.vue";
import InputSelectInplace from "../src/components/forms/InputSelectInplace.vue";
import InputString from "../src/components/forms/InputString.vue";
import InputText from "../src/components/forms/InputText.vue";
import InfoPopover from "../src/components/forms/InfoPopover.vue";
import LocaleSwitch from "../src/components/account/LocaleSwitch.vue";
import TooltipComponent from "../src/components/forms/TooltipComponent.vue";
import Tooltip from "../src/components/forms/Tooltip.vue";

import MessageError from "../src/components/forms/MessageError.vue";
import MessageSuccess from "../src/components/forms/MessageSuccess.vue";
import MessageWarning from "../src/components/forms/MessageWarning.vue";
import RowEdit from "../src/components/forms/RowEdit.vue";
import RowEditFooter from "../src/components/forms/RowEditFooter.vue";

//layout
import Breadcrumb from "../src/components/layout/Breadcrumb.vue";
import LayoutCard from "../src/components/layout/LayoutCard.vue";
import LayoutForm from "../src/components/layout/LayoutForm.vue";
import LayoutModal from "../src/components/layout/LayoutModal.vue";
import Molgenis from "../src/components/layout/Molgenis.vue";
import MolgenisMenu from "../src/components/layout/MolgenisMenu.vue";
import MolgenisFooter from "../src/components/layout/MolgenisFooter.vue";
import ReadMore from "../src/components/layout/ReadMore.vue";
import SimpleModal from "../src/components/layout/SimpleModal.vue";
import ShowMore from "../src/components/layout/ShowMore.vue";
import Spinner from "../src/components/layout/Spinner.vue";
import Tab from "../src/components/layout/Tab.vue";
import Tabs from "../src/components/layout/Tabs.vue";
import VueTemplate from "../src/components/layout/VueTemplate.vue";

//tables
import DataDisplayCell from "../src/components/tables/DataDisplayCell.vue";
import RoutedTableExplorer from "../src/components/tables/RoutedTableExplorer.vue";
import TableExplorer from "../src/components/tables/TableExplorer.vue";
import Pagination from "../src/components/tables/Pagination.vue";
import RowButton from "../src/components/tables/RowButton.vue";
import RowButtonAdd from "../src/components/tables/RowButtonAdd.vue";
import RowButtonClone from "../src/components/tables/RowButtonClone.vue";
import RowButtonDelete from "../src/components/tables/RowButtonDelete.vue";
import RowButtonEdit from "../src/components/tables/RowButtonEdit.vue";
import ShowHide from "../src/components/tables/ShowHide.vue";
import TableMolgenis from "../src/components/tables/TableMolgenis.vue";
import TableSimple from "../src/components/tables/TableSimple.vue";
import TableStickyHeaders from "../src/components/tables/TableStickyHeaders.vue";
import AggregateTable from "../src/components/tables/AggregateTable.vue";

//tables/celltypes
import EmailDisplay from "../src/components/tables/cellTypes/EmailDisplay.vue";
import FileDisplay from "../src/components/tables/cellTypes/FileDisplay.vue";
import HyperlinkDisplay from "../src/components/tables/cellTypes/HyperlinkDisplay.vue";
import ListDisplay from "../src/components/tables/cellTypes/ListDisplay.vue";
import ObjectDisplay from "../src/components/tables/cellTypes/ObjectDisplay.vue";
import StringDisplay from "../src/components/tables/cellTypes/StringDisplay.vue";
import TextDisplay from "../src/components/tables/cellTypes/TextDisplay.vue";

//task
import SubTask from "../src/components/task/SubTask.vue";
import Task from "../src/components/task/Task.vue";
import TaskList from "../src/components/task/TaskList.vue";
import TaskManager from "../src/components/task/TaskManager.vue";

//utils
import {
  deepClone,
  convertToCamelCase,
  convertToPascalCase,
  getLocalizedDescription,
  getLocalizedLabel,
  flattenObject,
  applyJsTemplate,
} from "../src/components/utils";
import constants, { privacyConstants } from "../src/components/constants";

export {
  ContactDisplay,
  GridBlock,
  ImageCard,
  ImageDisplay,
  KeyValueBlock,
  LinksList,
  PageHeader,
  PersonDetails,
  TableDisplay,
  MessageWarning,
  InputHeading,
  EmailDisplay,
  HyperlinkDisplay,
  FilterContainer,
  FilterInput,
  FilterSidebar,
  FilterWell,
  FilterWells,
  ArrayInput,
  ButtonAction,
  ButtonDropdown,
  ButtonOutline,
  ButtonAlt,
  ButtonDanger,
  ButtonSubmit,
  Client,
  ConfirmModal,
  DataDisplayCell,
  EditModal,
  FileDisplay,
  FormGroup,
  FormInput,
  FormMolgenis,
  HtmlDropdown,
  IconAction,
  IconBar,
  IconDanger,
  Info,
  InputTextLocalized,
  InlineInput,
  InputBoolean,
  InputCheckbox,
  InputDate,
  InputDateTime,
  InputDecimal,
  InputFile,
  InputGroup,
  InputInt,
  InputLong,
  InputOntology,
  InputOntologySubtree,
  InputPassword,
  InputRadio,
  InputRangeDecimal,
  InputRangeDate,
  InputRangeDateTime,
  InputRangeInt,
  InputRangeLong,
  InputRef,
  InputRefBack,
  InputRefList,
  InputRefSelect,
  InputSearch,
  InputSelect,
  InputSelectInplace,
  InputString,
  InputText,
  InfoPopover,
  LocaleSwitch,
  MessageError,
  MessageSuccess,
  RowButtonAdd,
  RowButtonClone,
  RowButtonDelete,
  RowEdit,
  RowButtonEdit,
  RowEditFooter,
  Breadcrumb,
  LayoutCard,
  LayoutForm,
  LayoutModal,
  Molgenis,
  Pagination,
  Spinner,
  VueTemplate,
  AggregateTable,
  RoutedTableExplorer,
  TableExplorer,
  TableMolgenis,
  TableSimple,
  TableStickyHeaders,
  SubTask,
  Task,
  Tab,
  Tabs,
  TaskManager,
  TaskList,
  TextDisplay,
  TooltipComponent,
  Tooltip,
  MolgenisAccount,
  MolgenisFooter,
  MolgenisMenu,
  MolgenisSession,
  MolgenisSignin,
  MolgenisSignup,
  QueryEMX2,
  ReadMore,
  ShowHide,
  ShowMore,
  SimpleModal,
  StringDisplay,
  RowButton,
  ListDisplay,
  ObjectDisplay,
  TokenManager,
  deepClone,
  constants,
  privacyConstants,
  request,
  convertToPascalCase,
  convertToCamelCase,
  getLocalizedDescription,
  getLocalizedLabel,
  flattenObject,
  applyJsTemplate,
};
