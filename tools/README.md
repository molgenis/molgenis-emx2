# Tools

This package contains Python tools. 
Each tool can be built into a package using the 'buildpy' task included in the `build.gradle`.

## To globally build all Python tools
In the root of molgenis-emx2 run 
    
    gradle buildPython

## How to install tools for use
The molgenis-emx2-pyclient packages is published in PyPI at https://pypi.org/project/molgenis-emx2-pyclient/.

In order to install a specific version you can type:

    pip install molgenis-emx2-pyclient==8.193.2

## To locally build one package 
You can run within that folder
```gradle buildPython```
or it makes more sense to actually say
```python3 -m build```

## To add a new package you need to
* add a folder with a valid Python package structure
* add your new project to molgenis-emx2/settings.gradle
