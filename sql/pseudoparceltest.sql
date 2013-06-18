
-- select two parcels from the parcel data set for testing.
drop table if exists pseudoparceltest;
create table pseudoparceltest with oids as select tricountymerge16.* from tricountymerge16 where (parcel_no='11 01 01 02 000 004.000' or parcel_no='11 02 10 02 000 001.000');
alter table pseudoparceltest drop column the_geom;
SELECT AddGeometryColumn('pseudoparceltest', 'the_geom', 26916, 'MULTIPOLYGON', 2);
update pseudoparceltest as p set the_geom=tricountymerge16.the_geom from tricountymerge16 where 
   p.parcel_no = tricountymerge16.parcel_no;

-- now create the pseduo_parcels, 20x20m.
drop table if exists my_parcel_shards;
Select p.parcel_no,
	p.hor_n as shard_hpos,
	p.ver_n as shard_vpos,
	0::int as initialPseudoParcel,
	null::int as pseudoParcel,
	ST_Intersection(p.the_geom,ST_Translate(
		p.boxref,p.hor_n*20,p.ver_n*20)) as the_shard
	into my_parcel_shards
	from (select parcel_no, hor.n as hor_n, ver.n as ver_n,
			the_geom,
			ST_SetSRID(ST_MakeBox2D(
				ST_MakePoint(ST_Xmin(the_geom),	ST_Ymin(the_geom)),
				ST_MakePoint(ST_Xmin(the_geom) + 20, ST_Ymin(the_geom) + 20)),
			26916) as BoxRef FROM pseudoparceltest
	cross join generate_series(0,99) As hor(n)
	cross join generate_series(0,99) as ver(n)) p
where
	ST_Intersects(p.the_geom, ST_Translate(p.boxref, p.hor_n*20, p.ver_n*20));

-- to restart
update my_parcel_shards set pseudoparcel=null;

-- just to look at it.
-- select GeometryType(the_shard),ST_Area(the_shard) as area,parcel_no,shard_hpos,shard_vpos from my_parcel_shards order by geometrytype, area;

-- set the srid constraint (needed?)
-- select dropGeometryColumn('my_parcel_shards', 'shard_geom');
--select addGeometryColumn('my_parcel_shards', 'shard_geom', 26916, 'GEOMETRY', 2);
--update my_parcel_shards set shard_geom=the_shard;
--alter table my_parcel_shards drop the_shard;

-- add key field for QGIS
--alter table my_parcel_shards drop keyfield;
alter table my_parcel_shards add keyfield serial primary key;

-- select count(keyfield), pseudoparcel, parcel_no from my_parcel_shards group by pseudoparcel, parcel_no;
create index shard_index on my_parcel_shards (parcel_no,shard_hpos,shard_vpos);


--create view that tells us our options for taking bottom-left shards to make 2500to2900 sq m groups.
drop view positionoptions cascade;
create view positionoptions as 
SELECT f3.parcel_no, f3.shard_hpos, f3.shard_vpos, amt_used, 
		f3.shard_hpos*1.000001+f3.shard_vpos as compositePosition
		FROM (SELECT f1.parcel_no, f1.shard_hpos, f1.shard_vpos,
			SUM(ST_Area(f2.the_shard)) As amt_used
				FROM my_parcel_shards f1 INNER JOIN 
					my_parcel_shards f2
					ON (f1.parcel_no = f2.parcel_no)
				WHERE   f1.shard_hpos >= f2.shard_hpos AND
					f1.shard_vpos >= f2.shard_vpos
					AND f1.pseudoParcel IS NULL
					AND f2.pseudoParcel IS NULL
				group by f1.parcel_no, f1.shard_hpos, f1.shard_vpos
				order by parcel_no, f1.shard_hpos, f1.shard_vpos
		) f3
			where f3.amt_used >= 6400 AND f3.amt_used <= 10000
			order by parcel_no, compositePosition;

