--
-- PostgreSQL database dump
--

-- Started on 2010-02-24 16:49:04

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

--
-- TOC entry 368 (class 2612 OID 16386)
-- Name: plpgsql; Type: PROCEDURAL LANGUAGE; Schema: -; Owner: postgres
--

CREATE PROCEDURAL LANGUAGE plpgsql;


ALTER PROCEDURAL LANGUAGE plpgsql OWNER TO postgres;

SET search_path = public, pg_catalog;

--
-- TOC entry 21 (class 1255 OID 2373227)
-- Dependencies: 3 368
-- Name: clean_up_tables(); Type: FUNCTION; Schema: public; Owner: usrPostgres
--

CREATE OR REPLACE FUNCTION output.clean_up_tables()
  RETURNS void AS
$$
	DECLARE 
		rec RECORD ;
	BEGIN
		FOR rec IN SELECT * FROM aa_output_files LOOP			
			RAISE NOTICE 'Table % truncated', rec.all_table_name ; 
			EXECUTE 'TRUNCATE TABLE ' || rec.all_table_name;
			EXECUTE 'TRUNCATE TABLE ' || rec.temp_table_name;
		END LOOP;
		--RETURN 0;
	END;
$$


ALTER FUNCTION public.clean_up_tables() OWNER TO "usrPostgres";

--
-- TOC entry 20 (class 1255 OID 2372932)
-- Dependencies: 3 368
-- Name: load_aa_output(text, integer, text); Type: FUNCTION; Schema: public; Owner: usrPostgres
--

CREATE OR REPLACE FUNCTION output.load_aa_output(root_folder_path text, run_year integer, scenario_name text)
  RETURNS text AS
$$
	DECLARE 
		v_path text;
		file_path text;
		rec RECORD;
		output text := '' ;
		result text;
	BEGIN
		v_path := root_folder_path || scenario_name || '/' || run_year || '/' ;
		
		FOR rec IN SELECT * FROM aa_output_files LOOP
			file_path := v_path || rec.csv_file_name;
			SELECT load_table(rec.all_table_name, rec.temp_table_name, file_path, run_year, scenario_name) into result;
			output := output || result || E';\n' ; 		
		END LOOP;

		return output;
	END;
$$

ALTER FUNCTION public.load_aa_output(root_folder_path text, run_year integer, scenario_name text) OWNER TO "usrPostgres";

--
-- TOC entry 19 (class 1255 OID 2372540)
-- Dependencies: 3 368
-- Name: load_table(text, text, text, integer, text); Type: FUNCTION; Schema: public; Owner: usrPostgres
--

CREATE OR REPLACE FUNCTION output.load_table(table_name text, temp_table text, file_name_with_path text, run_year integer, scenario_name text)
  RETURNS text AS
$$ 
	DECLARE
		cBefore integer;
		cAfter  integer;
        BEGIN

	EXECUTE 'TRUNCATE ' || temp_table;

	EXECUTE 'COPY '|| temp_table || ' FROM ' || quote_literal(file_name_with_path) || ' CSV HEADER';
	
        EXECUTE 'SELECT count(*) FROM '|| table_name INTO  cBefore  ;       

        EXECUTE 'INSERT INTO ' || table_name || 
		' (select '||run_year||' as Year, '|| quote_literal(scenario_Name) ||' as Scenario, tbl.* from ' || temp_table || ' tbl );';

	 EXECUTE 'SELECT count(*) FROM '|| table_name INTO  cAfter  ;       
--
               
               return cAfter- cBefore  || ' records added to ' || table_name;
        END;

$$


ALTER FUNCTION public.load_table(table_name text, temp_table text, file_name_with_path text, run_year integer, scenario_name text) OWNER TO "usrPostgres";

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 1576 (class 1259 OID 2372552)
-- Dependencies: 3
-- Name: aa_output_files; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE aa_output_files (
    file_id integer NOT NULL,
    csv_file_name character varying,
    all_table_name character varying,
    temp_table_name character varying
);


ALTER TABLE public.aa_output_files OWNER TO "usrPostgres";

--
-- TOC entry 1575 (class 1259 OID 2372550)
-- Dependencies: 3 1576
-- Name: aa_output_files_file_id_seq; Type: SEQUENCE; Schema: public; Owner: usrPostgres
--

