copy %jd%\html\find*bnf.html %jd%\classes
cd %jd%\classes
appletviewer file:///%JD%/classes/findlongbnf.html   > %jd%\html\longbnf.html
appletviewer file:///%JD%/classes/findshortbnf.html  > %jd%\html\shortbnf.html
del find*bnf.html
