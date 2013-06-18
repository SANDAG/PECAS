
-- create TransitionConstantsI as a list of all from and to possibilities
-- of space types
drop table if exists TransitionConstantsI
select 
	a.spacetypename as fromspacetypename, 
	b.spacetypename as tospacetypename, 
	0 as transitionconstant into TransitionConstantsI 
from SpaceTypesI a, SpaceTypesI b

-- SpaceTypesI as originally created did not have this column, which is required for AA<->SD communication
alter table SpaceTypesI add isNotAACommodity bit

-- Set up zoning rules, this is extracting the zoning rules names from an existing table
select ZONING_N_1 as ZoningRulesCodeName into ZoningRulesI FROM [PECAS].[ATLANTAREGION\Wei].[_ful_zoning] group by ZONING_N_1;

-- add the primary key
alter table ZoningRulesI add ZoningRulesCode int primary key IDENTITY(1,1)
-- also add the no change, dereliction, demolition, renovation, new possibilities
alter table ZoningRulesI add 
	noChangePossibilities bit, 
	derilictionPossibilities bit,
	demolitionPossibilities bit,
	renovationPossibilities bit,
	additionPossibilities bit,
	newDevelopmentPossibilities bit;

-- this is getting all the information on zoning and putting it into a temporary table
select ZONING_N_1, Min_FAR, maxFAR, uses, Use2 into zoningTemp FROM [PECAS].[ATLANTAREGION\Wei].[_ful_zoning] group by ZONING_N_1, uses, Use2, min_FAR, maxFAR;
alter table zoningTemp alter column Min_FAR float;
alter table zoningTemp alter column maxFAR float;

-- now we are creating the zoningPermissions table in the database 
create table ZoningPermissions 
	(ZoningRulesCode int, 
	spaceTypeID int,
	minIntensityPermitted float,
	maxIntensityPermitted float,
	acknowledgedUse bit,
	penaltyAcknowledgedSpace float,
	penaltyAcknowledgedLand float,
	servicesRequirement int);

--***********************--
-- Now we have a bunch of queries which add specific permissions into
-- our zoning permissions table.
-- add agricultural permissions
Insert into ZoningPermissions (
	zoningRulesCode,
	SpaceTypeID,
	minIntensityPermitted,
	maxIntensityPermitted,
	acknowledgedUse,
	penaltyAcknowledgedSpace,
	penaltyAcknowledgedLand,
	servicesRequirement)
 select 	
	z1.ZoningRulesCode, 1,
	z.Min_FAR*43560.0, 
	z.maxFAR*43560.0,0,0,0,0 
	from zoningTemp z, ZoningRulesI z1
	where z.ZONING_N_1 = z1.zoningRulesCodeName
	and z.uses = 'A'
-- add commercal-office permissions
Insert into ZoningPermissions (
	zoningRulesCode,
	SpaceTypeID,
	minIntensityPermitted,
	maxIntensityPermitted,
	acknowledgedUse,
	penaltyAcknowledgedSpace,
	penaltyAcknowledgedLand,
	servicesRequirement)
 select 	
	z1.ZoningRulesCode, 4,
	z.Min_FAR*43560.0, 
	z.maxFAR*43560.0,0,0,0,1 
	from zoningTemp z, ZoningRulesI z1
	where z.ZONING_N_1 = z1.zoningRulesCodeName
	and z.uses = 'C'
-- also commercal-retail permissions
Insert into ZoningPermissions (
	zoningRulesCode,
	SpaceTypeID,
	minIntensityPermitted,
	maxIntensityPermitted,
	acknowledgedUse,
	penaltyAcknowledgedSpace,
	penaltyAcknowledgedLand,
	servicesRequirement)
 select 	
	z1.ZoningRulesCode, 3,
	z.Min_FAR*43560.0, 
	z.maxFAR*43560.0,0,0,0,1 
	from zoningTemp z, ZoningRulesI z1
	where z.ZONING_N_1 = z1.zoningRulesCodeName
	and z.uses = 'C'
