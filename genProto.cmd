@echo off
setlocal

for /f %%A in ('powershell -NoProfile -Command "(Get-NetAdapter | ? Status -eq 'Up' | select -First 1 -Expand MacAddress)"') do set MAC=%%A

echo MAC: %MAC%

if /i "%MAC%"=="B0-82-E2-02-93-72" (
    set "DIR=C:\AORD\Proto\protoSV"
    echo HOME
) else (
    set "DIR=D:\AORD\TreasureHunter\Proto\protoSV"
    echo OFFICE
)

cd /d "%DIR%"
call genProto.bat
endlocal