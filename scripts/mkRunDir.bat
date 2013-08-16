@echo off
rem Setting up empty PECAS run directory
set dir=pecasRun
set begyear=2004
set endyear=2050

if exist ./%dir% (
    echo directory "%dir%" already exists
) else (
    mkdir %dir%

    for /L %%y in (%begyear% 1 %endyear%) do mkdir %dir%\%%y

    mkdir %dir%\AllYears
    mkdir %dir%\AllYears\Inputs
    mkdir %dir%\AllYears\Outputs
    mkdir %dir%\AllYears\Code
    mkdir %dir%\AllYears\Working
)