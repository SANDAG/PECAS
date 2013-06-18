-- Function: _pgoverlap(character varying, character varying, character varying, character varying, character varying)

-- DROP FUNCTION _pgoverlap(character varying, character varying, character varying, character varying, character varying);

CREATE OR REPLACE FUNCTION _pgoverlap(schema_a character varying, table_a character varying, a_id character varying, table_result1 character varying, table_result2 character varying)
  RETURNS void AS
$BODY$

/*
$Id: pgoverlap_wiki.sql 2011-03-09 15:19Z Birgit Laggner $

pgoverlap - checks table for overlapping geometries separating between sliver and non-sliver overlappings, returns table with non-overlapping geometries and adjusted sliver-overlapping polygons as well as table with non-sliver overlappings (for later treatment)

schema_a:      database schema where table is located
table_a:       table name
a_id:          id column name in table
table_result1: name of 1st result table (non-overlapping polygons)
table_result2: name of 2nd result table (overlapping polygons)

Preconditions for successful use of the function:
- gid column of data type integer or serial, unique, with btree index!!
- the_geom column of data type geometry and geometry type POLYGON, with gist index!!
- column of data type integer (name is variable) for subsequent joining of attributes, with btree index!!
- geometries should be valid (otherwise number of exceptions rises)

Caution: Do not call the function twice at a time!! - Intermediate tables have fixed names and are locked during function processing (otherwise their content would be overwritten).

Copyright (C) 2011 Johann Heinrich von Thünen-Institute (vTI) - Federal Research Institute for Rural Areas, Forestry and Fisheries, Institute of Rural Studies, Braunschweig, Germany (http://www.vti.bund.de)
Version 0.1
contact: birgit dot laggner at vti dot bund dot de

This is free software; you can redistribute and/or modify it under
the terms of the GNU General Public Licence. This software is without any warrenty and you use it at your own risk.
*/

DECLARE

i integer;
sql_1 text;


BEGIN

--find all overlappings with geometrytype Polygon:

EXECUTE
'DROP TABLE IF EXISTS '||schema_a||'.pgoverlap_tmp1;';

EXECUTE
'CREATE TABLE '||schema_a||'.pgoverlap_tmp1 (
gid serial,
a_'||a_id||' integer,
b_'||a_id||' integer,
a_geom geometry,
b_geom geometry,
i_geom geometry,
area numeric,
perimeter2 numeric,
perimeter2_uc numeric,
cmp numeric,
sliver character varying(1)
 );';

RAISE NOTICE 'Find all overlappings with geometrytype Polygon. ';

EXECUTE
'INSERT INTO '||schema_a||'.pgoverlap_tmp1 (
a_'||a_id||', b_'||a_id||', a_geom, b_geom, i_geom)
SELECT DISTINCT
sel.a_'||a_id||',
sel.b_'||a_id||',
sel.a_geom,
sel.b_geom,
sel.i_geom
FROM (
SELECT
a.'||a_id||' AS a_'||a_id||',
b.'||a_id||' AS b_'||a_id||',
a.the_geom AS a_geom,
b.the_geom AS b_geom,
_cleanGeometry4((ST_Dump(ST_Intersection(a.the_geom,b.the_geom))).geom) AS i_geom
FROM
'||schema_a||'.'||table_a||' a
LEFT JOIN
'||schema_a||'.'||table_a||' b
ON a.the_geom && b.the_geom
WHERE
a.gid < b.gid AND
ST_Intersects(a.the_geom, b.the_geom)) AS sel
WHERE
ST_GeometryType(sel.i_geom) NOT IN (
''ST_Point'',
''ST_LineString'',
''ST_MultiPoint'',
''ST_MultiLineString'',
''ST_Line'');';

EXECUTE
'CREATE INDEX pgoverlap_tmp1_gid_btree
ON '||schema_a||'.pgoverlap_tmp1
USING btree(gid);';

EXECUTE
'CREATE INDEX pgoverlap_tmp1_a_'||a_id||'_btree
ON '||schema_a||'.pgoverlap_tmp1
USING btree(a_'||a_id||');';

