
CREATE OR REPLACE FUNCTION remove_self_overlap_1by1(tablename varchar)
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
	'CREATE TEMPORARY TABLE _trim_self_gids as SELECT t1.gid as my_gid, t2.gid as other_gid from '||tablename||' t1, '||tablename||' t2 where t2.gid<> t1.gid and t2.the_geom && t1.the_geom;';
	CREATE INDEX _trim_self_gids_idx_1 ON _trim_self_gids using btree(my_gid);
	CREATE INDEX _trim_self_gids_idx_2 ON _trim_self_gids using btree(other_gid);

	-- these are the shapes we are dealing with
	gid_s:= 'SELECT gid from '||tablename ||' ORDER BY gid;';

	FOR i IN EXECUTE gid_s LOOP
				-- now remove intersections from others with larger gids.
		istring:= cast(i as text);
		RAISE NOTICE 'Processing GID %', istring;
		BEGIN
			gis2_s:= 'SELECT other_gid from _trim_gids where my_gid = '||istring||' and other_gid > my_gid;';
			FOR j in EXECUTE gis2_s LOOP
				jstring:=cast(j as text);
				BEGIN
					EXECUTE
					'UPDATE '||tablename||' t1 SET the_geom=ST_Difference(t1.the_geom, t2.the_geom) from (select gid, the_geom from '||tablename||' where gid ='||jstring||') as t2 where t1.gid = '||istring||';';

				EXCEPTION WHEN internal_error THEN
					RAISE NOTICE 'Problem with intersection % and %', istring, jstring;
				END;
			END LOOP;
		END;
	END LOOP;
        -- should drop if exists the _trim_self_gids table here
END;
$BODY$
  LANGUAGE 'plpgsql' VOLATILE
  COST 100;
ALTER FUNCTION remove_self_overlap_1by1(varchar) OWNER TO postgres;
