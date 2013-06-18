-- buffer and simplify polygons removing any resulting overlaps and slivers.

-- create the table for the simplified version
drop table if exists luz_simple;
create table luz_simple (gid serial, luz integer);
SELECT AddGeometryColumn('public', 'luz_simple','the_geom', (select distinct ST_SRID(the_geom) FROM luz),'MULTIPOLYGON',2);
alter table luz_simple drop CONSTRAINT enforce_geotype_the_geom;

-- add the simplified
insert into luz_simple
select gid,luz, cleanGeometry(ST_SimplifyPreserveTopology(the_geom, 500)) as the_geom1  
from luz;

-- create the table for the buffered/simplified version
drop table if exists luz_bf;
create table luz_bf (gid serial, luz integer);
SELECT AddGeometryColumn('public', 'luz_bf','the_geom', (select distinct ST_SRID(the_geom) FROM luz),'MULTIPOLYGON',2);
alter table luz_bf drop CONSTRAINT enforce_geotype_the_geom;

-- add the buffered and simplified
insert into luz_bf
select gid,luz, ST_Buffer(the_geom, 1000) as the_geom1  
from luz_simple;

-- create the table for places where the buffer intersects the simplified, not necessary for algorithm, just for looking.
drop table if exists luz_bf_intersects;
create table luz_bf_intersects (gid serial, luz_orig integer, luz_other integer);
SELECT AddGeometryColumn('public', 'luz_bf_intersects','the_geom', (select distinct ST_SRID(the_geom) FROM luz),'MULTIPOLYGON',2);
alter table luz_bf_intersects drop CONSTRAINT enforce_geotype_the_geom;

insert into luz_bf_intersects (luz_orig, luz_other, the_geom) 
select bf.luz, simp.luz, ST_Intersection(bf.the_geom, simp.the_geom)
from luz_bf bf, luz_simple simp
where ST_Intersects(simp.the_geom, bf.the_geom) and
simp.luz <> bf.luz;

-- create the table for buffered but clipped to not overlap simplified
drop table if exists luz_bfclipped;
create table luz_bfclipped (gid serial, luz integer);
SELECT AddGeometryColumn('public', 'luz_bfclipped','the_geom', (select distinct ST_SRID(the_geom) FROM luz),'MULTIPOLYGON',2);
-- I have no idea why I have to dorop this constraint, check with later versions of POSTGIS and see if it's still necessary
alter table luz_bfclipped drop CONSTRAINT enforce_geotype_the_geom;

-- table of parts to exclude
drop table if exists exclude_parts;
create table exclude_parts as 
select me.luz as not_this_luz, ST_Simplify(ST_Union(ST_Buffer(other.the_geom,50)),50) as all_the_other_geom
from luz_simple me, luz_simple other, luz_bf box_check
where me.luz <> other.luz
and me.luz = box_check.luz
and box_check.the_geom && other.the_geom -- only where the other LUZ is close to my buffer.
--and ST_IsValid(other.the_geom)
group by me.luz


-- want all of my simplified geometry but all of my buffer that does not overlap someone elses simplified geometry.
-- snap to grid here so that we don't get errors later.
truncate table luz_bfclipped;
insert into luz_bfclipped (luz, the_geom)
select luz_bf.luz, --ST_SnapToGrid(
ST_Union(simp.the_geom,ST_Difference(luz_bf.the_geom, t.all_the_other_geom))--,1) 
as the_geom
from luz_bf, luz_simple simp, exclude_parts t
where luz_bf.luz = t.not_this_luz
and simp.luz = luz_bf.luz;


--  make it just a bunch of polygons
drop table if exists luz_buff_indiv_polys;
create table luz_buff_indiv_polys (gid serial, luz integer, the_geom geometry);
insert into luz_buff_indiv_polys (luz, the_geom) 
SELECT luz, ST_Buffer(foo.geom,1) as the_geom--, ST_GeometryType(foo.geom) as type
FROM (SELECT l.luz, (ST_Dump(l.the_geom)).* 
FROM luz_bfclipped l) As foo
where ST_GeometryType(foo.geom)='ST_Polygon';

select * from luz_buff_indiv_polys where NOT ST_IsValid(the_geom);
-- everything valid

-- this is now our base system; a set of polygons that overlap but don't overlap the ORIGINAL SIMPLIFIED polygons.
-- so the overlapping parts can be added to one of the two fairly arbitrarily.
-- we'll add it to the one that is closest.
alter table luz_buff_indiv_polys add primary key (gid);
create index luz_buff_indiv_polys_gist on luz_buff_indiv_polys using gist(the_geom);

update luz_buff_indiv_polys i set the_geom=ST_Difference(i.the_geom, j.the_geom) 
from (select gid, the_geom from luz_buff_indiv_polys where gid =2) AS j
where i.gid=1

CREATE OR REPLACE FUNCTION remove_smaller_bits(tablename character varying, priorityfield character varying)
  RETURNS void AS
$BODY$
DECLARE
	gid_s text;
	i integer;
	istring text;
	gis2_s text;
	j integer;
	jstring text;
	qstring text;
BEGIN
	gid_s:= 'SELECT gid from '||tablename||' ORDER BY gid;';

	FOR i IN EXECUTE gid_s LOOP
		istring:= cast(i as text);
		RAISE NOTICE 'Processing GID %', istring;
		BEGIN
			gis2_s:= 'SELECT gid from '||tablename||' where gid > '||i||' ORDER BY gid;';
			FOR j in EXECUTE gis2_s LOOP
			jstring:=cast(j as text);
			BEGIN
				EXECUTE
				'UPDATE '||tablename||' t1 SET the_geom=ST_Difference(t1.the_geom, t2.the_geom) from (select gid, the_geom from '||tablename||' where gid ='||jstring||') as t2 where t1.gid = '||istring||' AND t1.the_geom && t2.the_geom;';
				EXCEPTION WHEN internal_error THEN
				RAISE NOTICE 'Problem with intersection % and %', istring, jstring;
			END;
			END LOOP;
		END;
END LOOP;
END;
$BODY$
  LANGUAGE 'plpgsql' VOLATILE
  COST 100;
ALTER FUNCTION remove_smaller_bits(character varying, character varying) OWNER TO postgres;

drop table if exists luz_buff_final;
create table luz_buff_final(gid serial, luz integer, the_geom geometry);
insert into luz_buff_final select * from luz_buff_indiv_polys;
alter table luz_buff_final add primary key (gid);
create index luz_buff_final_gist on luz_buff_final using gist(the_geom);

select remove_smaller_bits('luz_buff_final', 'thisdoesntmatter');

delete from luz_buff_final where ST_Area(the_geom) < 1000;

select count(*) from luz_buff_final;

drop table if exists luz_buff_final_simplified;
create table luz_buff_final_simplified(gid serial, luz integer, the_geom geometry);
insert into luz_buff_final_simplified select gid, luz, ST_Simplify(ST_Buffer(the_geom, 10),10) from luz_buff_final;
alter table luz_buff_final_simplified add primary key (gid);
create index luz_buff_final_simplified_gist on luz_buff_final_simplified using gist(the_geom);

select remove_smaller_bits('luz_buff_final_simplified', 'thisdoesntmatter');