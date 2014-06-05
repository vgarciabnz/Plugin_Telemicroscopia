***Telemicroscopy_Plugin***

Description:
This is an ImageJ plugin that provides extra funcionality to ImageJ. It includes all the funcionality related to video management: capture, streaming, server, configuration, etc. 


Modifying the code:
It can be imported as an Eclipse project for easy modification of the code. One of the most important characteristics of the code is that there are two kind of classes: the classes that are in a package, and the classes that are not in a package. This is due to an ImageJ requirement: the classes that are directly used as a plugin from ImageJ must be out of any package and their name must contain at least one underscore.

The project must be placed at the same level that AppTm4l. So, the workspace tree would be, at least:

*workspace
  -AppTm4l
  -Telemicroscopy_Plugin

The project is built with ant. The following targets are available:

- compile: compile the source code.
- build: compile and generate a jar file.
- deploy (default): compile, generate a jar file and copy it to the AppTm4l/plugins directory.


Integration with ImageJ:
The code of Telemicroscopy_Plugin is compressed in a jar file. This file must be placed in the plugins directory of ImageJ. In this way, any class of the jar file that meets the requirements to be considered an ImageJ plugin can be used directly from ImageJ.
The most usual way to run a plugin is to execute the following command from the Java code of any class:

new MacroRunner("run(\"NameOfTheClass\")\n");

The "NameOfTheClass" parameter must match exactly the name of the class, but replacing underscores with spaces. So, the command to call "Close_.java" class would be:

new MacroRunner("run(\"Close \")\n");
