-- Database: "MontgomerySpatial"

DROP DATABASE "MontgomerySpatial";

CREATE DATABASE MontgomerySpatial
			  WITH OWNER = "John"
        TEMPLATE=template_postgis
        
--OLD don't use, use PostGIS template instead 
-- CREATE DATABASE "MontgomeryOutputs"
--  WITH OWNER = "John"
--       ENCODING = 'UTF8'
--       TABLESPACE = pg_default;

-- floorspace
drop table floorspace;
drop table floorspacetempYr1;

create table floorspacetempYr1 (FloorspaceZone integer, Commodity varchar(50), Quantity float, chunksize float);

create table floorspace (Year integer, Scenario char(10), FloorspaceZone integer, Commodity varchar(50), Quantity float, chunksize float);
alter table floorspace add primary key (Year, Scenario, FloorspaceZone, Commodity);

copy floorspacetempYr1 from 'C:/Models/PECASMontgomery/S01/2000/Floorspace.csv' DELIMITERS ',' CSV HEADER;
# copy floorspacetempYr1 from 'C:/Models/PECASMontgomery/S01/2000/Floorspace.csv' DELIMITERS ',' CSV HEADER;

insert into floorspace select 2000 as Year, 'S01' as Scenario, FloorspaceZone, Commodity, Quantity, 150 as chunksize from floorspacetempYr1;

alter table floorspace drop column chunksize;
alter table floorspace add column PECASTYPE char(1);

drop table floorspacetemp;
create table floorspacetemp (FloorspaceZone integer, PECASTYPE char(1), Commodity varchar(50), Quantity float);

delete from floorspacetemp;
copy floorspacetemp from 'C:/Models/PECASMontgomery/S01/2001/FloorspaceI.csv' DELIMITERS ',' CSV HEADER;
insert into floorspace select 2001 as Year, 'S01' as Scenario, FloorspaceZone as FloorspaceZone, Commodity as Commodity, Quantity as Quantity, PECASTYPE as PECASTYPE from floorspacetemp;

delete from floorspacetemp;
copy floorspacetemp from 'C:/Models/PECASMontgomery/S01/2002/FloorspaceI.csv' DELIMITERS ',' CSV HEADER;
insert into floorspace select 2002 as Year, 'S01' as Scenario, FloorspaceZone as FloorspaceZone, Commodity as Commodity, Quantity as Quantity, PECASTYPE as PECASTYPE from floorspacetemp;

delete from floorspacetemp;
copy floorspacetemp from 'C:/Models/PECASMontgomery/S01/2003/FloorspaceI.csv' DELIMITERS ',' CSV HEADER;
insert into floorspace select 2003 as Year, 'S01' as Scenario, FloorspaceZone as FloorspaceZone, Commodity as Commodity, Quantity as Quantity, PECASTYPE as PECASTYPE from floorspacetemp;

delete from floorspacetemp;
copy floorspacetemp from 'C:/Models/PECASMontgomery/S01/2004/FloorspaceI.csv' DELIMITERS ',' CSV HEADER;
insert into floorspace select 2004 as Year, 'S01' as Scenario, FloorspaceZone as FloorspaceZone, Commodity as Commodity, Quantity as Quantity, PECASTYPE as PECASTYPE from floorspacetemp;

delete from floorspacetemp;
copy floorspacetemp from 'C:/Models/PECASMontgomery/S01/2005/FloorspaceI.csv' DELIMITERS ',' CSV HEADER;
insert into floorspace select 2005 as Year, 'S01' as Scenario, FloorspaceZone as FloorspaceZone, Commodity as Commodity, Quantity as Quantity, PECASTYPE as PECASTYPE from floorspacetemp;

delete from floorspacetemp;
copy floorspacetemp from 'C:/Models/PECASMontgomery/S01/2006/FloorspaceI.csv' DELIMITERS ',' CSV HEADER;
insert into floorspace select 2006 as Year, 'S01' as Scenario, FloorspaceZone as FloorspaceZone, Commodity as Commodity, Quantity as Quantity, PECASTYPE as PECASTYPE from floorspacetemp;

