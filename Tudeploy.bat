@echo off
setlocal enabledelayedexpansion

for /f %%A in ('powershell -NoProfile -Command "(Get-NetAdapter | Where-Object Status -eq \"Up\" | Select-Object -First 1 -ExpandProperty MacAddress)"') do set MAC=%%A

echo Current MAC: !MAC!

if /i "!MAC!"=="B0-82-E2-02-93-72" (
    echo Deploy HOME
    call winscp.com /script=DeployHome.scp
) else (
    echo Deploy OFFICE
    call winscp.com /script=Deploy.scp
)