CREATE SEQUENCE aa_output_files_file_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.aa_output_files_file_id_seq OWNER TO "usrPostgres";

--
-- TOC entry 1915 (class 0 OID 0)
-- Dependencies: 1575
-- Name: aa_output_files_file_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: usrPostgres
--

ALTER SEQUENCE aa_output_files_file_id_seq OWNED BY aa_output_files.file_id;


--
-- TOC entry 1916 (class 0 OID 0)
-- Dependencies: 1575
-- Name: aa_output_files_file_id_seq; Type: SEQUENCE SET; Schema: public; Owner: usrPostgres
--

SELECT pg_catalog.setval('aa_output_files_file_id_seq', 9, true);


--
-- TOC entry 1555 (class 1259 OID 2372291)
-- Dependencies: 3
-- Name: activity_locations_temp; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE activity_locations_temp (
    activity character varying,
    zonenumber integer,
    quantity double precision,
    technologylogsum double precision,
    sizeutility double precision,
    zoneconstant double precision,
    constrained boolean,
    constraintvalue double precision,
    locationutility double precision,
    size double precision
);


ALTER TABLE public.activity_locations_temp OWNER TO "usrPostgres";

--
-- TOC entry 1557 (class 1259 OID 2372302)
-- Dependencies: 3
-- Name: activity_numbers; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE activity_numbers (
    activitynumber integer NOT NULL,
    activity character varying
);


ALTER TABLE public.activity_numbers OWNER TO "usrPostgres";

--
-- TOC entry 1559 (class 1259 OID 2372318)
-- Dependencies: 3
-- Name: activity_summary_temp; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE activity_summary_temp (
    activity character varying NOT NULL,
    compositeutility double precision,
    size double precision
);


ALTER TABLE public.activity_summary_temp OWNER TO "usrPostgres";

--
-- TOC entry 1556 (class 1259 OID 2372297)
-- Dependencies: 3
-- Name: all_activity_locations; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE all_activity_locations (
    year_run integer NOT NULL,
    scenario character(10) NOT NULL,
    activity character varying NOT NULL,
    zonenumber integer NOT NULL,
    quantity double precision,
    technologylogsum double precision,
    sizeutility double precision,
    zoneconstant double precision,
    constrained boolean,
    constraintvalue double precision,
    locationutility double precision,
    size double precision
);


ALTER TABLE public.all_activity_locations OWNER TO "usrPostgres";

--
-- TOC entry 1558 (class 1259 OID 2372310)
-- Dependencies: 3
-- Name: all_activity_summary; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE all_activity_summary (
    year_run integer NOT NULL,
    scenario character(10) NOT NULL,
    activity character varying NOT NULL,
    compositeutility double precision,
    size double precision
);


ALTER TABLE public.all_activity_summary OWNER TO "usrPostgres";

--
-- TOC entry 1562 (class 1259 OID 2372340)
-- Dependencies: 3
-- Name: all_commodity_zutilities; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE all_commodity_zutilities (
    year_run integer NOT NULL,
    scenario character(10) NOT NULL,
    commodity character varying NOT NULL,
    zone integer NOT NULL,
    buyingorselling character varying NOT NULL,
    quantity double precision,
    zutility double precision,
    variationcomponent double precision,
    pricecomponent double precision,
    sizecomponent double precision,
    transportcomponent1 double precision,
    transportcomponent2 double precision,
    transportcomponent3 double precision,
    transportcomponent4 double precision
);


ALTER TABLE public.all_commodity_zutilities OWNER TO "usrPostgres";

--
-- TOC entry 1563 (class 1259 OID 2372348)
-- Dependencies: 3
-- Name: all_exchange_results; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE all_exchange_results (
    year_run integer NOT NULL,
    scenario character(10) NOT NULL,
    commodity character varying NOT NULL,
    zonenumber integer NOT NULL,
    demand double precision,
    internalbought double precision,
    exports double precision,
    supply double precision,
    internalsold double precision,
    imports double precision,
    surplus double precision,
    price double precision,
    buyingsizeterm double precision,
    sellingsizeterm double precision,
    derivative double precision
);


ALTER TABLE public.all_exchange_results OWNER TO "usrPostgres";

