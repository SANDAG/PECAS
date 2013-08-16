postgres = "postgres"
sqlserver = "sqlserver"

# Using postgreSQL or SQL Server for SD?
sql_system = sqlserver
#sql_system = postgres

#----------------------------------------------------------------------------------------------------------------------#
#2) setup runtime variables

# Define databasename and user in postgresSQL
pguser=""
pgpassword=""      
# password can sometimes also be in .pgpass file in user's home directory
# WARNING the default installation template for MAPIT does not have enough permissions for the usrPostgres user.  If usrPostgres is not a
# 'superuser' in the database you will have to change the ownership of the tables in the mapit schema to usrPostgres (using right-click in pgAdminIII
# or the database command 'ALTER TABLE output.loaded_scenarios OWNER TO "usrPostgres"')   

# If the scenario directory is on the same physical machine as the PostgreSQL database server there is a faster way to load the files that doesn't need psycopg2 
# HOWEVER this only works if the pguser (specified above) is a superuser in the database.  Normally usrPostgres is NOT a superuser while postgres is a superuser,
# (depends on how postgresql was installed)
isScenDirOnDatabaseServer=False 

# for Cube Voyager integration it would be better if the scenario was on the same physical machine as the PostgreSQL database server.
# if scenario is on database server, we need to tell the database server exactly where it is in the servers file system
EXACTPATH="F:/PECASSandag/S22/"
# Folder path on local computer to the mapped drive on pele where developmentEvents.csv gets written
LOGFILEPATH="F:/PECASSandag/S22/AllYears/Outputs/"
# Folder path on pele where developmentEvents.csv is placed
DEVEVENTPATH="F:/PECASSandag/S22/AllYears/Outputs/"
# unc path is easiest for bulk inserts on network (don't need to worry about users and mapping)
local_unc_path = '//pele/PECASSandag/S22/'
tm_input_dir="//sandag.org/transdata/data/data/sr12/PECAS/pecas_s22/"
tm_output_dir="//sandag.org/transdata/projects/sr12/OWP/pecas/pecas_s22/"


# PECAS run configuration

scendir = "F:/PECASSandag/S22/" #"./" means current directory, i.e. usually the same directory where this file is located 
sd_host="pele.sandag.org"
sd_database="pecas_sd_run"
aa_database = "pecas_sr13"
aa_scenario_id = 22
# For SQL Server sd_schema has to be blank in SD.Properties and the default schema needs to be set for the database user using management studio (#CHECK WHEN WRITING TO USE sd_rents table).
sd_schema="s22"
sd_user=""
sd_password=""
sd_port=1433
baseyear=2012
startyear=2036
endyear=2050

# Years in which new skims are available
skimyears=[2005,2012,2016,2019,2021,2026,2031,2036]


# Pattern for activities that are importers and those that are exporters.
importer_string = "%Importer%"
exporter_string = "%Exporter%"
# Column in the skims file where travel distances are stored.
distance_column = "dist_da_t_op"

mapit_host="pele.sandag.org"
mapit_port=5432
mapit_schema="output"
mapit_database='db_sandag'
scenario='s22'

# installation configuration
#Windows 
pgpath="C:/Program Files (x86)/PostgreSQL/8.4/bin/"
# Mac 
#pgpath="/opt/local/bin/"
# Windows
javaRunCommand="C:/Program Files/Java/jre7/bin/java.exe"
# Mac 
#javaRunCommand="java"

# For John's computer
# pgpath = "/opt/local/bin/"

# For SANDAG Computer
sqlpath ="C:/Program Files/Microsoft SQL Server/100/Tools/Binn/"

codepath=scendir+"AllYears/Code"

gravity_exponent = -2.0

pecasjar="PecasV2.7_r3005.jar"
commonbasejar="common-base_r2884.jar"
simpleormjar="simple_orm_r3004.jar"
flItaz="TAZ"
flIcommodity="Commodity"
flIquantity="Quantity"

