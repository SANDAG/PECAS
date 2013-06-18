set SCENDIR="I:\PECASMontgomery-4enci619\S01"
set FWD=I:/PECASMontgomery-4enci619/S01
SET YEAR=2000
SET PGDATABASE=montgomeryspatial
SET PGUSER=John
SET PGPASSWORD=""
SET LASTYEAR=2010
SET postcmd="C:\Program Files\PostgreSQL\8.2\bin\psql.exe"
SET SCENARIO='S01'

REM default database is postgis and default user is postgres

:LOOP

REM load the data into the database
REM load floorspace
%postcmd% -c "delete from floorspacetemp;"
%postcmd% -c "copy floorspacetemp from '%FWD%/%YEAR%/FloorspaceI.csv' DELIMITERS ',' CSV HEADER;"
%postcmd% -c "delete from floorspace where Year=%YEAR% and Scenario=%SCENARIO%;"
%postcmd% -c "insert into floorspace select %YEAR% as Year, %SCENARIO% as Scenario, FloorspaceZone as FloorspaceZone, Commodity as Commodity, Quantity as Quantity, PECASTYPE as PECASTYPE from floorspacetemp;"

REM load exchange results
%postcmd% -c "delete from exchangeresultstemp;"
%postcmd% -c "copy exchangeresultstemp from '%FWD%/%YEAR%/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;"
%postcmd% -c "delete from exchangeresults where Year=%YEAR% and Scenario=%SCENARIO%;"
%postcmd% -c "insert into exchangeresults select %YEAR% as Year, %SCENARIO% as Scenario, Commodity,	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;"

REM load activitylocations
%postcmd% -c "delete from activitylocationstemp;"
%postcmd% -c "copy activitylocationstemp from '%FWD%/%YEAR%/ActivityLocations.csv' DELIMITERS ',' CSV HEADER;"
%postcmd% -c "delete from activitylocations where Year=%YEAR% and Scenario=%SCENARIO%;"
%postcmd% -c "insert into activitylocations select %YEAR% as Year, %SCENARIO% as Scenario, Activity, ZoneNumber, Quantity, TechnologyLogsum, SizeUtility, ZoneConstant, Constrained, ConstraintValue, LocationUtility from activitylocationstemp;"


SET /A YEAR=%YEAR%+1
IF %YEAR% LSS %LASTYEAR% GOTO LOOP
