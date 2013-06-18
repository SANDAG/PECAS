SET PGDATABASE=montgomeryspatial
SET PGUSER=John
SET PGPASSWORD=""
SET postcmd="C:\Program Files\PostgreSQL\8.2\bin\psql.exe"

REM default database is postgis and default user is postgres

REM create tables, these should all error if the tables already exist
%postcmd% -c "create table floorspace (Year integer, Scenario char(10), FloorspaceZone integer, Commodity varchar(50), Quantity float) with oids;"
%postcmd% -c "alter table floorspace add primary key (Year, Scenario, FloorspaceZone, Commodity);"
%postcmd% -c "alter table floorspace add column PECASTYPE char(1);"
%postcmd% -c "create table floorspacetemp (FloorspaceZone integer, PECASTYPE char(1), Commodity varchar(50), Quantity float);"
%postcmd% -c "alter table floorspace  add keyfield serial;"
%postcmd% -c "alter table floorspace add constraint keyfieldconstraint unique (keyfield);"

%postcmd% -c "create table exchangeresultstemp (Commodity varchar(50), ZoneNumber integer, Demand float, InternalBought float, Exports float,	Supply float,	InternalSold float,	Imports float,	Surplus float,	Price float);"
%postcmd% -c "create table exchangeresults (Year integer,	Scenario char(10), Commodity varchar(50),	ZoneNumber integer,	Demand float ,	InternalBought float,	Exports float,	Supply float,	InternalSold float,	Imports float,	Surplus float,	Price float, primary key (Year, Scenario, Commodity, ZoneNumber)) with oids;"
%postcmd% -c "alter table exchangeresults  add keyfield serial;"
%postcmd% -c "alter table exchangeresults add constraint keyfieldconstraint2 unique (keyfield);"

%postcmd% -c "create table activitylocationstemp  (Activity char(50),	ZoneNumber int,	Quantity float,	TechnologyLogsum float,	SizeUtility float,	ZoneConstant float,	Constrained boolean,	ConstraintValue float,	LocationUtility float);"
%postcmd% -c "create table activitylocations (Year integer,	Scenario char(10),	Activity char(50),	ZoneNumber int,	Quantity float,	TechnologyLogsum float,	SizeUtility float,	ZoneConstant float,	Constrained boolean,	ConstraintValue float,	LocationUtility float, primary key (Year, Scenario, Activity, ZoneNumber)) with oids;"
%postcmd% -c "alter table activitylocations  add keyfield serial;"
%postcmd% -c "alter table activitylocations add constraint keyfieldconstraint3 unique (keyfield);"

REM create view
%postcmd% -c "create view activitylocationsspatial as (select luz.the_geom, activitylocations.* from luz, activitylocations where activitylocations.zonenumber = luz.taz_ctpp);"
%postcmd% -c "create view floorspacespatial as (select luz.the_geom, floorspace.* from luz, floorspace where floorspace.floorspacezone = luz.taz_ctpp);"
%postcmd% -c "create view exchangeresultsspatial as (select luz.the_geom, exchangeresults.* from luz, exchangeresults where Exchangeresults.zonenumber = luz.taz_ctpp);"