delete from floorspacetemp;
copy floorspacetemp from 'C:/Models/PECASMontgomery/S01/2007/FloorspaceI.csv' DELIMITERS ',' CSV HEADER;
insert into floorspace select 2007 as Year, 'S01' as Scenario, FloorspaceZone as FloorspaceZone, Commodity as Commodity, Quantity as Quantity, PECASTYPE as PECASTYPE from floorspacetemp;

delete from floorspacetemp;
copy floorspacetemp from 'C:/Models/PECASMontgomery/S01/2008/FloorspaceI.csv' DELIMITERS ',' CSV HEADER;
insert into floorspace select 2008 as Year, 'S01' as Scenario, FloorspaceZone as FloorspaceZone, Commodity as Commodity, Quantity as Quantity, PECASTYPE as PECASTYPE from floorspacetemp;

delete from floorspacetemp;
copy floorspacetemp from 'C:/Models/PECASMontgomery/S01/2009/FloorspaceI.csv' DELIMITERS ',' CSV HEADER;
insert into floorspace select 2009 as Year, 'S01' as Scenario, FloorspaceZone as FloorspaceZone, Commodity as Commodity, Quantity as Quantity, PECASTYPE as PECASTYPE from floorspacetemp;

delete from floorspacetemp;
copy floorspacetemp from 'C:/Models/PECASMontgomery/S01/2010/FloorspaceI.csv' DELIMITERS ',' CSV HEADER;
insert into floorspace select 2010 as Year, 'S01' as Scenario, FloorspaceZone as FloorspaceZone, Commodity as Commodity, Quantity as Quantity, PECASTYPE as PECASTYPE from floorspacetemp;

delete from floorspacetemp;
copy floorspacetemp from 'C:/Models/PECASMontgomery/S01/2011/FloorspaceI.csv' DELIMITERS ',' CSV HEADER;
insert into floorspace select 2011 as Year, 'S01' as Scenario, FloorspaceZone as FloorspaceZone, Commodity as Commodity, Quantity as Quantity, PECASTYPE as PECASTYPE from floorspacetemp;

delete from floorspacetemp;
copy floorspacetemp from 'C:/Models/PECASMontgomery/S01/2012/FloorspaceI.csv' DELIMITERS ',' CSV HEADER;
insert into floorspace select 2012 as Year, 'S01' as Scenario, FloorspaceZone as FloorspaceZone, Commodity as Commodity, Quantity as Quantity, PECASTYPE as PECASTYPE from floorspacetemp;

delete from floorspacetemp;
copy floorspacetemp from 'C:/Models/PECASMontgomery/S01/2013/FloorspaceI.csv' DELIMITERS ',' CSV HEADER;
insert into floorspace select 2013 as Year, 'S01' as Scenario, FloorspaceZone as FloorspaceZone, Commodity as Commodity, Quantity as Quantity, PECASTYPE as PECASTYPE from floorspacetemp;

delete from floorspacetemp;
copy floorspacetemp from 'C:/Models/PECASMontgomery/S01/2014/FloorspaceI.csv' DELIMITERS ',' CSV HEADER;
insert into floorspace select 2014 as Year, 'S01' as Scenario, FloorspaceZone as FloorspaceZone, Commodity as Commodity, Quantity as Quantity, PECASTYPE as PECASTYPE from floorspacetemp;

delete from floorspacetemp;
copy floorspacetemp from 'C:/Models/PECASMontgomery/S01/2015/FloorspaceI.csv' DELIMITERS ',' CSV HEADER;
insert into floorspace select 2015 as Year, 'S01' as Scenario, FloorspaceZone as FloorspaceZone, Commodity as Commodity, Quantity as Quantity, PECASTYPE as PECASTYPE from floorspacetemp;


-- exchange results
drop table exchangeresults;
drop table exchangeresultstemp;