-- add single family permissions
Insert into ZoningPermissions (
	zoningRulesCode,
	SpaceTypeID,
	minIntensityPermitted,
	maxIntensityPermitted,
	acknowledgedUse,
	penaltyAcknowledgedSpace,
	penaltyAcknowledgedLand,
	servicesRequirement)
 select 	
	z1.ZoningRulesCode, 7,
	z.Min_FAR*43560.0, 
	z.maxFAR*43560.0,0,0,0,1 
	from zoningTemp z, ZoningRulesI z1
	where z.ZONING_N_1 = z1.zoningRulesCodeName
	and z.uses = 'SF'
-- add multi family permissions
Insert into ZoningPermissions (
	zoningRulesCode,
	SpaceTypeID,
	minIntensityPermitted,
	maxIntensityPermitted,
	acknowledgedUse,
	penaltyAcknowledgedSpace,
	penaltyAcknowledgedLand,
	servicesRequirement)
 select 	
	z1.ZoningRulesCode, 8,
	z.Min_FAR*43560.0, 
	z.maxFAR*43560.0,0,0,0,1 
	from zoningTemp z, ZoningRulesI z1
	where z.ZONING_N_1 = z1.zoningRulesCodeName
	and z.uses = 'MF'
-- add institutional permissions
Insert into ZoningPermissions (
	zoningRulesCode,
	SpaceTypeID,
	minIntensityPermitted,
	maxIntensityPermitted,
	acknowledgedUse,
	penaltyAcknowledgedSpace,
	penaltyAcknowledgedLand,
	servicesRequirement)
 select 	
	z1.ZoningRulesCode, 5,
	z.Min_FAR*43560.0, 
	z.maxFAR*43560.0,0,0,0,1 
	from zoningTemp z, ZoningRulesI z1
	where z.ZONING_N_1 = z1.zoningRulesCodeName
	and z.uses = 'INS'
-- add industrial permissions
Insert into ZoningPermissions (
	zoningRulesCode,
	SpaceTypeID,
	minIntensityPermitted,
	maxIntensityPermitted,
	acknowledgedUse,
	penaltyAcknowledgedSpace,
	penaltyAcknowledgedLand,
	servicesRequirement)
 select 	
	z1.ZoningRulesCode, 2,
	z.Min_FAR*43560.0, 
	z.maxFAR*43560.0,0,0,0,1 
	from zoningTemp z, ZoningRulesI z1
	where z.ZONING_N_1 = z1.zoningRulesCodeName
	and z.uses = 'IND'
-- Do it all again for Use2, which was a separate column showing the second permitted use
-- add agricultural permissions
Insert into ZoningPermissions (
	zoningRulesCode,
	SpaceTypeID,
	minIntensityPermitted,
	maxIntensityPermitted,
	acknowledgedUse,
	penaltyAcknowledgedSpace,
	penaltyAcknowledgedLand,
	servicesRequirement)
 select 	
	z1.ZoningRulesCode, 1,
	z.Min_FAR*43560.0, 
	z.maxFAR*43560.0,0,0,0,0 
	from zoningTemp z, ZoningRulesI z1
	where z.ZONING_N_1 = z1.zoningRulesCodeName
	and z.use2 = 'A'
-- add commercal-office permissions
Insert into ZoningPermissions (
	zoningRulesCode,
	SpaceTypeID,
	minIntensityPermitted,
	maxIntensityPermitted,
	acknowledgedUse,
	penaltyAcknowledgedSpace,
	penaltyAcknowledgedLand,
	servicesRequirement)
 select 	
	z1.ZoningRulesCode, 4,
	z.Min_FAR*43560.0, 
	z.maxFAR*43560.0,0,0,0,1 
	from zoningTemp z, ZoningRulesI z1
	where z.ZONING_N_1 = z1.zoningRulesCodeName
	and z.use2 = 'C'
