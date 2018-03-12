@echo off
deltree /y c:\javaback
mkdir c:\javaback
xcopy /s/e/v %JD% c:\javaback
