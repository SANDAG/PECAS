-- Function: remove_smaller_bits(character varying, character varying)

-- DROP FUNCTION remove_smaller_bits(character varying, character varying);

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
