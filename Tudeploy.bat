@echo off
if "%1"=="" (
    winscp.com /script=DeployVi.scp
) else (
    winscp.com /script=DeployTest.scp
)