EXECUTE
'CREATE INDEX pgoverlap_tmp1_b_'||a_id||'_btree
ON '||schema_a||'.pgoverlap_tmp1
USING btree(b_'||a_id||');';


--Check for sliver polygons

RAISE NOTICE 'Check for sliver polygons. ';

--1. Calculation of area and perimeter:

EXECUTE
'UPDATE '||schema_a||'.pgoverlap_tmp1 
SET area=ST_Area(i_geom),
    perimeter2=ST_Perimeter(i_geom)^2;';

--2. Calculation of circle perimeter with area equal to area of the polygon:
EXECUTE
'UPDATE '||schema_a||'.pgoverlap_tmp1 
SET perimeter2_uc=4*pi()*area;';


--3. Calculation of Compactness Index (cmp):

EXECUTE
'UPDATE '||schema_a||'.pgoverlap_tmp1 
SET cmp=perimeter2_uc/perimeter2;';

--4. Decision: sliver polygon, true or false?:

EXECUTE
'UPDATE '||schema_a||'.pgoverlap_tmp1 SET sliver=''t'' WHERE area < 6000000000;';

EXECUTE
'UPDATE '||schema_a||'.pgoverlap_tmp1 SET sliver=''t'' WHERE cmp < 0.4;';

EXECUTE
'UPDATE '||schema_a||'.pgoverlap_tmp1 SET sliver=''f'' WHERE sliver IS NULL;';
/*
EXECUTE
'UPDATE '||schema_a||'.pgoverlap_tmp1 SET sliver=''f'' WHERE area > 150;';
*/

--Create table for sliver polygons:

EXECUTE
'DROP TABLE IF EXISTS '||schema_a||'.pgoverlap_tmp2;';

EXECUTE
'CREATE TABLE '||schema_a||'.pgoverlap_tmp2 (
gid serial,
a_'||a_id||' integer,
a_geom geometry,
i_geom_union geometry,
the_geom geometry
);';

RAISE NOTICE 'Processing sliver polygons. ';

EXECUTE
'INSERT INTO '||schema_a||'.pgoverlap_tmp2
(a_'||a_id||', a_geom, i_geom_union)
SELECT a_'||a_id||', a_geom, CASE WHEN ST_Union(i_geom) IS NULL THEN ST_Collect(i_geom) ELSE _cleanGeometry4(ST_Union(i_geom)) END
FROM '||schema_a||'.pgoverlap_tmp1
WHERE sliver=''t''
GROUP BY a_'||a_id||', a_geom;';

i:=0;

sql_1:='SELECT gid FROM '||schema_a||'.pgoverlap_tmp2 ORDER BY gid;';

FOR i IN EXECUTE sql_1 LOOP
BEGIN

EXECUTE
'UPDATE '||schema_a||'.pgoverlap_tmp2 SET the_geom=ST_Difference(a_geom, i_geom_union) WHERE gid='||i||';';

EXCEPTION WHEN internal_error THEN

EXECUTE
'UPDATE '||schema_a||'.pgoverlap_tmp2 SET the_geom=ST_Difference(ST_Buffer(a_geom,0.0), ST_Buffer(i_geom_union,0.0)) WHERE gid='||i||';';

END;
END LOOP;

EXECUTE
'CREATE INDEX pgoverlap_tmp2_a_'||a_id||'_btree
ON '||schema_a||'.pgoverlap_tmp2
USING btree(a_'||a_id||');';


--Create table with IDs of sliver polygons:

RAISE NOTICE 'Creating table with IDs of sliver polygons. ';

EXECUTE
'DROP TABLE IF EXISTS '||schema_a||'.pgoverlap_tmp3;';

EXECUTE
'CREATE TABLE '||schema_a||'.pgoverlap_tmp3 (
'||a_id||' integer);';

EXECUTE
'INSERT INTO '||schema_a||'.pgoverlap_tmp3 ('||a_id||')
SELECT DISTINCT a_'||a_id||'
FROM '||schema_a||'.pgoverlap_tmp2;';