--
-- TOC entry 1566 (class 1259 OID 2372364)
-- Dependencies: 3
-- Name: all_exchange_results_totals; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE all_exchange_results_totals (
    year_run integer NOT NULL,
    scenario character(10) NOT NULL,
    commodity character varying NOT NULL,
    demand double precision,
    internalbought double precision,
    exports double precision,
    supply double precision,
    internalsold double precision,
    imports double precision,
    rmssurplus double precision,
    averageprice double precision
);


ALTER TABLE public.all_exchange_results_totals OWNER TO "usrPostgres";

--
-- TOC entry 1568 (class 1259 OID 2372377)
-- Dependencies: 3
-- Name: all_histogram; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE all_histogram (
    year_run integer NOT NULL,
    scenario character(10) NOT NULL,
    commodity character varying NOT NULL,
    buyingselling character varying NOT NULL,
    bandnumber integer NOT NULL,
    lowerbound double precision,
    quantity double precision,
    averagelength double precision
);


ALTER TABLE public.all_histogram OWNER TO "usrPostgres";

--
-- TOC entry 1570 (class 1259 OID 2372391)
-- Dependencies: 3
-- Name: all_latest_activity_constants; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE all_latest_activity_constants (
    year_run integer NOT NULL,
    scenario character(10) NOT NULL,
    activity character(50) NOT NULL,
    zonenumber integer NOT NULL,
    quantity double precision,
    technologylogsum double precision,
    sizeutility double precision,
    zoneconstant double precision,
    constrained boolean,
    constraintvalue double precision,
    locationutility double precision,
    size double precision
);


ALTER TABLE public.all_latest_activity_constants OWNER TO "usrPostgres";

--
-- TOC entry 1571 (class 1259 OID 2372396)
-- Dependencies: 3
-- Name: all_makeuse; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE all_makeuse (
    year_run integer NOT NULL,
    scenario character(10) NOT NULL,
    activity character varying NOT NULL,
    commodity character varying NOT NULL,
    moru character varying NOT NULL,
    coefficient double precision,
    stddev double precision,
    amount double precision
);


ALTER TABLE public.all_makeuse OWNER TO "usrPostgres";

--
-- TOC entry 1574 (class 1259 OID 2372445)
-- Dependencies: 3
-- Name: all_zonalmakeuse; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE all_zonalmakeuse (
    year_run integer NOT NULL,
    scenario character(10) NOT NULL,
    activity integer NOT NULL,
    zonenumber integer NOT NULL,
    commodity integer NOT NULL,
    moru character varying NOT NULL,
    coefficient double precision,
    utility double precision,
    amount double precision
);


ALTER TABLE public.all_zonalmakeuse OWNER TO "usrPostgres";

--
-- TOC entry 1560 (class 1259 OID 2372326)
-- Dependencies: 3
-- Name: commodity_numbers; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE commodity_numbers (
    commoditynumber integer NOT NULL,
    commodity character varying
);


ALTER TABLE public.commodity_numbers OWNER TO "usrPostgres";

--
-- TOC entry 1561 (class 1259 OID 2372334)
-- Dependencies: 3
-- Name: commodity_zutilities_temp; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE commodity_zutilities_temp (
    commodity character varying,
    zone integer,
    buyingorselling character varying,
    quantity double precision,
    zutility double precision,
    variationcomponent double precision,
    pricecomponent double precision,
    sizecomponent double precision,
    transportcomponent1 double precision,
    transportcomponent2 double precision,
    transportcomponent3 double precision,
    transportcomponent4 double precision
);


ALTER TABLE public.commodity_zutilities_temp OWNER TO "usrPostgres";

--
-- TOC entry 1564 (class 1259 OID 2372353)
-- Dependencies: 3
-- Name: exchange_results_temp; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE exchange_results_temp (
    commodity character varying NOT NULL,
    zonenumber integer NOT NULL,
    demand double precision,
    internalbought double precision,
    exports double precision,
    supply double precision,
    internalsold double precision,
    imports double precision,
    surplus double precision,
    price double precision,
    buyingsizeterm double precision,
    sellingsizeterm double precision,
    derivative double precision
);


ALTER TABLE public.exchange_results_temp OWNER TO "usrPostgres";

--
-- TOC entry 1565 (class 1259 OID 2372359)
-- Dependencies: 3
-- Name: exchange_results_totals_temp; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE exchange_results_totals_temp (
    commodity character varying NOT NULL,
    demand double precision,
    internalbought double precision,
    exports double precision,
    supply double precision,
    internalsold double precision,
    imports double precision,
    rmssurplus double precision,
    averageprice double precision
);