-- pick the options that has the smallest "composite position"
--drop view bestPositionOption;
-- TODO this currently works its way up the diagonal.  It might be nice to 
-- count the vertical extent in npos and the horizontal extent in npos
-- and try to pick options that are more square, instead of options that are 
-- close to the diagonal.
create view bestPositionOption as select p.* from positionoptions p,
	(select min(compositeposition) as mincompositeposition, parcel_no from positionoptions group by parcel_no) f
where p.compositeposition = f.mincompositeposition and
	p.parcel_no=f.parcel_no;

UPDATE my_parcel_shards
    SET pseudoParcel = 1
    FROM bestPositionOption b
WHERE my_parcel_shards.parcel_no = b.parcel_no
    AND my_parcel_shards.shard_hpos <= b.shard_hpos
    AND my_parcel_shards.shard_vpos <= b.shard_vpos
    AND pseudoParcel IS NULL; 

-- select * from my_parcel_shards where pseudoParcel=1;

UPDATE my_parcel_shards
    SET pseudoParcel = 2
    FROM bestPositionOption b
WHERE my_parcel_shards.parcel_no = b.parcel_no
    AND my_parcel_shards.shard_hpos <= b.shard_hpos
    AND my_parcel_shards.shard_vpos <= b.shard_vpos
    AND pseudoParcel IS NULL; 

UPDATE my_parcel_shards
    SET pseudoParcel = 3
    FROM bestPositionOption b
WHERE my_parcel_shards.parcel_no = b.parcel_no
    AND my_parcel_shards.shard_hpos <= b.shard_hpos
    AND my_parcel_shards.shard_vpos <= b.shard_vpos
    AND pseudoParcel IS NULL; 

UPDATE my_parcel_shards
    SET pseudoParcel = 4
    FROM bestPositionOption b
WHERE my_parcel_shards.parcel_no = b.parcel_no
    AND my_parcel_shards.shard_hpos <= b.shard_hpos
    AND my_parcel_shards.shard_vpos <= b.shard_vpos
    AND pseudoParcel IS NULL; 

UPDATE my_parcel_shards
    SET pseudoParcel = 5
    FROM bestPositionOption b
WHERE my_parcel_shards.parcel_no = b.parcel_no
    AND my_parcel_shards.shard_hpos <= b.shard_hpos
    AND my_parcel_shards.shard_vpos <= b.shard_vpos
    AND pseudoParcel IS NULL; 

UPDATE my_parcel_shards
    SET pseudoParcel = 6
    FROM bestPositionOption b
WHERE my_parcel_shards.parcel_no = b.parcel_no
    AND my_parcel_shards.shard_hpos <= b.shard_hpos
    AND my_parcel_shards.shard_vpos <= b.shard_vpos
    AND pseudoParcel IS NULL; 

UPDATE my_parcel_shards
    SET pseudoParcel = 7
    FROM bestPositionOption b
WHERE my_parcel_shards.parcel_no = b.parcel_no
    AND my_parcel_shards.shard_hpos <= b.shard_hpos
    AND my_parcel_shards.shard_vpos <= b.shard_vpos
    AND pseudoParcel IS NULL; 

UPDATE my_parcel_shards
    SET pseudoParcel = 8
    FROM bestPositionOption b
WHERE my_parcel_shards.parcel_no = b.parcel_no
    AND my_parcel_shards.shard_hpos <= b.shard_hpos
    AND my_parcel_shards.shard_vpos <= b.shard_vpos
    AND pseudoParcel IS NULL; 

UPDATE my_parcel_shards
    SET pseudoParcel = 9
    FROM bestPositionOption b
WHERE my_parcel_shards.parcel_no = b.parcel_no
    AND my_parcel_shards.shard_hpos <= b.shard_hpos
    AND my_parcel_shards.shard_vpos <= b.shard_vpos
    AND pseudoParcel IS NULL; 

UPDATE my_parcel_shards
    SET pseudoParcel = 10
    FROM bestPositionOption b
