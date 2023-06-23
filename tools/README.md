# Tools

This package contains python tools. Each tool can be built into a package using the 'buildpy' task included in the build.gradle

## To globally build all python tools
In root of molgenis-emx2 run 
```gradle buildPython```

## How to install tools for use
Currently we publish only to pytest. So for example:

For *pyclient* you can find the packages at  https://test.pypi.org/project/molgenis-emx2-pyclient/#history

And then to install specific version you can type:

```pip install -i https://test.pypi.org/simple/ molgenis-emx2-client~=8.193.2.dev1687022806011```

## To locally build one package 
You can run within that folder
```gradle buildPython```
or it make more sense to actually say
```python3 -m build```

## To add a new package you need to
* add a folder with a valid python package structure
* add your new project to molgenis-emx2/settings.gradle