ALTER TABLE public.exchange_results_totals_temp OWNER TO "usrPostgres";

--
-- TOC entry 1567 (class 1259 OID 2372369)
-- Dependencies: 3
-- Name: histogram_temp; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE histogram_temp (
    commodity character varying NOT NULL,
    buyingselling character varying,
    bandnumber integer,
    lowerbound double precision,
    quantity double precision,
    averagelength double precision
);


ALTER TABLE public.histogram_temp OWNER TO "usrPostgres";

--
-- TOC entry 1569 (class 1259 OID 2372385)
-- Dependencies: 3
-- Name: latest_activity_constants_temp; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE latest_activity_constants_temp (
    activity character varying,
    zonenumber integer,
    quantity double precision,
    technologylogsum double precision,
    sizeutility double precision,
    zoneconstant double precision,
    constrained boolean,
    constraintvalue double precision,
    locationutility double precision,
    size double precision
);


ALTER TABLE public.latest_activity_constants_temp OWNER TO "usrPostgres";

--
-- TOC entry 1572 (class 1259 OID 2372404)
-- Dependencies: 3
-- Name: makeuse_temp; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE makeuse_temp (
    activity character varying NOT NULL,
    commodity character varying NOT NULL,
    moru character varying NOT NULL,
    coefficient double precision,
    stddev double precision,
    amount double precision
);


ALTER TABLE public.makeuse_temp OWNER TO "usrPostgres";

--
-- TOC entry 1573 (class 1259 OID 2372437)
-- Dependencies: 3
-- Name: zonalmakeuse_temp; Type: TABLE; Schema: public; Owner: usrPostgres; Tablespace: 
--

CREATE TABLE zonalmakeuse_temp (
    activity integer NOT NULL,
    zonenumber integer NOT NULL,
    commodity integer NOT NULL,
    moru character varying NOT NULL,
    coefficient double precision,
    utility double precision,
    amount double precision
);


ALTER TABLE public.zonalmakeuse_temp OWNER TO "usrPostgres";

--
-- TOC entry 1854 (class 2604 OID 2372555)
-- Dependencies: 1575 1576 1576
-- Name: file_id; Type: DEFAULT; Schema: public; Owner: usrPostgres
--

ALTER TABLE aa_output_files ALTER COLUMN file_id SET DEFAULT nextval('aa_output_files_file_id_seq'::regclass);


--
-- TOC entry 1909 (class 0 OID 2372552)
-- Dependencies: 1576
-- Data for Name: aa_output_files; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY aa_output_files (file_id, csv_file_name, all_table_name, temp_table_name) FROM stdin;
1	ActivitySummary.csv	all_activity_summary	activity_summary_temp
2	ActivityLocations.csv	all_activity_locations	activity_locations_temp
3	ExchangeResults.csv	all_exchange_results	exchange_results_temp
4	ExchangeResultsTotals.csv	all_exchange_results_totals	exchange_results_totals_temp
5	Histograms.csv	all_histogram	histogram_temp
6	LatestActivityConstants.csv	all_latest_activity_constants	latest_activity_constants_temp
7	MakeUse.csv	all_makeuse	makeuse_temp
8	CommodityZUtilities.csv	all_commodity_zutilities	commodity_zutilities_temp
9	ZonalMakeUse.csv	all_zonalmakeuse	zonalmakeuse_temp
\.


--
-- TOC entry 1889 (class 0 OID 2372291)
-- Dependencies: 1555
-- Data for Name: activity_locations_temp; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY activity_locations_temp (activity, zonenumber, quantity, technologylogsum, sizeutility, zoneconstant, constrained, constraintvalue, locationutility, size) FROM stdin;
\.


--
-- TOC entry 1891 (class 0 OID 2372302)
-- Dependencies: 1557
-- Data for Name: activity_numbers; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY activity_numbers (activitynumber, activity) FROM stdin;
\.


--
-- TOC entry 1893 (class 0 OID 2372318)
-- Dependencies: 1559
-- Data for Name: activity_summary_temp; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY activity_summary_temp (activity, compositeutility, size) FROM stdin;
\.