WHERE my_parcel_shards.parcel_no = b.parcel_no
    AND my_parcel_shards.shard_hpos <= b.shard_hpos
    AND my_parcel_shards.shard_vpos <= b.shard_vpos
    AND pseudoParcel IS NULL; 

UPDATE my_parcel_shards
    SET pseudoParcel = 11
    FROM bestPositionOption b
WHERE my_parcel_shards.parcel_no = b.parcel_no
    AND my_parcel_shards.shard_hpos <= b.shard_hpos
    AND my_parcel_shards.shard_vpos <= b.shard_vpos
    AND pseudoParcel IS NULL; 

UPDATE my_parcel_shards
    SET pseudoParcel = 12
    FROM bestPositionOption b
WHERE my_parcel_shards.parcel_no = b.parcel_no
    AND my_parcel_shards.shard_hpos <= b.shard_hpos
    AND my_parcel_shards.shard_vpos <= b.shard_vpos
    AND pseudoParcel IS NULL; 

UPDATE my_parcel_shards
    SET pseudoParcel = 13
    FROM bestPositionOption b
WHERE my_parcel_shards.parcel_no = b.parcel_no
    AND my_parcel_shards.shard_hpos <= b.shard_hpos
    AND my_parcel_shards.shard_vpos <= b.shard_vpos
    AND pseudoParcel IS NULL; 

UPDATE my_parcel_shards
    SET pseudoParcel = 14
    FROM bestPositionOption b
WHERE my_parcel_shards.parcel_no = b.parcel_no
    AND my_parcel_shards.shard_hpos <= b.shard_hpos
    AND my_parcel_shards.shard_vpos <= b.shard_vpos
    AND pseudoParcel IS NULL; 

UPDATE my_parcel_shards
    SET pseudoParcel = 15
    FROM bestPositionOption b
WHERE my_parcel_shards.parcel_no = b.parcel_no
    AND my_parcel_shards.shard_hpos <= b.shard_hpos
    AND my_parcel_shards.shard_vpos <= b.shard_vpos
    AND pseudoParcel IS NULL; 

UPDATE my_parcel_shards
    SET pseudoParcel = 16
    FROM bestPositionOption b
WHERE my_parcel_shards.parcel_no = b.parcel_no
    AND my_parcel_shards.shard_hpos <= b.shard_hpos
    AND my_parcel_shards.shard_vpos <= b.shard_vpos
    AND pseudoParcel IS NULL; 

UPDATE my_parcel_shards
    SET pseudoParcel = 17
    FROM bestPositionOption b
WHERE my_parcel_shards.parcel_no = b.parcel_no
    AND my_parcel_shards.shard_hpos <= b.shard_hpos
    AND my_parcel_shards.shard_vpos <= b.shard_vpos
    AND pseudoParcel IS NULL; 

UPDATE my_parcel_shards
    SET pseudoParcel = 18
    FROM bestPositionOption b
WHERE my_parcel_shards.parcel_no = b.parcel_no
    AND my_parcel_shards.shard_hpos <= b.shard_hpos
    AND my_parcel_shards.shard_vpos <= b.shard_vpos
    AND pseudoParcel IS NULL; 

UPDATE my_parcel_shards
    SET pseudoParcel = 19
    FROM bestPositionOption b
WHERE my_parcel_shards.parcel_no = b.parcel_no
    AND my_parcel_shards.shard_hpos <= b.shard_hpos
    AND my_parcel_shards.shard_vpos <= b.shard_vpos
    AND pseudoParcel IS NULL; 

UPDATE my_parcel_shards
    SET pseudoParcel = 20
    FROM bestPositionOption b
WHERE my_parcel_shards.parcel_no = b.parcel_no
    AND my_parcel_shards.shard_hpos <= b.shard_hpos
    AND my_parcel_shards.shard_vpos <= b.shard_vpos
    AND pseudoParcel IS NULL; 