CREATE OR REPLACE FUNCTION _cleangeometrycore(geometry)
  RETURNS geometry AS
$BODY$


DECLARE
  InGeom alias for $1;
  outGeom geometry;
  InArea double precision;
  outArea double precision;

Begin

InArea:=st_area(InGeom);

outGeom:=st_geomfromtext('POLYGON EMPTY',st_srid(InGeom));

if st_isvalid(InGeom)='t' then return InGeom;

else

begin

outGeom:= case when a1.geom is null
               then st_geomfromtext('POLYGON EMPTY',st_srid(InGeom))
               else a1.geom end from
          (select st_buildarea(st_union(st_snaptogrid(a.geom,0.000001))) geom 
          from
          (select (st_dump(st_union(
                    st_snaptogrid(st_exteriorring(InGeom),0.000001),
                    st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                   )).geom geom) a) a1;

exception when internal_error then

outGeom:= case when a1.geom is null
               then st_geomfromtext('POLYGON EMPTY',st_srid(InGeom))
               else a1.geom end from
          (select st_buildarea(st_union(st_snaptogrid(a.geom,0.000001))) geom 
          from
          (select (st_dump(st_union(
                    st_snaptogrid(st_exteriorring(InGeom),0.0001),
                    st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                   )).geom geom) a) a1;

end;

outArea:=st_area(outGeom);
if abs(InArea-outArea)<5 then return outGeom; else

begin

outGeom:= case when a2.geom is null
               then st_geomfromtext('POLYGON EMPTY',st_srid(InGeom))
               else a2.geom end from
          (select st_buildarea(st_union(st_snaptogrid(a1.geom,0.000001))) geom 
          from
          (select (st_dump(st_union(
                    st_snaptogrid(st_exteriorring(a.geom),0.000001),
                    st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                   )).geom geom from
           (select (st_dump(
                    st_buffer(st_buffer(InGeom,-0.0001),0.0001)
                    )).geom geom) a) a1) a2;

exception when internal_error then

outGeom:= case when a2.geom is null
               then st_geomfromtext('POLYGON EMPTY',st_srid(InGeom))
               else a2.geom end from
          (select st_buildarea(st_union(st_snaptogrid(a1.geom,0.000001))) geom 
          from
          (select (st_dump(st_union(
                    st_snaptogrid(st_exteriorring(a.geom),0.0001),
                    st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                   )).geom geom from
           (select (st_dump(
                    st_buffer(st_buffer(InGeom,-0.001),0.001)
                    )).geom geom) a) a1) a2;

end;

outArea:=st_area(outGeom);
if abs(InArea-outArea)<5 then return outGeom; else

begin

outGeom:= case when a1.geom is null
               then st_geomfromtext('POLYGON EMPTY',st_srid(InGeom))
               else a1.geom end from
          (select st_buildarea(st_union(st_snaptogrid(a.geom,0.000001))) geom 
          from
          (select (st_dump(st_union(
                    st_snaptogrid(st_exteriorring(InGeom),0.000001),
                    st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                   )).geom geom) a
           where (st_isclosed(a.geom)='t' and
                  st_area(st_buildarea(a.geom))>0.5) or 
                  st_isclosed(a.geom)='f') a1;

exception when internal_error then

outGeom:= case when a1.geom is null
               then st_geomfromtext('POLYGON EMPTY',st_srid(InGeom))
               else a1.geom end from
          (select st_buildarea(st_union(st_snaptogrid(a.geom,0.000001))) geom 
          from
          (select (st_dump(st_union(
                    st_snaptogrid(st_exteriorring(InGeom),0.0001),
                    st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                   )).geom geom) a
           where (st_isclosed(a.geom)='t' and
                  st_area(st_buildarea(a.geom))>0.5) or 
                  st_isclosed(a.geom)='f') a1;

end;

outArea:=st_area(outGeom);
if abs(InArea-outArea)<5 then return outGeom; else

begin

outGeom:=case when c.geom is null
              then st_geomfromtext('POLYGON EMPTY',st_srid(InGeom))
              else c.geom end geom from
         (select st_buildarea(st_union(st_snaptogrid(a.geom,0.000001))) geom          from
          (select (st_dump(st_union(
                   st_snaptogrid(st_exteriorring(InGeom),0.000001),
                   st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                   )).geom geom,
                  (st_dump(st_union(
                   st_snaptogrid(st_exteriorring(InGeom),0.000001),
                   st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                   )).path[1] path) a
           left join
          (select a1.path from
           (select a0.point, a0.path from
            (select st_startpoint(a01.geom) point, a01.path from
             (select (st_dump(st_union(
                      st_snaptogrid(st_exteriorring(InGeom),0.000001),
                      st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                      )).geom geom,
                     (st_dump(st_union(
                      st_snaptogrid(st_exteriorring(InGeom),0.000001),
                      st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                      )).path[1] path) a01
             union all
             select st_endpoint(a02.geom) point, a02.path from
             (select (st_dump(st_union(
                      st_snaptogrid(st_exteriorring(InGeom),0.000001),
                      st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                      )).geom geom,
                     (st_dump(st_union(
                      st_snaptogrid(st_exteriorring(InGeom),0.000001),
                      st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                      )).path[1] path) a02
             ) a0) a1,
          (select b1.point from
           (select b0.point, count(b0.point) count_p from
            (select st_startpoint(b01.geom) point from
             (select (st_dump(st_union(
                      st_snaptogrid(st_exteriorring(InGeom),0.000001),
                      st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                      )).geom geom) b01
             union all
             select st_endpoint(b02.geom) point from
             (select (st_dump(st_union(
                      st_snaptogrid(st_exteriorring(InGeom),0.000001),
                      st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                      )).geom geom) b02
             ) b0 group by b0.point) b1 where b1.count_p=1) b2
           where st_equals(a1.point, b2.point)='t'
           group by a1.path order by a1.path) a2
          on a.path=a2.path
          where a2.path is null) c;

exception when internal_error then

outGeom:=case when c.geom is null
              then st_geomfromtext('POLYGON EMPTY',st_srid(InGeom))
              else c.geom end geom from
         (select st_buildarea(st_union(st_snaptogrid(a.geom,0.000001))) geom from
          (select (st_dump(st_union(
                   st_snaptogrid(st_exteriorring(InGeom),0.0001),
                   st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                   )).geom geom,
                  (st_dump(st_union(
                   st_snaptogrid(st_exteriorring(InGeom),0.0001),
                   st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                   )).path[1] path) a
           inner join
          (select a1.path from
           (select a0.point, a0.path from
            (select st_startpoint(a01.geom) point, a01.path from
             (select (st_dump(st_union(
                      st_snaptogrid(st_exteriorring(InGeom),0.0001),
                      st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                      )).geom geom,
                     (st_dump(st_union(
                      st_snaptogrid(st_exteriorring(InGeom),0.0001),
                      st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                      )).path[1] path) a01
             union all
             select st_endpoint(a02.geom) point, a02.path from
             (select (st_dump(st_union(
                      st_snaptogrid(st_exteriorring(InGeom),0.0001),
                      st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                      )).geom geom,
                     (st_dump(st_union(
                      st_snaptogrid(st_exteriorring(InGeom),0.0001),
                      st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                      )).path[1] path) a02
             ) a0) a1,
          (select b1.point from
           (select b0.point, count(b0.point) count_p from
            (select st_startpoint(b01.geom) point from
             (select (st_dump(st_union(
                      st_snaptogrid(st_exteriorring(InGeom),0.0001),
                      st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                      )).geom geom) b01
             union all
             select st_endpoint(b02.geom) point from
             (select (st_dump(st_union(
                      st_snaptogrid(st_exteriorring(InGeom),0.0001),
                      st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                      )).geom geom) b02
             ) b0 group by b0.point) b1 where b1.count_p=1) b2
           where st_equals(a1.point, b2.point)='f'
           group by a1.path) a2
          on a.path=a2.path) c;

end;

end if;
end if;
end if;

outArea:=st_area(outGeom);
if abs(InArea-outArea)<5 then return outGeom; else

begin

outGeom:=case when c.geom is null
              then st_geomfromtext('POLYGON EMPTY',st_srid(InGeom))
              else c.geom end geom from
        (select st_buildarea(st_union(st_snaptogrid(a.geom,0.000001))) geom         from
         (select a1.geom, a1.path from
          (select (st_dump(st_union(
                   st_snaptogrid(st_exteriorring(InGeom),0.0000001),
                   st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                   )).geom geom,
                  (st_dump(st_union(
                   st_snaptogrid(st_exteriorring(InGeom),0.0000001),
                   st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                   )).path[1] path) a1
          where st_isclosed(a1.geom)='t' and
                st_area(st_buildarea(a1.geom))>0.5
          union
          select a3.geom, a3.path from (
          select a2.geom, a2.path from
          (select (st_dump(st_union(
                   st_snaptogrid(st_exteriorring(InGeom),0.0000001),
                   st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                   )).geom geom,
                  (st_dump(st_union(
                   st_snaptogrid(st_exteriorring(InGeom),0.0000001),
                   st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                   )).path[1] path) a2
           inner join
          (select (b12.ringpoints).path from
           (select row(b11.path,b11.startpoint,b11.endpoint)::ringpoints
                                                              ringpoints
            from
            (select (st_dump(st_union(st_snaptogrid(
                     st_exteriorring(InGeom),0.0000001),
                     st_geomfromtext('LINESTRING EMPTY',31467))
                     )).path[1] path,
                    (st_dump(st_union(st_snaptogrid(
                     st_exteriorring(InGeom),0.0000001),
                     st_geomfromtext('LINESTRING EMPTY',31467))
                     )).geom geom,
                    st_startpoint((st_dump(st_union(st_snaptogrid(
                     st_exteriorring(InGeom),0.0000001),
                     st_geomfromtext('LINESTRING EMPTY',31467))
                     )).geom) startpoint,
                    st_endpoint((st_dump(st_union(st_snaptogrid(
                     st_exteriorring(InGeom),0.0000001),
                     st_geomfromtext('LINESTRING EMPTY',31467))
                     )).geom) endpoint) b11
            where st_isclosed(b11.geom)='f') b12
            left join
            (select row(b21.path,b21.startpoint,b21.endpoint)::ringpoints
                                                               ringpoints
             from
             (select (st_dump(st_union(st_snaptogrid(
                      st_exteriorring(InGeom),0.0000001),
                      st_geomfromtext('LINESTRING EMPTY',31467))
                      )).path[1] path,
                     (st_dump(st_union(st_snaptogrid(
                      st_exteriorring(InGeom),0.0000001),
                      st_geomfromtext('LINESTRING EMPTY',31467))
                      )).geom geom,
                     st_startpoint((st_dump(st_union(st_snaptogrid(
                      st_exteriorring(InGeom),0.0000001),
                      st_geomfromtext('LINESTRING EMPTY',31467))
                      )).geom) startpoint,
                     st_endpoint((st_dump(st_union(st_snaptogrid(
                      st_exteriorring(InGeom),0.0000001),
                      st_geomfromtext('LINESTRING EMPTY',31467))
                      )).geom) endpoint) b21
              where st_isclosed(b21.geom)='f') b22
            on (b12.ringpoints).path=((b22.ringpoints).path-1)
            where st_equals((b12.ringpoints).endpoint,
                            (b22.ringpoints).startpoint)
           union
           select (b32.ringpoints).path from
           (select row(b31.path,b31.startpoint,b31.endpoint)::ringpoints
                                                              ringpoints
            from
            (select (st_dump(st_union(st_snaptogrid(
                     st_exteriorring(InGeom),0.0000001),
                     st_geomfromtext('LINESTRING EMPTY',31467))
                     )).path[1] path,
                    st_startpoint((st_dump(st_union(st_snaptogrid(
                     st_exteriorring(InGeom),0.0000001),
                     st_geomfromtext('LINESTRING EMPTY',31467))
                     )).geom) startpoint,
                    st_endpoint((st_dump(st_union(st_snaptogrid(
                     st_exteriorring(InGeom),0.0000001),
                     st_geomfromtext('LINESTRING EMPTY',31467))
                     )).geom) endpoint) b31
            order by b31.path desc limit 1) b32,
           (select row(b41.path,b41.startpoint,b41.endpoint)::ringpoints
                                                              ringpoints
            from
            (select (st_dump(st_union(st_snaptogrid(
                     st_exteriorring(InGeom),0.0000001),
                     st_geomfromtext('LINESTRING EMPTY',31467))
                     )).path[1] path,
                    st_startpoint((st_dump(st_union(st_snaptogrid(
                     st_exteriorring(InGeom),0.0000001),
                     st_geomfromtext('LINESTRING EMPTY',31467))
                     )).geom) startpoint,
                    st_endpoint((st_dump(st_union(st_snaptogrid(
                     st_exteriorring(InGeom),0.0000001),
                     st_geomfromtext('LINESTRING EMPTY',31467))
                     )).geom) endpoint) b41
            order by b41.path asc limit 1) b42
           where st_equals((b32.ringpoints).endpoint,
                           (b42.ringpoints).startpoint)
          ) b3
          on a2.path=b3.path
          order by a2.path) a3) a) c;

 exception when internal_error then

outGeom:=case when c.geom is null
              then st_geomfromtext('POLYGON EMPTY',st_srid(InGeom))
              else c.geom end geom from
        (select st_buildarea(st_union(st_snaptogrid(a.geom,0.00001))) geom         from
         (select a1.geom, a1.path from
          (select (st_dump(st_union(
                   st_snaptogrid(st_exteriorring(InGeom),0.0001),
                   st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                   )).geom geom,
                  (st_dump(st_union(
                   st_snaptogrid(st_exteriorring(InGeom),0.0001),
                   st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                   )).path[1] path) a1
          where st_isclosed(a1.geom)='t' and
                st_area(st_buildarea(a1.geom))>0.5
          union
          select a2.geom, a2.path from
          (select (st_dump(st_union(
                   st_snaptogrid(st_exteriorring(InGeom),0.0001),
                   st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                   )).geom geom,
                  (st_dump(st_union(
                   st_snaptogrid(st_exteriorring(InGeom),0.0001),
                   st_geomfromtext('LINESTRING EMPTY',st_srid(InGeom)))
                   )).path[1] path) a2
           inner join
          (select (b12.ringpoints).path from
           (select row(b11.path,b11.startpoint,b11.endpoint)::ringpoints
                                                              ringpoints
            from
            (select (st_dump(st_union(st_snaptogrid(
                     st_exteriorring(InGeom),0.0001),
                     st_geomfromtext('LINESTRING EMPTY',31467))
                     )).path[1] path,
                    (st_dump(st_union(st_snaptogrid(
                     st_exteriorring(InGeom),0.0001),
                     st_geomfromtext('LINESTRING EMPTY',31467))
                     )).geom geom,
                    st_startpoint((st_dump(st_union(st_snaptogrid(
                     st_exteriorring(InGeom),0.0001),
                     st_geomfromtext('LINESTRING EMPTY',31467))
                     )).geom) startpoint,
                    st_endpoint((st_dump(st_union(st_snaptogrid(
                     st_exteriorring(InGeom),0.0001),
                     st_geomfromtext('LINESTRING EMPTY',31467))
                     )).geom) endpoint) b11
            where st_isclosed(b11.geom)='f') b12
            left join
            (select row(b21.path,b21.startpoint,b21.endpoint)::ringpoints
                                                               ringpoints
             from
             (select (st_dump(st_union(st_snaptogrid(
                      st_exteriorring(InGeom),0.0001),
                      st_geomfromtext('LINESTRING EMPTY',31467))
                      )).path[1] path,
                     (st_dump(st_union(st_snaptogrid(
                      st_exteriorring(InGeom),0.0001),
                      st_geomfromtext('LINESTRING EMPTY',31467))
                      )).geom geom,
                     st_startpoint((st_dump(st_union(st_snaptogrid(
                      st_exteriorring(InGeom),0.0001),
                      st_geomfromtext('LINESTRING EMPTY',31467))
                      )).geom) startpoint,
                     st_endpoint((st_dump(st_union(st_snaptogrid(
                      st_exteriorring(InGeom),0.0001),
                      st_geomfromtext('LINESTRING EMPTY',31467))
                      )).geom) endpoint) b21
              where st_isclosed(b21.geom)='f') b22
            on (b12.ringpoints).path=((b22.ringpoints).path-1)
            where st_equals((b12.ringpoints).endpoint,
                            (b22.ringpoints).startpoint)
           union
           select (b32.ringpoints).path from
           (select row(b31.path,b31.startpoint,b31.endpoint)::ringpoints
                                                              ringpoints
            from
            (select (st_dump(st_union(st_snaptogrid(
                     st_exteriorring(InGeom),0.0001),
                     st_geomfromtext('LINESTRING EMPTY',31467))
                     )).path[1] path,
                    st_startpoint((st_dump(st_union(st_snaptogrid(
                     st_exteriorring(InGeom),0.0001),
                     st_geomfromtext('LINESTRING EMPTY',31467))
                     )).geom) startpoint,
                    st_endpoint((st_dump(st_union(st_snaptogrid(
                     st_exteriorring(InGeom),0.0001),
                     st_geomfromtext('LINESTRING EMPTY',31467))
                     )).geom) endpoint) b31
            order by b31.path desc limit 1) b32,
           (select row(b41.path,b41.startpoint,b41.endpoint)::ringpoints
                                                              ringpoints
            from
            (select (st_dump(st_union(st_snaptogrid(
                     st_exteriorring(InGeom),0.0001),
                     st_geomfromtext('LINESTRING EMPTY',31467))
                     )).path[1] path,
                    st_startpoint((st_dump(st_union(st_snaptogrid(
                     st_exteriorring(InGeom),0.0001),
                     st_geomfromtext('LINESTRING EMPTY',31467))
                     )).geom) startpoint,
                    st_endpoint((st_dump(st_union(st_snaptogrid(
                     st_exteriorring(InGeom),0.0001),
                     st_geomfromtext('LINESTRING EMPTY',31467))
                     )).geom) endpoint) b41
            order by b41.path asc limit 1) b42
           where st_equals((b32.ringpoints).endpoint,
                           (b42.ringpoints).startpoint)
          ) b3
          on a2.path=b3.path
          order by a2.path) a) c;

end;

end if;

outArea:=st_area(outGeom);
if abs(InArea-outArea)<5 then return outGeom; else

raise exception '_cleangeometrycore war nicht erfolgreich. ' ;

end if;

end if;

return outGeom;

End;
$BODY$
  LANGUAGE 'plpgsql' VOLATILE
  COST 100;
ALTER FUNCTION _cleangeometrycore(geometry) OWNER TO postgres;


CREATE OR REPLACE FUNCTION _cleangeometry4(geometry)
  RETURNS geometry AS
$BODY$

/*
Input: Any given geometry
Wanted Output: valid polygon or multipolygon
*/

DECLARE
  InGeom alias for $1;
  workGeom geometry;
  outGeom geometry;
  exteriorring geometry;
  interiorrings geometry;
  i integer;

Begin

outGeom:=st_geomfromtext('POLYGON EMPTY',st_srid(InGeom));

if st_isvalid(InGeom)='t' then return InGeom; end if;

if InGeom is null then return st_geomfromtext('POLYGON EMPTY',st_srid(InGeom));
end if;

if st_geometrytype(InGeom) in ('ST_Polygon','ST_MultiPolygon') then 

 workGeom:=InGeom;

else 
 if st_geometrytype(InGeom)='ST_GeometryCollection' and
    st_isempty(InGeom)='f' and
    st_area(InGeom)>0
 then
  workGeom:=st_collectionextract(InGeom,3); --what about if there are Multipolygons as part of the GeometryCollection as well?
 else return outGeom;

 end if;

end if;

--POLYGONS:

if st_geometrytype(workGeom)='ST_Polygon' then

if st_isvalid(workGeom)='t' then return workGeom; end if;

exteriorring:=a.geom from 
              (select (st_dumprings(workGeom)).geom geom,
                      (st_dumprings(workGeom)).path[1] path) a
               where a.path=0;

interiorrings:=st_collect(a.geom) from
              (select (st_dumprings(workGeom)).geom geom,
                      (st_dumprings(workGeom)).path[1] path) a
               where a.path>0 and st_area(a.geom)>0.5;

if interiorrings is null then interiorrings:=st_geomfromtext('POLYGON EMPTY',st_srid(InGeom)); end if;

--Cleaning exteriorring:

i:=1;
while st_isvalid(exteriorring)='f' and i<=10 loop

begin

exteriorring:=case when st_union(a1.geom) is null 
                   then st_collect(a1.geom)
                   else st_union(a1.geom) end from 
              (select (st_dump(_cleangeometrycore(a.geom))).geom geom from
              (select (st_dump(exteriorring)).geom geom) a
               where st_area(a.geom)>0.5) a1;

exception when internal_error then

exteriorring:=st_collect(a1.geom) from 
              (select (st_dump(_cleangeometrycore(a.geom))).geom geom from
              (select (st_dump(exteriorring)).geom geom) a
               where st_area(a.geom)>0.5) a1;

end;

i:=i+1;

end loop;

if exteriorring is null then exteriorring:=st_geomfromtext('POLYGON EMPTY',st_srid(InGeom)); end if;

--Cleaning interiorrings:

i:=1;
while st_isvalid(interiorrings)='f' and i<=10 loop

begin

interiorrings:=case when st_union(a2.geom) is null 
                    then st_collect(a2.geom)
                    else st_union(a2.geom) end from 
              (select a1.geom from
              (select (st_dump(_cleangeometrycore(a.geom))).geom geom from
              (select (st_dump(interiorrings)).geom geom) a) a1
               where st_area(a1.geom)>0.5) a2;

exception when internal_error then

interiorrings:=st_collect(a2.geom) from 
              (select a1.geom from
              (select (st_dump(_cleangeometrycore(a.geom))).geom geom from
              (select (st_dump(interiorrings)).geom geom) a) a1
               where st_area(a1.geom)>0.5) a2;

end;

i:=i+1;

end loop;

--Putting together exteriorring and interiorrings:

if st_isempty(interiorrings)='t' then 

 if st_geometrytype(exteriorring)='ST_MultiPolygon' then
  workGeom:=exteriorring;
 else return exteriorring;
 end if;

else

begin

outGeom:=st_difference(exteriorring,interiorrings)
         where st_intersects(exteriorring,interiorrings) and
               st_area(st_intersection(exteriorring,interiorrings))>0;

exception when internal_error then

exteriorring:=st_snaptogrid(exteriorring,0.0001);

interiorrings:=st_snaptogrid(interiorrings,0.0001);

outGeom:=st_difference(exteriorring,interiorrings)
         where st_intersects(exteriorring,interiorrings) and
               st_area(st_intersection(exteriorring,interiorrings))>0;

end;

 if st_geometrytype(outGeom)='ST_MultiPolygon' then
  workGeom:=outGeom;
 else return outGeom;
 end if;

end if;

if st_isempty(exteriorring)='f' and st_isempty(interiorrings)='f' and 
   st_isvalid(exteriorring)='t' and st_isvalid(interiorrings)='t' then

   outGeom:=st_difference(exteriorring,interiorrings)
            where st_intersects(exteriorring,interiorrings) and
                  st_area(st_intersection(exteriorring,interiorrings))>0;

   if st_area(outGeom)<0.5 then outGeom:=st_geomfromtext('POLYGON EMPTY',
                                         st_srid(InGeom)); end if;

   return outGeom;

end if;

end if;

--MULTIPOLYGONS:

if st_geometrytype(workGeom)='ST_MultiPolygon' then

outGeom:=case when st_union(sel.geom) is null
              then st_collect(sel.geom)
              else st_union(sel.geom) end from
         (select sel1.geom geom from
          (select (st_dump(workGeom)).geom geom) sel1
          where st_area(sel1.geom)>0.5) sel;

 if outGeom is null then outGeom:=st_geomfromtext('POLYGON EMPTY',st_srid(InGeom)); end if;

if st_geometrytype(workGeom)='ST_MultiPolygon' then
workGeom:=outGeom; 

begin

exteriorring:=st_collect(a1.geom) from
              (select (st_dumprings((st_dump(a.geom)).geom)).geom geom,
               (st_dumprings((st_dump(a.geom)).geom)).path[1] path from
               (select st_collectionextract(st_union(workGeom,
                        st_geomfromtext('POLYGON EMPTY',st_srid(InGeom))),3) 
                       geom) a ) a1
               where a1.path=0 and st_area(a1.geom)>0.5; 

exception when internal_error then

begin

exteriorring:=st_collect(a1.geom) from
              (select (st_dumprings((st_dump(a.geom)).geom)).geom geom,
               (st_dumprings((st_dump(a.geom)).geom)).path[1] path from
               (select st_collectionextract(st_union(
                        st_snaptogrid(workGeom,0.0001),
                        st_geomfromtext('POLYGON EMPTY',st_srid(InGeom))),3) 
                       geom) a ) a1
               where a1.path=0 and st_area(a1.geom)>0.5; 

exception when internal_error then

begin

exteriorring:=st_collect(a1.geom) from
              (select (st_dumprings((st_dump(a.geom)).geom)).geom geom,
               (st_dumprings((st_dump(a.geom)).geom)).path[1] path from
               (select st_collectionextract(st_union(
                        st_snaptogrid(workGeom,0.01),
                        st_geomfromtext('POLYGON EMPTY',st_srid(InGeom))),3) 
                       geom) a ) a1
               where a1.path=0 and st_area(a1.geom)>0.5; 

exception when internal_error then

exteriorring:=st_collect(a1.geom) from
              (select (st_dumprings((st_dump(a.geom)).geom)).geom geom,
               (st_dumprings((st_dump(a.geom)).geom)).path[1] path from
               (select st_buffer(st_collectionextract(st_union(
                        st_buffer(st_snaptogrid(workGeom,0.0001),0.001),
                        st_geomfromtext('POLYGON EMPTY',st_srid(InGeom))),3),
                        -0.001) 
                       geom) a ) a1
               where a1.path=0 and st_area(a1.geom)>0.5; 

end;
end;
end;

begin

interiorrings:=st_collect(a1.geom) from
              (select (st_dumprings((st_dump(a.geom)).geom)).geom geom,
               (st_dumprings((st_dump(a.geom)).geom)).path[1] path from
               (select st_collectionextract(st_union(workGeom,
                        st_geomfromtext('POLYGON EMPTY',st_srid(InGeom))),3) 
                       geom) a ) a1
               where a1.path>0 and st_area(a1.geom)>0.5; 

exception when internal_error then

begin

interiorrings:=st_collect(a1.geom) from
              (select (st_dumprings((st_dump(a.geom)).geom)).geom geom,
               (st_dumprings((st_dump(a.geom)).geom)).path[1] path from
               (select st_collectionextract(st_union(
                        st_snaptogrid(workGeom,0.0001),
                        st_geomfromtext('POLYGON EMPTY',st_srid(InGeom))),3) 
                       geom) a ) a1
               where a1.path>0 and st_area(a1.geom)>0.5; 

exception when internal_error then

begin

interiorrings:=st_collect(a1.geom) from
              (select (st_dumprings((st_dump(a.geom)).geom)).geom geom,
               (st_dumprings((st_dump(a.geom)).geom)).path[1] path from
               (select st_collectionextract(st_union(
                        st_snaptogrid(workGeom,0.01),
                        st_geomfromtext('POLYGON EMPTY',st_srid(InGeom))),3) 
                       geom) a ) a1
               where a1.path>0 and st_area(a1.geom)>0.5; 

exception when internal_error then

interiorrings:=st_collect(a1.geom) from
              (select (st_dumprings((st_dump(a.geom)).geom)).geom geom,
               (st_dumprings((st_dump(a.geom)).geom)).path[1] path from
               (select st_buffer(st_collectionextract(st_union(
                        st_buffer(st_snaptogrid(workGeom,0.0001),0.001),
                        st_geomfromtext('POLYGON EMPTY',st_srid(InGeom))),3),
                        -0.001) 
                       geom) a ) a1
               where a1.path>0 and st_area(a1.geom)>0.5; 

end;
end;
end;

if interiorrings is null then interiorrings:=st_geomfromtext('POLYGON EMPTY',st_srid(InGeom)); end if;


--Cleaning exteriorrings:

i:=1;
while st_isvalid(exteriorring)='f' and i<=10 loop

begin

exteriorring:=case when st_union(a1.geom) is null 
                   then st_collect(a1.geom)
                   else st_union(a1.geom) end from 
              (select (st_dump(_cleangeometrycore(a.geom))).geom geom from
              (select (st_dump(exteriorring)).geom geom) a
               where st_area(a.geom)>0.5) a1;

exception when internal_error then

exteriorring:=st_collect(a1.geom) from 
              (select (st_dump(_cleangeometrycore(a.geom))).geom geom from
              (select (st_dump(exteriorring)).geom geom) a
               where st_area(a.geom)>0.5) a1;

end;

i:=i+1;

end loop;

if exteriorring is null then exteriorring:=st_geomfromtext('POLYGON EMPTY',st_srid(InGeom)); end if;

--Cleaning interiorrings:

i:=1;
while st_isvalid(interiorrings)='f' and i<=10 loop

begin

interiorrings:=case when st_union(a2.geom) is null 
                    then st_collect(a2.geom)
                    else st_union(a2.geom) end from 
              (select a1.geom from
              (select (st_dump(_cleangeometrycore(a.geom))).geom geom from
              (select (st_dump(interiorrings)).geom geom) a) a1
               where st_area(a1.geom)>0.5) a2;

exception when internal_error then

interiorrings:=st_collect(a2.geom) from 
              (select a1.geom from
              (select (st_dump(_cleangeometrycore(a.geom))).geom geom from
              (select (st_dump(interiorrings)).geom geom) a) a1
               where st_area(a1.geom)>0.5) a2;

end;

i:=i+1;

end loop;

if interiorrings is null then interiorrings:=st_geomfromtext('POLYGON EMPTY',st_srid(InGeom)); end if;

--Putting together exteriorring and interiorrings:

if st_isempty(interiorrings)='t' then 

 return exteriorring;

else

begin

outGeom:=st_difference(exteriorring,interiorrings)
         where st_intersects(exteriorring,interiorrings) and
               st_area(st_intersection(exteriorring,interiorrings))>0;

if outGeom is null then raise internal_error; end if;

exception when internal_error then

exteriorring:=st_snaptogrid(exteriorring,0.0001);

interiorrings:=st_snaptogrid(interiorrings,0.0001);

outGeom:=st_difference(exteriorring,interiorrings)
         where st_intersects(exteriorring,interiorrings) and
               st_area(st_intersection(exteriorring,interiorrings))>0;

if outGeom is null then outGeom:=exteriorring; end if;

end;

end if;
end if;

return outGeom;

end if;

return InGeom;

End;
$BODY$
  LANGUAGE 'plpgsql' VOLATILE
  COST 100;
ALTER FUNCTION _cleangeometry4(geometry) OWNER TO postgres;