--
-- TOC entry 1890 (class 0 OID 2372297)
-- Dependencies: 1556
-- Data for Name: all_activity_locations; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY all_activity_locations (year_run, scenario, activity, zonenumber, quantity, technologylogsum, sizeutility, zoneconstant, constrained, constraintvalue, locationutility, size) FROM stdin;
\.


--
-- TOC entry 1892 (class 0 OID 2372310)
-- Dependencies: 1558
-- Data for Name: all_activity_summary; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY all_activity_summary (year_run, scenario, activity, compositeutility, size) FROM stdin;
\.


--
-- TOC entry 1896 (class 0 OID 2372340)
-- Dependencies: 1562
-- Data for Name: all_commodity_zutilities; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY all_commodity_zutilities (year_run, scenario, commodity, zone, buyingorselling, quantity, zutility, variationcomponent, pricecomponent, sizecomponent, transportcomponent1, transportcomponent2, transportcomponent3, transportcomponent4) FROM stdin;
\.


--
-- TOC entry 1897 (class 0 OID 2372348)
-- Dependencies: 1563
-- Data for Name: all_exchange_results; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY all_exchange_results (year_run, scenario, commodity, zonenumber, demand, internalbought, exports, supply, internalsold, imports, surplus, price, buyingsizeterm, sellingsizeterm, derivative) FROM stdin;
\.


--
-- TOC entry 1900 (class 0 OID 2372364)
-- Dependencies: 1566
-- Data for Name: all_exchange_results_totals; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY all_exchange_results_totals (year_run, scenario, commodity, demand, internalbought, exports, supply, internalsold, imports, rmssurplus, averageprice) FROM stdin;
\.


--
-- TOC entry 1902 (class 0 OID 2372377)
-- Dependencies: 1568
-- Data for Name: all_histogram; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY all_histogram (year_run, scenario, commodity, buyingselling, bandnumber, lowerbound, quantity, averagelength) FROM stdin;
\.


--
-- TOC entry 1904 (class 0 OID 2372391)
-- Dependencies: 1570
-- Data for Name: all_latest_activity_constants; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY all_latest_activity_constants (year_run, scenario, activity, zonenumber, quantity, technologylogsum, sizeutility, zoneconstant, constrained, constraintvalue, locationutility, size) FROM stdin;
\.


--
-- TOC entry 1905 (class 0 OID 2372396)
-- Dependencies: 1571
-- Data for Name: all_makeuse; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY all_makeuse (year_run, scenario, activity, commodity, moru, coefficient, stddev, amount) FROM stdin;
\.


--
-- TOC entry 1908 (class 0 OID 2372445)
-- Dependencies: 1574
-- Data for Name: all_zonalmakeuse; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY all_zonalmakeuse (year_run, scenario, activity, zonenumber, commodity, moru, coefficient, utility, amount) FROM stdin;
\.


--
-- TOC entry 1894 (class 0 OID 2372326)
-- Dependencies: 1560
-- Data for Name: commodity_numbers; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY commodity_numbers (commoditynumber, commodity) FROM stdin;
\.


--
-- TOC entry 1895 (class 0 OID 2372334)
-- Dependencies: 1561
-- Data for Name: commodity_zutilities_temp; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY commodity_zutilities_temp (commodity, zone, buyingorselling, quantity, zutility, variationcomponent, pricecomponent, sizecomponent, transportcomponent1, transportcomponent2, transportcomponent3, transportcomponent4) FROM stdin;
\.


--
-- TOC entry 1898 (class 0 OID 2372353)
-- Dependencies: 1564
-- Data for Name: exchange_results_temp; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY exchange_results_temp (commodity, zonenumber, demand, internalbought, exports, supply, internalsold, imports, surplus, price, buyingsizeterm, sellingsizeterm, derivative) FROM stdin;
\.


--
-- TOC entry 1899 (class 0 OID 2372359)
-- Dependencies: 1565
-- Data for Name: exchange_results_totals_temp; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY exchange_results_totals_temp (commodity, demand, internalbought, exports, supply, internalsold, imports, rmssurplus, averageprice) FROM stdin;
\.


--
-- TOC entry 1901 (class 0 OID 2372369)
-- Dependencies: 1567
-- Data for Name: histogram_temp; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY histogram_temp (commodity, buyingselling, bandnumber, lowerbound, quantity, averagelength) FROM stdin;
\.


