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

Using the `MOLGENIS_TOKEN` you can also use API to submit jobs (change server URL to your server):

`
curl -X POST https://localhost:8080/api/tasks/ -v
`

or with parameters
`
curl -X POST https://localhost/api/tasks -v -d 'my parameters, as text or json or whever you like'  
`

You will receive a taskID, you can use that to inspect status of the running task

E.g. to retrieve task 123123:
`
curl GET https://server/api/tasks/123123
`