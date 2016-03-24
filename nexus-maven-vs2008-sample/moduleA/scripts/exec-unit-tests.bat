REM exec-unit-tests.bat generic Qt on windows Unit Test runner
REM option 1 [%1] is UNIT TEST EXECUTABLE PATH
REM option 2 [%2] is UNIT TEST REPORTFILE PATH

::%~1 remove quotes at begin and end of %1
set EXECUTABLEPATH=%~1
::%~2 remove quotes at begin and end of %2
set REPORTFILEPATH=%~2

echo OFF
setlocal enableextensions
for /f %%i in ("%REPORTFILEPATH%") do (
	set REPORTFILEdrive=%%~di
	set REPORTFILEpath=%%~pi
	set REPORTFILEname=%%~ni
	set REPORTFILEextension=%%~xi
)
md "%REPORTFILEdrive%%REPORTFILEpath%"
endlocal

echo Execute "%EXECUTABLEPATH%" -xunitxml -o "%REPORTFILEPATH%"
"%EXECUTABLEPATH%" -xunitxml -o "%REPORTFILEPATH%"