--
-- TOC entry 1903 (class 0 OID 2372385)
-- Dependencies: 1569
-- Data for Name: latest_activity_constants_temp; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY latest_activity_constants_temp (activity, zonenumber, quantity, technologylogsum, sizeutility, zoneconstant, constrained, constraintvalue, locationutility, size) FROM stdin;
\.


--
-- TOC entry 1906 (class 0 OID 2372404)
-- Dependencies: 1572
-- Data for Name: makeuse_temp; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY makeuse_temp (activity, commodity, moru, coefficient, stddev, amount) FROM stdin;
\.


--
-- TOC entry 1907 (class 0 OID 2372437)
-- Dependencies: 1573
-- Data for Name: zonalmakeuse_temp; Type: TABLE DATA; Schema: public; Owner: usrPostgres
--

COPY zonalmakeuse_temp (activity, zonenumber, commodity, moru, coefficient, utility, amount) FROM stdin;
\.


--
-- TOC entry 1888 (class 2606 OID 2372560)
-- Dependencies: 1576 1576
-- Name: aa_output_pk; Type: CONSTRAINT; Schema: public; Owner: usrPostgres; Tablespace: 
--

ALTER TABLE ONLY aa_output_files
    ADD CONSTRAINT aa_output_pk PRIMARY KEY (file_id);


--
-- TOC entry 1858 (class 2606 OID 2372309)
-- Dependencies: 1557 1557
-- Name: activity_numbers_pkey; Type: CONSTRAINT; Schema: public; Owner: usrPostgres; Tablespace: 
--

ALTER TABLE ONLY activity_numbers
    ADD CONSTRAINT activity_numbers_pkey PRIMARY KEY (activitynumber);


--
-- TOC entry 1862 (class 2606 OID 2372325)
-- Dependencies: 1559 1559
-- Name: activity_summary_pkey; Type: CONSTRAINT; Schema: public; Owner: usrPostgres; Tablespace: 
--

ALTER TABLE ONLY activity_summary_temp
    ADD CONSTRAINT activity_summary_pkey PRIMARY KEY (activity);


--
-- TOC entry 1856 (class 2606 OID 2372954)
-- Dependencies: 1556 1556 1556 1556 1556
-- Name: all_activity_locations_pkey; Type: CONSTRAINT; Schema: public; Owner: usrPostgres; Tablespace: 
--

ALTER TABLE ONLY all_activity_locations
    ADD CONSTRAINT all_activity_locations_pkey PRIMARY KEY (year_run, scenario, activity, zonenumber);


--
-- TOC entry 1860 (class 2606 OID 2372317)
-- Dependencies: 1558 1558 1558 1558
-- Name: all_activity_summary_pkey; Type: CONSTRAINT; Schema: public; Owner: usrPostgres; Tablespace: 
--

ALTER TABLE ONLY all_activity_summary
    ADD CONSTRAINT all_activity_summary_pkey PRIMARY KEY (year_run, scenario, activity);


--
-- TOC entry 1866 (class 2606 OID 2372347)
-- Dependencies: 1562 1562 1562 1562 1562 1562
-- Name: all_commodityzutilities_pkey; Type: CONSTRAINT; Schema: public; Owner: usrPostgres; Tablespace: 
--

ALTER TABLE ONLY all_commodity_zutilities
    ADD CONSTRAINT all_commodityzutilities_pkey PRIMARY KEY (year_run, scenario, commodity, zone, buyingorselling);


--
-- TOC entry 1868 (class 2606 OID 2372966)
-- Dependencies: 1563 1563 1563 1563 1563
-- Name: all_exchange_results_pkey; Type: CONSTRAINT; Schema: public; Owner: usrPostgres; Tablespace: 
--

ALTER TABLE ONLY all_exchange_results
    ADD CONSTRAINT all_exchange_results_pkey PRIMARY KEY (year_run, scenario, commodity, zonenumber);


--
-- TOC entry 1874 (class 2606 OID 2373002)
-- Dependencies: 1566 1566 1566 1566
-- Name: all_exchange_results_totals_pkey; Type: CONSTRAINT; Schema: public; Owner: usrPostgres; Tablespace: 
--