-- also commercal-retail permissions
Insert into ZoningPermissions (
	zoningRulesCode,
	SpaceTypeID,
	minIntensityPermitted,
	maxIntensityPermitted,
	acknowledgedUse,
	penaltyAcknowledgedSpace,
	penaltyAcknowledgedLand,
	servicesRequirement)
 select 	
	z1.ZoningRulesCode, 3,
	z.Min_FAR*43560.0, 
	z.maxFAR*43560.0,0,0,0,1 
	from zoningTemp z, ZoningRulesI z1
	where z.ZONING_N_1 = z1.zoningRulesCodeName
	and z.use2 = 'C'
-- add single family permissions
Insert into ZoningPermissions (
	zoningRulesCode,
	SpaceTypeID,
	minIntensityPermitted,
	maxIntensityPermitted,
	acknowledgedUse,
	penaltyAcknowledgedSpace,
	penaltyAcknowledgedLand,
	servicesRequirement)
 select 	
	z1.ZoningRulesCode, 7,
	z.Min_FAR*43560.0, 
	z.maxFAR*43560.0,0,0,0,1 
	from zoningTemp z, ZoningRulesI z1
	where z.ZONING_N_1 = z1.zoningRulesCodeName
	and z.use2 = 'SF'
-- add multi family permissions
Insert into ZoningPermissions (
	zoningRulesCode,
	SpaceTypeID,
	minIntensityPermitted,
	maxIntensityPermitted,
	acknowledgedUse,
	penaltyAcknowledgedSpace,
	penaltyAcknowledgedLand,
	servicesRequirement)
 select 	
	z1.ZoningRulesCode, 8,
	z.Min_FAR*43560.0, 
	z.maxFAR*43560.0,0,0,0,1 
	from zoningTemp z, ZoningRulesI z1
	where z.ZONING_N_1 = z1.zoningRulesCodeName
	and z.use2 = 'MF'
-- add institutional permissions
Insert into ZoningPermissions (
	zoningRulesCode,
	SpaceTypeID,
	minIntensityPermitted,
	maxIntensityPermitted,
	acknowledgedUse,
	penaltyAcknowledgedSpace,
	penaltyAcknowledgedLand,
	servicesRequirement)
 select 	
	z1.ZoningRulesCode, 5,
	z.Min_FAR*43560.0, 
	z.maxFAR*43560.0,0,0,0,1 
	from zoningTemp z, ZoningRulesI z1
	where z.ZONING_N_1 = z1.zoningRulesCodeName
	and z.use2 = 'INS'
-- add industrial permissions
Insert into ZoningPermissions (
	zoningRulesCode,
	SpaceTypeID,
	minIntensityPermitted,
	maxIntensityPermitted,
	acknowledgedUse,
	penaltyAcknowledgedSpace,
	penaltyAcknowledgedLand,
	servicesRequirement)
 select 	
	z1.ZoningRulesCode, 2,
	z.Min_FAR*43560.0, 
	z.maxFAR*43560.0,0,0,0,1 
	from zoningTemp z, ZoningRulesI z1
	where z.ZONING_N_1 = z1.zoningRulesCodeName
	and z.use2 = 'IND'


---******************----
--Now add uniqueID to zoningpermissions
alter table zoningpermissions add uniqueid int primary key identity(1,1) 

----------
--- set no change, derelict, demolition, renovation, addition, newDevelopment possibilities to true
update ZoningRulesI 
   set noChangePossibilities=1,
	derilictionPossibilities=1,
	demolitionPossibilities=1,
	renovationPossibilities=1,
	additionPossibilities=1,
	newDevelopmentPossibilities=1

--- create ParcelZoningXRef
select p.PARID, 
	z.zoningrulescode,
	2000 as YearEffective
  into ParcelZoningXRef
 from [ATLANTAREGION\Wei]._ful_zoning p, zoningrulesi z
  where p.ZONING_N_1=z.ZoningRulesCodeName 

--- create ParcelFeeXRef
select p.PARID, 
	1 as feeScheduleID,
	2000 as YearEffective
  into ParcelFeeXRef
 from  dbo._Fulton_Parcel_withTax p

