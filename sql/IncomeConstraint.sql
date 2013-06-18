--drop table if exists zonalmakeuse;
--CREATE TABLE "zonalmakeuse"
--(
-- activity character varying,
--  zonenumber integer,
--  commodity character varying,
--  moru character(1),
--  coefficient double precision,
--  utility double precision,
--  amount double precision
--) 
--WITHOUT OIDS;


-- Index: "Activity"

--CREATE INDEX "activity"
--  ON "zonalmakeuse"
--  USING btree
--  (activity);

-- Index: "Commodity"

--CREATE INDEX "commodity"
--  ON "zonalmakeuse"
--  USING btree
--  (commodity);

-- Index: "MorU"

--CREATE INDEX "moru"
--  ON "zonalmakeuse"
--  USING btree
--  (moru);

delete from zonalmakeuse;
copy "zonalmakeuse" from 'I:/PECAS_MontgomeryV2/S01/2000/zonalmakeuse.csv' CSV header;

drop table if exists activitylocationstemp;
create table activitylocationstemp  
	(Activity char(50),
	ZoneNumber int,
	Quantity float,
	TechnologyLogsum float,
	SizeUtility float,
	ZoneConstant float,
	Constrained boolean,
	ConstraintValue float,
	LocationUtility float,
	Size float);

--create table activitylocations
--	(Year integer,
--	Scenario char(10),
--	Activity char(50),
--	ZoneNumber int,
--	Quantity float,
--	TechnologyLogsum float,
--	SizeUtility float,
--	ZoneConstant float,
--	Constrained boolean,
--	ConstraintValue float,
--	LocationUtility float, primary key (Year, Scenario, Activity, ZoneNumber));


delete from activitylocations where scenario='S01' and year='2000';
copy activitylocationstemp from 'I:/PECAS_MontgomeryV2/S01/2000/ActivityLocations.csv' DELIMITERS ',' CSV HEADER;
insert into activitylocations select 2000 
	as Year, 'S01' as Scenario, Activity, ZoneNumber, Quantity, TechnologyLogsum, SizeUtility, 
	ZoneConstant, Constrained, ConstraintValue, LocationUtility from activitylocationstemp;

drop table activitylocationstemp;

drop table if exists commodityzutilitiestemp;
create table commodityzutilitiestemp  
	(Commodity char(50),
	Zone int,
	BuyingOrSelling char(1),
	Quantity float,
	zUtility float,
	VariationComponent float,
	PriceComponent float,
	TransportComponent1 float,
	TransportComponent2 float
--	,TransportComponent3 float
--	,TransportComponent4 float
	);

--drop table if exists commodityzutilities;
--create table commodityzutilities
--	(Year integer,
--	Scenario char(10),
--	Commodity char(50),
--	Zone int,
--	BuyingOrSelling char(1),
--	Quantity float,
--	zUtility float,
--	VariationComponent float,
--	PriceComponent float,
--	TransportComponent1 float,
--	TransportComponent2 float
----	,TransportComponent3 float
----	,TransportComponent4 float
--	);

delete from commodityzutilities where scenario='S01' and year='2000';
copy commodityzutilitiestemp from 'I:/PECAS_MontgomeryV2/S01/2000/CommodityZUtilities.csv' DELIMITERS ',' CSV HEADER;
insert into commodityzutilities select 2000 
	as Year, 'S01' as Scenario, Commodity, Zone, BuyingOrSelling, Quantity, zUtility, 
	VariationComponent, PriceComponent, TransportComponent1, TransportComponent2
	--, TransportComponent3, TransportComponent4
	from commodityzutilitiestemp;

drop view if exists expenditures;
create view expenditures as select c.year, c.scenario, mu.Activity, mu.zonenumber, mu.Commodity, mu.moru, mu.amount, c.pricecomponent, mu.amount*c.pricecomponent as expenditure
from commodityzutilities c, ZonalMakeUse mu
where 
  c.year =2000 and c.scenario='S01' and
 mu.zonenumber=c.zone and
 mu.commodity=c.commodity and mu.moru='U' and c.buyingorselling='B' and
 mu.amount!=0 order by mu.activity, mu.zonenumber, mu.commodity;

drop view if exists revenue;
create view revenue as select  c.year, c.scenario, mu.Activity, mu.zonenumber, mu.Commodity, mu.moru, mu.amount, c.pricecomponent, mu.amount*c.pricecomponent as revenue
from commodityzutilities c, ZonalMakeUse mu
where 
  c.year =2000 and c.scenario='S01' and
 mu.zonenumber=c.zone and
 mu.commodity=c.commodity and mu.moru='M' and c.buyingorselling='S' and
 mu.amount!=0 order by mu.activity, mu.zonenumber, mu.commodity;

--select activity, commodity, sum(expenditure) from expenditures where activity ='Households-med' group by activity, commodity order by activity, commodity;
select activity, commodity, sum(revenue) from revenue where activity ='Households-high' group by activity, commodity order by activity, commodity;