create table exchangeresultstemp 
	(Commodity varchar(50),
	ZoneNumber integer,
	Demand float ,
	InternalBought float,
	Exports float,
	Supply float,
	InternalSold float,
	Imports float,
	Surplus float,
	Price float);

create table exchangeresults
	(Year integer,
	Scenario char(10),
	Commodity varchar(50),
	ZoneNumber integer,
	Demand float ,
	InternalBought float,
	Exports float,
	Supply float,
	InternalSold float,
	Imports float,
	Surplus float,
	Price float, primary key (Year, Scenario, Commodity, ZoneNumber));

delete from exchangeresults;

delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2000/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2000 
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;

delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2001/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2001 
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;


delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2002/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2002 
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;


delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2003/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2003
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;


delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2004/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2004 
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;


delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2005/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2005 
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;


delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2006/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2006 
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;


delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2007/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2007
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;


delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2008/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2008
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;


delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2009/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2009
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;


delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2010/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2010
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;


delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2011/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2011
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;


delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2012/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2012
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;


delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2013/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2013 
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;



delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2014/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2014
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;


delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2015/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2015 
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;


delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2016/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2016 
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;

delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2017/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2017 
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;

delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2018/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2018 
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;

delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2019/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2019
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;

delete from exchangeresultstemp;
copy exchangeresultstemp from 'C:/Models/PECASMontgomery/S01/2020/ExchangeResults.csv' DELIMITERS ',' CSV HEADER;
insert into exchangeresults select 2020
	as Year, 'S01' as Scenario, Commodity, 	ZoneNumber, Demand, InternalBought, Exports, Supply, InternalSold, Imports, Surplus, Price from exchangeresultstemp;


-- activitylocations
drop table activitylocations;
drop table activitylocationstemp;

create table activitylocationstemp  
	(Activity char(50),
	ZoneNumber int,
	Quantity float,
	TechnologyLogsum float,
	SizeUtility float,
	ZoneConstant float,
	Constrained boolean,
	ConstraintValue float,
	LocationUtility float);

create table activitylocations
	(Year integer,
	Scenario char(10),
	Activity char(50),
	ZoneNumber int,
	Quantity float,
	TechnologyLogsum float,
	SizeUtility float,
	ZoneConstant float,
	Constrained boolean,
	ConstraintValue float,
	LocationUtility float, primary key (Year, Scenario, Activity, ZoneNumber));

delete from activitylocations;

delete from activitylocationstemp;
copy activitylocationstemp from 'C:/Models/PECASMontgomery/S01/2000/ActivityLocations.csv' DELIMITERS ',' CSV HEADER;
insert into activitylocations select 2000 
	as Year, 'S01' as Scenario, Activity, ZoneNumber, Quantity, TechnologyLogsum, SizeUtility, 
	ZoneConstant, Constrained, ConstraintValue, LocationUtility from activitylocationstemp;

-- Now load LUZ shape file into PostGIS using SPIT or shp2pgsql
-- E.g. C:\Program Files\PostgreSQL\8.2\bin>shp2pgsql -D -d C:\MontgomeryShapefiles\TriCounty_Merge_5\TriCounty_Merge_5_allfields.shp public.tricountymerge > tricountymerge.sql
-- psql -d montgomeryspatial -f tricountymerge.sql

-- then to view some results in a PostGIS file:
drop table if exists exchangeresultsgis;
create table exchangeresultsgis as (select luz.the_geom, exchangeresults.* from luz, exchangeresults where (exchangeresults.zonenumber = luz.taz_ctpp and  exchangeresults.commodity='Mobile Home Dwelling' and exchangeresults.year=2001));
-- adding a primary key
alter table exchangeresultsgis add id int primary key

drop table if exists exchangeresultsgis;
create table exchangeresultsgis as (select luz.the_geom, exchangeresults.* from luz, exchangeresults where (exchangeresults.zonenumber = luz.taz_ctpp and  exchangeresults.commodity='Mobile Home Dwelling' and exchangeresults.year=2001));
-- adding a primary key
alter table exchangeresultsgis add id int primary key
