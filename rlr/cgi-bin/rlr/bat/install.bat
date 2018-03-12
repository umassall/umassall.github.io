@echo off
a:
deltree /y %JD%
mkdir %JD%
xcopy /s/e/v a:\java %JD%

