-- import zonalmakeuse

DROP TABLE if exists "ZonalMakeUse";

CREATE TABLE "ZonalMakeUse"
(
  activity character varying,
  zonenumber integer,
  commodity character varying,
  moru character(1),
  coefficient double precision,
  utility double precision,
  amount double precision
) 
WITHOUT OIDS;

-- Index: "Activity"

CREATE INDEX "Activity"
  ON "ZonalMakeUse"
  USING btree
  (activity);

-- Index: "Commodity"

CREATE INDEX "Commodity"
  ON "ZonalMakeUse"
  USING btree
  (commodity);

-- Index: "MorU"

CREATE INDEX "MorU"
  ON "ZonalMakeUse"
  USING btree
  (moru);


-- import data
copy "ZonalMakeUse" from 'C:/Documents and Settings/John/My Documents/Consult/CaliforniaPECAS2006/zonalmakeuse/zonalmakeuse.csv' using delimiters ',';

-- this query was used to get details of columns for pasting into crosstab queries below (don't know how to make column headings in crosstab query dynamic)
select distinct commodity from observed_coefficients order by 1;

create view observed_coefficients as 
select activity, commodity, moru, stddev(coefficient) as stddev, avg(coefficient) as average from 
"ZonalMakeUse" where amount <> 0 group by activity,commodity,moru order by moru,activity,commodity;

create view observed_make_coefficients as 
select activity, commodity, moru, stddev(coefficient) as stddev, avg(coefficient) as average from 
"ZonalMakeUse" where amount <> 0 and moru='M' group by activity,commodity,moru order by moru,activity,commodity;

create view observed_use_coefficients as 
select activity, commodity, moru, stddev(coefficient) as stddev, avg(coefficient) as average from 
"ZonalMakeUse" where amount <> 0 and moru='U' group by activity,commodity,moru order by moru,activity,commodity;

-- if the crosstab function doesn't work, you need to install tablefunc extension to postgresql by running 
-- share\contrib\tablefunc.sql

-- these are the make crosstabs, use crosstabs are similar

drop table if exists make_crosstab;

create table make_crosstab as select * from crosstab ('
select activity, commodity, average
from observed_make_coefficients order by 1,2
',
'select distinct commodity from observed_coefficients order by 1')
as 
(
activity text,
"Agricultural space" float8,
"AGRICULTURE (Animals) PRODUCT" float8,
"AGRICULTURE (Forestry and Fishing) PRODUCT"  float8,
"AGRICULTURE (Plants) PRODUCT"  float8,
"AGRICULTURE (Services) PRODUCT"  float8,
"AGRICULTURE office support PRODUCT"  float8,
"Agriculture workers"  float8,
"AMUSEMENT SERVICES (events and tracks) PRODUCT"  float8,
"AMUSEMENT SERVICES (motion pictures and theatrics) PRODUCT"  float8,
"AMUSEMENT SERVICES other PRODUCT" float8,
"AMUSEMENT SERVICES theme parks PRODUCT" float8,
"Assembly and Fabrication workers" float8,
"Business and financial operation workers" float8,
"BUSINESS SERVICES  PRODUCT" float8,
"BUSINESS SERVICES office support PRODUCT" float8,
"Commercial High space" float8,
"Commercial Low space" float8,
"COMMUNICATIONS AND UTILITIES office support PRODUCT" float8,
"COMMUNICATIONS AND UTILITIES PRODUCT" float8,
"CONSTRUCTION (space) PRODUCT" float8,
"CONSTRUCTION office support PRODUCT" float8,
"Construction workers" float8,
"EDUCATION (K12) PRODUCT" float8,
"EDUCATION (public post-primary) PRODUCT" float8,
"Entertainers and media workers" float8,
"FEDERAL GOVERNMENT PRODUCT" float8,
"FINANCE INSURANCE LEGAL PRODUCT" float8,
"Food workers" float8,
"GOVERNMENT ENTERPRISES PRODUCT" float8,
"HEALTH SERVICES PRODUCT" float8,
"Health workers" float8,
"HOTELS PRODUCT" float8,
"Household investment" float8,
"Maintenance and repair workers" float8,
"Managers" float8,
"MANUFACTURING (Chemicals Plastic Rubber Glass Cement) PRODUCT" float8,
"MANUFACTURING (Computers Electronics) PRODUCT" float8,
"MANUFACTURING (Electrical Equipment Appliance) PRODUCT" float8,
"MANUFACTURING (Food) PRODUCT" float8,
"MANUFACTURING (Machinery) PRODUCT" float8,
"MANUFACTURING (Metal Steel) PRODUCT" float8,
"MANUFACTURING (Petro-Coal production) PRODUCT" float8,
"MANUFACTURING (Pulp and Paper) PRODUCT" float8,
"MANUFACTURING (Textiles) PRODUCT" float8,
"MANUFACTURING (Transportation Equipment) PRODUCT" float8,
"MANUFACTURING (Wood Products Printing Furniture Misc) PRODUCT" float8,
"MANUFACTURING office support PRODUCT" float8,
"Manufacturing space" float8,
"MILITARY PRODUCT" float8,
"Military workers" float8,
"MINING AND EXTRACTION office support PRODUCT" float8,
"MINING AND EXTRACTION PRODUCT" float8,
"Money transfers" float8,
"Non retail sales workers" float8,
"Office and administration workers" float8,
"PERSONAL SERVICES PRODUCT" float8,
"Post secondary education workers" float8,
"Primary education workers" float8,
"PROFESSIONAL SERVICES PRODUCT" float8,
"Professionals" float8,
"REAL ESTATE PRODUCT" float8,
"Resources space" float8,
"RESTAURANTS PRODUCT" float8,
"ResType1-VL Luxury" float8,
"ResType2-VL Economy" float8,
"ResType3-L Luxury" float8,
"ResType4-L Economy" float8,
"ResType5-MD Separate Entrance" float8,
"ResType6-MD Shared Entrance" float8,
"ResType7-Higher Density" float8,
"ResType8-Highrise" float8,
"ResType9-Urban MH" float8,
"RETAIL TRADE (automotive) PRODUCT" float8,
"RETAIL TRADE (food) PRODUCT" float8,
"RETAIL TRADE (general merchandise) PRODUCT" float8,
"Retail workers" float8,
"SCRAP" float8,
"Service workers" float8,
"STATE AND LOCAL GOVERNMENT PRODUCT" float8,
"Tax bills" float8,
"Transport workers" float8,
"TRANSPORTATION SERVICES PRODUCT" float8,
"WHOLESALE TRADE (sales and management) PRODUCT" float8,
"WHOLESALE TRADE warehousing and transportation PRODUCT" float8
);

drop table if exists make_elastic;

create table make_elastic as select * from crosstab ('
select activity, commodity, stddev
from observed_make_coefficients order by 1,2
',
'select distinct commodity from observed_coefficients order by 1')
as 
(
activity text,
"Agricultural space" float8,
"AGRICULTURE (Animals) PRODUCT" float8,
"AGRICULTURE (Forestry and Fishing) PRODUCT"  float8,
"AGRICULTURE (Plants) PRODUCT"  float8,
"AGRICULTURE (Services) PRODUCT"  float8,
"AGRICULTURE office support PRODUCT"  float8,
"Agriculture workers"  float8,
"AMUSEMENT SERVICES (events and tracks) PRODUCT"  float8,
"AMUSEMENT SERVICES (motion pictures and theatrics) PRODUCT"  float8,
"AMUSEMENT SERVICES other PRODUCT" float8,
"AMUSEMENT SERVICES theme parks PRODUCT" float8,
"Assembly and Fabrication workers" float8,
"Business and financial operation workers" float8,
"BUSINESS SERVICES  PRODUCT" float8,
"BUSINESS SERVICES office support PRODUCT" float8,
"Commercial High space" float8,
"Commercial Low space" float8,
"COMMUNICATIONS AND UTILITIES office support PRODUCT" float8,
"COMMUNICATIONS AND UTILITIES PRODUCT" float8,
"CONSTRUCTION (space) PRODUCT" float8,
"CONSTRUCTION office support PRODUCT" float8,
"Construction workers" float8,
"EDUCATION (K12) PRODUCT" float8,
"EDUCATION (public post-primary) PRODUCT" float8,
"Entertainers and media workers" float8,
"FEDERAL GOVERNMENT PRODUCT" float8,
"FINANCE INSURANCE LEGAL PRODUCT" float8,
"Food workers" float8,
"GOVERNMENT ENTERPRISES PRODUCT" float8,
"HEALTH SERVICES PRODUCT" float8,
"Health workers" float8,
"HOTELS PRODUCT" float8,
"Household investment" float8,
"Maintenance and repair workers" float8,
"Managers" float8,
"MANUFACTURING (Chemicals Plastic Rubber Glass Cement) PRODUCT" float8,
"MANUFACTURING (Computers Electronics) PRODUCT" float8,
"MANUFACTURING (Electrical Equipment Appliance) PRODUCT" float8,
"MANUFACTURING (Food) PRODUCT" float8,
"MANUFACTURING (Machinery) PRODUCT" float8,
"MANUFACTURING (Metal Steel) PRODUCT" float8,
"MANUFACTURING (Petro-Coal production) PRODUCT" float8,
"MANUFACTURING (Pulp and Paper) PRODUCT" float8,
"MANUFACTURING (Textiles) PRODUCT" float8,
"MANUFACTURING (Transportation Equipment) PRODUCT" float8,
"MANUFACTURING (Wood Products Printing Furniture Misc) PRODUCT" float8,
"MANUFACTURING office support PRODUCT" float8,
"Manufacturing space" float8,
"MILITARY PRODUCT" float8,
"Military workers" float8,
"MINING AND EXTRACTION office support PRODUCT" float8,
"MINING AND EXTRACTION PRODUCT" float8,
"Money transfers" float8,
"Non retail sales workers" float8,
"Office and administration workers" float8,
"PERSONAL SERVICES PRODUCT" float8,
"Post secondary education workers" float8,
"Primary education workers" float8,
"PROFESSIONAL SERVICES PRODUCT" float8,
"Professionals" float8,
"REAL ESTATE PRODUCT" float8,
"Resources space" float8,
"RESTAURANTS PRODUCT" float8,
"ResType1-VL Luxury" float8,
"ResType2-VL Economy" float8,
"ResType3-L Luxury" float8,
"ResType4-L Economy" float8,
"ResType5-MD Separate Entrance" float8,
"ResType6-MD Shared Entrance" float8,
"ResType7-Higher Density" float8,
"ResType8-Highrise" float8,
"ResType9-Urban MH" float8,
"RETAIL TRADE (automotive) PRODUCT" float8,
"RETAIL TRADE (food) PRODUCT" float8,
"RETAIL TRADE (general merchandise) PRODUCT" float8,
"Retail workers" float8,
"SCRAP" float8,
"Service workers" float8,
"STATE AND LOCAL GOVERNMENT PRODUCT" float8,
"Tax bills" float8,
"Transport workers" float8,
"TRANSPORTATION SERVICES PRODUCT" float8,
"WHOLESALE TRADE (sales and management) PRODUCT" float8,
"WHOLESALE TRADE warehousing and transportation PRODUCT" float8
);
