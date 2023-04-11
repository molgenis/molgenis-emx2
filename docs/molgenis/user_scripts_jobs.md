# Scripts and Jobs

In the 'tasks' menu item (also available via http://servername/apps/tasks) you can define scripts and view jobs.

## Scripts

Scripts currently can only be python scripts.

Scripts can have the following:

* unique name, required
* the script type
* the script
* outputFileExtension, optional, for returning output files. E.g. 'txt'
* disabled, when true scripts cannot be run
* cron, will schedule the script to run at planned intervals

Your script will receive:

* a token via environment variable MOLGENIS_TOKEN
* a path to OUTPUT_FILE which you can use to produce an outputFile

## Jobs

Lists the previous jobs. You can see the progress on each of them. Also you can expect any output produced.


