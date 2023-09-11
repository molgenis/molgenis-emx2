This folder contains scripts for the ci

We create a ci docker image to speed up build and make sure we have consistent environment.
See Dockerfile and in particular install_software.sh

To publish the docker:

```
docker login
docker build -t molgenis/molgenis-emx2-ci:v1.0.0 -t molgenis/molgenis-emx2-ci:latest .
docker push molgenis/molgenis-emx2-ci --all-tags
```

Please update version number