@echo off
cd /d "D:\Tools\tv\mytv-android\app"

set "OUTPUT=tv-kt-src-%date:~0,4%%date:~5,2%%date:~8,2%-%time:~0,2%%time:~3,2%%time:~6,2%.txt"
set "OUTPUT=%OUTPUT: =0%"

type nul > "%OUTPUT%"

for /f "delims=" %%F in ('dir /s /b /a-d "*.kt" ^| sort') do (
    echo //========= %%F ==========>> "%OUTPUT%"
    type "%%F" >> "%OUTPUT%"
    echo.>> "%OUTPUT%"
)

echo Íê³É£º%OUTPUT%
pause