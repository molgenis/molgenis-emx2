# How to run

MOLGENIS EMX2 can be run via

* [local install using java + postgresql](run_java.md)
* [quick test drive via docker-compose](run_docker.md)
* [cloud deploy on kubernetes + helm](run_helm.md)
* [by compiling the source code (for developers)](run_source.md)

Note: if you deploy a newer software version on existing postgresql instance, MOLGENIS will attempt to migrate your postgresql schemas such that it keeps on working.
If it cannot MOLGENIS will throw an exception and you will need to start with empty postgresql database.
Importantly, migrations might not fix your data so please always read release notes on breaking changes.
