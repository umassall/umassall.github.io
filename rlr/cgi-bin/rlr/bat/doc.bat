del %JD%\bat\output
deltree /y %JD%\doc
mkdir %JD%\doc
mkdir %JD%\doc\images
copy %JD%\bat\images %JD%\doc\images
copy %jd%\html\find*bnf.html %jd%\classes
cd %jd%\classes
appletviewer file:///%JD%/classes/findlongbnf.html   > %jd%\html\longbnf.html
appletviewer file:///%JD%/classes/findshortbnf.html  > %jd%\html\shortbnf.html
javadoc -author -version -d %JD%\doc -sourcepath %JC%\lib\classes.zip;%JD%\source;%JD%\classes;%JD%\classes.zip %JD%\source\*.java fix parse pointer picture.directFractal picture watch matrix sim sim.data sim.display sim.funApp sim.gradDesc sim.errFun sim.mdp
cd  %JD%
del classes\find*bnf.html
del classes.jar
del ..\websim.zip
    rem jar cf ..\websim.zip *
cd classes
    rem jar cf ..\classes.jar *
cd ..