EXECUTE
'INSERT INTO '||schema_a||'.pgoverlap_tmp3 ('||a_id||')
SELECT DISTINCT a.b_'||a_id||'
FROM
'||schema_a||'.pgoverlap_tmp1 a
LEFT JOIN
'||schema_a||'.pgoverlap_tmp2 b
ON a.b_'||a_id||'=b.a_'||a_id||'
WHERE b.a_'||a_id||' IS NULL;';


--Insert non-sliver polygons into separate table:

BEGIN
EXECUTE
'SELECT DropGeometryTable('''||schema_a||''','''||table_result2||''');';
EXCEPTION WHEN undefined_table THEN 
END;

EXECUTE
'CREATE TABLE '||schema_a||'.'||table_result2||' (
 gid serial PRIMARY KEY, 
 a_'||a_id||' integer,
 b_'||a_id||' integer
 );';

EXECUTE
'SELECT AddGeometryColumn('''||schema_a||''', '''||table_result2||''',''a_geom'',(SELECT DISTINCT ST_SRID(the_geom) FROM '||schema_a||'.'||table_a||'),''POLYGON'',2);';

EXECUTE
'ALTER TABLE '||schema_a||'.'||table_result2||' DROP CONSTRAINT enforce_geotype_a_geom';

EXECUTE
'SELECT AddGeometryColumn('''||schema_a||''', '''||table_result2||''',''b_geom'',(SELECT DISTINCT ST_SRID(the_geom) FROM '||schema_a||'.'||table_a||'),''POLYGON'',2);';

EXECUTE
'ALTER TABLE '||schema_a||'.'||table_result2||' DROP CONSTRAINT enforce_geotype_b_geom';

EXECUTE
'SELECT AddGeometryColumn('''||schema_a||''', '''||table_result2||''',''i_geom'',(SELECT DISTINCT ST_SRID(the_geom) FROM '||schema_a||'.'||table_a||'),''MULTIPOLYGON'',2);';

EXECUTE
'ALTER TABLE '||schema_a||'.'||table_result2||' DROP CONSTRAINT enforce_geotype_i_geom';


RAISE NOTICE 'Inserting non-sliver polygons into separate table. ';

EXECUTE
'INSERT INTO '||schema_a||'.'||table_result2||' (
 a_'||a_id||',
 b_'||a_id||',
 a_geom,
 b_geom,
 i_geom)
SELECT 
 a_'||a_id||',
 b_'||a_id||',
 a_geom,
 b_geom,
 i_geom
FROM '||schema_a||'.pgoverlap_tmp1
WHERE sliver=''f'';';


--create table with IDs of non-sliver polygons:

RAISE NOTICE 'creating table with IDs of non-sliver polygons. ';

EXECUTE
'DROP TABLE IF EXISTS '||schema_a||'.pgoverlap_tmp4;';

EXECUTE
'CREATE TABLE '||schema_a||'.pgoverlap_tmp4 (
'||a_id||' integer);';

EXECUTE
'INSERT INTO '||schema_a||'.pgoverlap_tmp4 ('||a_id||')
SELECT DISTINCT a_'||a_id||'
FROM '||schema_a||'.'||table_result2||';';

EXECUTE
'INSERT INTO '||schema_a||'.pgoverlap_tmp4 ('||a_id||')
SELECT DISTINCT a.b_'||a_id||'
FROM
'||schema_a||'.'||table_result2||' a
LEFT JOIN
'||schema_a||'.'||table_result2||' b
ON a.b_'||a_id||'=b.a_'||a_id||'
WHERE b.a_'||a_id||' IS NULL;';


--Insert geometries without overlappings into new table:

EXECUTE
'DROP TABLE IF EXISTS '||schema_a||'.pgoverlap_tmp5;';

EXECUTE
'CREATE TABLE '||schema_a||'.pgoverlap_tmp5 (
 gid serial PRIMARY KEY, 
 '||a_id||' integer,
 the_geom geometry
 );';

EXECUTE
'INSERT INTO '||schema_a||'.pgoverlap_tmp5
 ('||a_id||', the_geom)
SELECT 
 a.'||a_id||',
 a.the_geom
FROM
'||schema_a||'.'||table_a||' a
LEFT JOIN
'||schema_a||'.pgoverlap_tmp3 b
ON a.'||a_id||'=b.'||a_id||'
LEFT JOIN
'||schema_a||'.pgoverlap_tmp4 c
ON a.'||a_id||'=c.'||a_id||'
WHERE
b.'||a_id||' IS NULL AND
c.'||a_id||' IS NULL;';


--Create and merge result table:

BEGIN
EXECUTE
'SELECT DropGeometryTable('''||schema_a||''','''||table_result1||''');';
EXCEPTION WHEN undefined_table THEN 
END;

EXECUTE
'CREATE TABLE '||schema_a||'.'||table_result1||' (
gid serial PRIMARY KEY,
'||a_id||' integer
);';
EXECUTE
'SELECT AddGeometryColumn('''||schema_a||''','''||table_result1||''',''the_geom'',(SELECT DISTINCT ST_SRID(the_geom) FROM '||schema_a||'.'||table_a||'),''POLYGON'',2);';
EXECUTE
'ALTER TABLE '||schema_a||'.'||table_result1||' DROP CONSTRAINT enforce_geotype_the_geom;';


--Insert Difference (sliver overlappings) into result table:

EXECUTE
'INSERT INTO '||schema_a||'.'||table_result1||' (
'||a_id||',
the_geom
)
SELECT
a_'||a_id||',
(ST_Dump(the_geom)).geom
FROM
'||schema_a||'.pgoverlap_tmp2
WHERE ST_IsEmpty(the_geom)=''f'';';


--Insert unchanged polygons (sliver overlappings) into result table:

EXECUTE
'INSERT INTO '||schema_a||'.'||table_result1||' (
'||a_id||',
the_geom
)
SELECT DISTINCT
a.b_'||a_id||',
a.b_geom
FROM
'||schema_a||'.pgoverlap_tmp1 a
LEFT JOIN
'||schema_a||'.pgoverlap_tmp2 b
ON a.b_'||a_id||'=b.a_'||a_id||'
WHERE 
a.sliver=''t'' AND
b.a_'||a_id||' IS NULL;';


--Insert non-overlapping polygons into result table:

EXECUTE
'INSERT INTO '||schema_a||'.'||table_result1||' (
'||a_id||',
the_geom
)
SELECT
'||a_id||',
the_geom
FROM '||schema_a||'.pgoverlap_tmp5;';


--Update geometries already processed as sliver overlappings which also have non-sliver overlappings:

EXECUTE
'UPDATE '||schema_a||'.'||table_result2||' t SET a_geom=a.geom FROM (SELECT '||a_id||', CASE WHEN ST_Union(the_geom) IS NULL THEN ST_Collect(the_geom) ELSE ST_Union(the_geom) END AS geom FROM '||schema_a||'.'||table_result1||' GROUP BY '||a_id||') a WHERE t.a_'||a_id||'=a.'||a_id||';';

EXECUTE
'UPDATE '||schema_a||'.'||table_result2||' t SET b_geom=a.geom FROM (SELECT '||a_id||', CASE WHEN ST_Union(the_geom) IS NULL THEN ST_Collect(the_geom) ELSE ST_Union(the_geom) END AS geom FROM '||schema_a||'.'||table_result1||' GROUP BY '||a_id||') a WHERE t.b_'||a_id||'=a.'||a_id||';';

EXECUTE
'DELETE FROM '||schema_a||'.'||table_result1||' WHERE '||a_id||' IN (SELECT '||a_id||' FROM '||schema_a||'.pgoverlap_tmp4);';

--Remove tmp tables:

EXECUTE
'DROP TABLE '||schema_a||'.pgoverlap_tmp1;';
EXECUTE
'DROP TABLE '||schema_a||'.pgoverlap_tmp2;';
EXECUTE
'DROP TABLE '||schema_a||'.pgoverlap_tmp3;';
EXECUTE
'DROP TABLE '||schema_a||'.pgoverlap_tmp4;';
EXECUTE
'DROP TABLE '||schema_a||'.pgoverlap_tmp5;';


END;
$BODY$
  LANGUAGE 'plpgsql' VOLATILE
  COST 100;
ALTER FUNCTION _pgoverlap(character varying, character varying, character varying, character varying, character varying) OWNER TO postgres;
