# Tools

This package contains python tools. Each tool can be built into a package using the 'buildpy' task included in the build.gradle

## To globally build all python tools
In root of molgenis-emx2 run 
```gradle buildPython```

## To locally build one package 
You can run within that folder
```gradle buildPython```
or it make more sense to actually say
```python3 -m build```

Note that ```gradle build``` will include python next to the java and javascript in the overall build.

## To add a new package you need to
* add a folder with a valid python package structure
* add your new project to molgenis-emx2/settings.gradle