--- create ParcelCostXRef
select p.PARID, 
	1 as costScheduleID,
	2000 as YearEffective
  into ParcelCostXRef
 from  dbo._Fulton_Parcel_withTax p

-- create fee information table to be filled in
create table developmentFees (
	feeScheduleID int,
	spaceTypeID int,
	DevelopmentFeePerUnitSpaceInitial float,
	DevelopmentFeePerUnitLandInitial float,
	DevelopmentFeePerUnitSpaceOngoing float,
	DevelopmentFeePerUnitLandOngoing float)

insert into developmentFees 
	select 1,s.spaceTypeID,0,0,0,0 from SpaceTypesI s

-- create cost information table to be filled in
create table transitionCostCodes (
	costScheduleID int,
	HighCapacityServicesInstallationCost float,
	LowCapacityServicesInstallationCost float,
	BrownfieldCleanupCost float)

insert into transitionCostCodes 
	select 1,0,0,0

create table transitionCosts (
	costScheduleID int,
	spaceTypeID int,
	DemolitionCost float,
	RenovationCost float,
	AdditionCost float,
	ConstructionCost float)

insert into transitionCosts 
	select 1,s.spaceTypeID,10,20,100,100 from SpaceTypesI s

-- create view of landusecodes with spacetypeid's
create view landusecode2 as
   select l.*, s.spacetypeid from ful_landuse_code l,spacetypesI s
   where l.spacetype_code=s.spacetypecode

-- CLEANUP micro-parcels by merging together
-- WEI is going to run some queries and then copy the SQL into this
-- file so that it can be reproduced when the tax parcel file changes
-- WEI can delete his name here AFTER he copies in the SQL script into this section




-- END OF WEI's PASTED SQL scripts for cleaning up micro parcels

-- now create the pecas parcel file
select 
	parid, 
	TAZ05 as taz,
	SD as luz,
	TotParcAcr as acres,
	lucs.spacetypeid,
	heated_floorspace as spacequantity,
	yr_built,
	1 as availableServicesCode
into fulton_parcels
	from dbo._Fulton_Parcel_withTax tp, landusecode2 lucs
where
	tp.LUC = lucs.code

-- need to set yearbuilt to 1980 if there is no year-built
update fulton_parcels
  set yr_built = 1980 where yr_built is null


-- create the view that finds the most recent zoning change
-- TODO should we check to make sure every parcel has zoning?


-- set up the current year.
select 2000 as currentyear into currentYearTable
-- perhaps we have a table with currentyear.
drop view  mostRecentZoningYear
go
create view mostRecentZoningYear as
  select 
	parid, max(yeareffective) as currentZoningYear
	 from parcelzoningxref, currentYearTable
	where yeareffective <= currentYearTable.currentYear
	group by parid

create view mostRecentFeeYear as
  select 
	parid, max(yeareffective) as currentFeeYear
	 from parcelfeexref, currentYearTable
	where yeareffective <= currentYearTable.currentYear
	group by parid

drop view mostRecentCostYear
create view mostRecentCostYear as
  select 
	parid, max(yeareffective) as currentCostYear
	 from parcelcostxref, currentYearTable
	where yeareffective <= currentYearTable.currentYear
	group by parid

-- Now this is the view that PECAS can use
drop view fulton_parcels_view
go
create view fulton_parcels_view as 
select
	fp.*, zxref.zoningRulesCode, costxref.costscheduleID,
	feexref.feescheduleID
  from fulton_parcels fp, mostRecentZoningYear z, 
	mostRecentFeeYear f, mostRecentCostYear c,
		parcelzoningxref zxref,
                parcelfeexref feexref,
		parcelcostxref costxref
  where fp.parid=z.parid and fp.parid=zxref.parid
	and zxref.yeareffective=z.currentZoningYear
	and fp.parid=feexref.parid and fp.parid=f.parid
	and feexref.yeareffective=f.currentfeeyear 
	and fp.parid=costxref.parid and fp.parid=c.parid
	and costxref.yeareffective=c.currentcostyear

