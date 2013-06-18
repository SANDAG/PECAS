
CREATE OR REPLACE FUNCTION remove_overlaps_1(tablename_to_trim varchar, tablename_orig varchar)
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
        DROP TABLE IF EXISTS _trim_gids;
	EXECUTE
	'CREATE TABLE _trim_gids as SELECT t1.gid as my_gid, t2.gid as other_gid from '||tablename_to_trim||' t1, '||tablename_orig||' t2 where t2.gid<> t1.gid and t2.the_geom && t1.the_geom;';
	CREATE INDEX _trim_gids_idx_1 ON _trim_gids using btree(my_gid);
	CREATE INDEX _trim_gids_idx_2 ON _trim_gids using btree(other_gid);

	-- these are the shapes we are dealing with
	gid_s:= 'SELECT gid from '||tablename_to_trim ||' ORDER BY gid;';

	FOR i IN EXECUTE gid_s LOOP
		istring:= cast(i as text);
		RAISE NOTICE 'Processing GID %', istring;
		BEGIN
			-- could first try to deal with the COLLECT (or UNION) of the other polys, instead of dealing with them one by one, and do the one-by-one in another EXCEPTION clause
			
			--UPDATE tablename_to_trim t1 SET the_geom=ST_DIFFERENCE(t1.the_geom, t2.the_geom) from 
			--(select ST_Collect(the_geom) as the_geom from tablename_orig a, _trim_gids b where a.gid=b.other_gid and b.my_gid=istring) as t2 where t1.gid=istring;
			gis2_s:= 'SELECT other_gid from _trim_gids where my_gid = '||istring||';';
			FOR j in EXECUTE gis2_s LOOP
				jstring:=cast(j as text);
				BEGIN
					EXECUTE
					'UPDATE '||tablename_to_trim||' t1 SET the_geom=ST_Difference(t1.the_geom, t2.the_geom) from (select gid, the_geom from '||tablename_orig||' where gid ='||jstring||') as t2 where t1.gid = '||istring||';';

				EXCEPTION WHEN internal_error THEN
					RAISE NOTICE 'Problem with intersection % and %', istring, jstring;
				END;
			END LOOP;
			-- now union the original
			BEGIN
				EXECUTE
				'UPDATE '||tablename_to_trim||' t1 SET the_geom=ST_Union(t1.the_geom, t2.the_geom) from '||tablename_orig||' t2 where t1.gid ='||istring||' and t2.gid = '||istring||';';
			EXCEPTION WHEN internal_error THEN
				RAISE NOTICE 'Problem with union original shape in GID %', istring;
			END;
		END;
	END LOOP;
END;
$BODY$
  LANGUAGE 'plpgsql' VOLATILE
  COST 100;
ALTER FUNCTION remove_overlaps_1(character varying, character varying) OWNER TO postgres;
