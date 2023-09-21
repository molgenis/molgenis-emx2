export default interface ITask {
  id: string;
  description: string;
  status: string;
  subTasks: ITask;
}
