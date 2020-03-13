This directory contains batch files useful for
editing, compiling and running Java files.

If this directory is c:\myfiles\java\bat
and Sun's latest JDK is in directory c:\programs\jdk
then for these batch files to work, the autoexec.bat file
should end with the commands:

    set JD=c:\myfiles\java
    set JC=c:\programs\jdk
    set CLASSPATH=%JC%\lib\classes.zip;%JD%\classes
    path=%path%;%JD%\bat;%JC%\bin

Then the command "CA" will compile all the java files.
If that seems to give incorrect error messages due to
a bug in the compiler, try again with "CA2".  "CO" is
like "CA" except it optimizes, which may make the files
smaller, faster, and give less debugging information.

"c Name" will compile the file "Name.java".
"e Name" will edit the file "Name.java".
"ls" lists all the .java files in the current directory.
"back" copies all the source files into c:\javaback.
"back2" copies all the source files into c:\javaback2.
"backa" copies all the source files onto drive a:.
"g" runs the appletviewer on "%JD%\html\index.html" and
    saves everything sent to standard out to "%JD%\bat\output".
"gt" is like "g" but standard out is saved in file "test" in the
     current directory instead.

