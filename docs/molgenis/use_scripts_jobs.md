# Scripts and jobs

MOLGENIS EMX2 offers the possibility to execute scripts that enable the administrator to perform tasks such as data management on the server itself.
In the 'tasks' menu item (also available via http://servername/apps/tasks) you can define scripts and view jobs. 

## Scripts

Currently, Python and bash scripts are supported.

When creating a script entry the following can be defined:
* script name, required
* script type, either _Python_ or _bash_
* the script itself
* dependencies, optional, list the dependencies for a Python script with optionally version numbers like in a `requirements.txt` file
* extra file, optional, may be a file used by the script or additional modules in a ZIP archive
* outputFileExtension, optional, for returning output files. E.g. 'txt'
* disabled, when set to true the script will not run if a cron schedule is set
* failureAddress, when set, will send a message to this email address, if a job fails
* cron, will schedule the script to run at planned intervals

Your script will receive as environment variables:

* a token via `MOLGENIS_TOKEN`
* a path to `OUTPUT_FILE` which you can use to produce an outputFile which will then be stored in `Jobs.output`

### Pyclient

Python scripts can make use of the [Molgenis Pyclient](use_usingpyclient.md). Make sure to initialize the client as
described [here](use_usingpyclient.md#scripts-and-jobs)


## Jobs
Under the _Jobs_ tab the results of previously executed jobs can be viewed and the status of currently running jobs can be observed. 
Inspect the logs for more detail about the progress of the 
If specified the output file can be obtained from here.

## API

Using the `MOLGENIS_TOKEN` you can also use the Task API to submit jobs (change server URL to your server):

### Authentication Header
To authenticate a request can include your `MOLGENIS_TOKEN` in the header:
```bash
-H "x-molgenis-token: $MOLGENIS_TOKEN"
```

---

### Submit a Job

Submit a script by name (replace `<server>` and `<script-name>` with your values):
```bash
curl -X POST   -H "x-molgenis-token: <MOLGENIS_TOKEN>"   https://<server>/api/script/<script-name>
```

Submit a script with parameters:
```bash
curl -X POST   -d 'params'   https://<server>/api/script/<script-name>
```

On success, you will receive a `taskID` that you can use to check the status of the job.

You can also run a job synchronously using a GET request:
```bash
curl -X GET   -d 'params'   https://<server>/api/script/<script-name>
```
This will return the result status of the job, or in case of a file the resulting file.

### Check Task Status

Retrieve the status of a specific task:
```bash
curl -X GET   https://<server>/api/tasks/<taskID>
```

### Retrieve Script Output

If the script produces a file, you can download it using:
```bash
curl -X GET   https://<server>/api/tasks/<taskID>/output
```

### Manage Tasks

Delete a task:
```bash
curl -X DELETE   https://<server>/api/tasks/<taskID>
```

List all tasks:
```bash
curl -X GET   https://<server>/api/tasks
```

Get all scheduled jobs:
```bash
curl -X GET   https://<server>/api/tasks/scheduled
```

Clear all non-running jobs:
```bash
curl -X POST  https://<server>/api/tasks/clear
```