ALTER TABLE ONLY all_exchange_results_totals
    ADD CONSTRAINT all_exchange_results_totals_pkey PRIMARY KEY (year_run, scenario, commodity);


--
-- TOC entry 1876 (class 2606 OID 2372454)
-- Dependencies: 1568 1568 1568 1568 1568 1568
-- Name: all_histogram_pkey; Type: CONSTRAINT; Schema: public; Owner: usrPostgres; Tablespace: 
--

ALTER TABLE ONLY all_histogram
    ADD CONSTRAINT all_histogram_pkey PRIMARY KEY (year_run, scenario, commodity, buyingselling, bandnumber);


--
-- TOC entry 1878 (class 2606 OID 2372395)
-- Dependencies: 1570 1570 1570 1570 1570
-- Name: all_latest_activity_constants_pkey; Type: CONSTRAINT; Schema: public; Owner: usrPostgres; Tablespace: 
--

ALTER TABLE ONLY all_latest_activity_constants
    ADD CONSTRAINT all_latest_activity_constants_pkey PRIMARY KEY (year_run, scenario, activity, zonenumber);


--
-- TOC entry 1880 (class 2606 OID 2372403)
-- Dependencies: 1571 1571 1571 1571 1571 1571
-- Name: all_makeuse_pkey; Type: CONSTRAINT; Schema: public; Owner: usrPostgres; Tablespace: 
--

ALTER TABLE ONLY all_makeuse
    ADD CONSTRAINT all_makeuse_pkey PRIMARY KEY (year_run, scenario, commodity, activity, moru);


--
-- TOC entry 1886 (class 2606 OID 2372452)
-- Dependencies: 1574 1574 1574 1574 1574 1574 1574
-- Name: all_zonalmakeuse_pkey; Type: CONSTRAINT; Schema: public; Owner: usrPostgres; Tablespace: 
--

ALTER TABLE ONLY all_zonalmakeuse
    ADD CONSTRAINT all_zonalmakeuse_pkey PRIMARY KEY (year_run, scenario, commodity, zonenumber, activity, moru);


--
-- TOC entry 1864 (class 2606 OID 2372333)
-- Dependencies: 1560 1560
-- Name: commodity_numbers_pkey; Type: CONSTRAINT; Schema: public; Owner: usrPostgres; Tablespace: 
--

ALTER TABLE ONLY commodity_numbers
    ADD CONSTRAINT commodity_numbers_pkey PRIMARY KEY (commoditynumber);


--
-- TOC entry 1870 (class 2606 OID 2372978)
-- Dependencies: 1564 1564 1564
-- Name: exchange_results_pkey; Type: CONSTRAINT; Schema: public; Owner: usrPostgres; Tablespace: 
--

ALTER TABLE ONLY exchange_results_temp
    ADD CONSTRAINT exchange_results_pkey PRIMARY KEY (commodity, zonenumber);


--
-- TOC entry 1872 (class 2606 OID 2372990)
-- Dependencies: 1565 1565
-- Name: exchange_results_totals_temp_pkey; Type: CONSTRAINT; Schema: public; Owner: usrPostgres; Tablespace: 
--

ALTER TABLE ONLY exchange_results_totals_temp
    ADD CONSTRAINT exchange_results_totals_temp_pkey PRIMARY KEY (commodity);


--
-- TOC entry 1882 (class 2606 OID 2372411)
-- Dependencies: 1572 1572 1572 1572
-- Name: makeuse_temp_pkey; Type: CONSTRAINT; Schema: public; Owner: usrPostgres; Tablespace: 
--

ALTER TABLE ONLY makeuse_temp
    ADD CONSTRAINT makeuse_temp_pkey PRIMARY KEY (commodity, activity, moru);


--
-- TOC entry 1884 (class 2606 OID 2372444)
-- Dependencies: 1573 1573 1573 1573 1573
-- Name: zonalmakeuse_temp_pkey; Type: CONSTRAINT; Schema: public; Owner: usrPostgres; Tablespace: 
--

ALTER TABLE ONLY zonalmakeuse_temp
    ADD CONSTRAINT zonalmakeuse_temp_pkey PRIMARY KEY (commodity, zonenumber, activity, moru);


--
-- TOC entry 1914 (class 0 OID 0)
-- Dependencies: 3
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


-- Completed on 2010-02-24 16:49:05

--
-- PostgreSQL database dump complete
--

