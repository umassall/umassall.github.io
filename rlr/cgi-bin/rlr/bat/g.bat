rem @echo off
appletviewer file:///%JD%/html/index.html > %JD%\bat\output
type %JD%\bat\output | more
