@echo off
IF     '%1'=='' dir/o    *.java | more
IF NOT '%1'=='' dir/o %1\*.java | more
