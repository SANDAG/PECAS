#14 steps sequence to build the program for the integration:

#1) configure program and define utility routines
#2) setup runtime variables
#3) clean up results of any previous run, in particular reset the parcels to the base year parcels 
#4) let year=2005
#5) do  (beginning of do-while loop, while year < endyear)
#6) Only run travel model in years divisible by 3
#7) update land use inputs to travel model
#8) Run travel model with appropriately chosen network
#9) generate new skims and set them up for AA to use
#10) RunAA for year
#11) load AA output to output database for mapit
#12) if year < endyear then RunSD for year (we don't run SD in the very last year)
#13) let year=year+1
#14) loop while year<=endyear


#----------------------------------------------------------------------------------------------------------------------#
#1) configure program and define utility routines

import os, sys
import subprocess,platform
import csv
import psycopg2 # library for interacting directly with PostgreSQL for mapit

class excelOne(csv.excel):
    # define CSV dialect for Excel to avoid blank lines from default \r\n
    lineterminator = "\n"

# installation configuration
if platform.system() == "Windows" or platform.system() == "Microsoft": pgpath="C:/Program Files (x86)/PostgreSQL/8.4/bin/"
else: pgpath="/Library/PostgresPlus/8.4SS/bin/"
if platform.system() == "Windows" or platform.system() == "Microsoft": javaRunCommand="C:/Program Files/Java/jre6/bin/java.exe"
else: javaRunCommand="java"

command = os.path.join(pgpath, "psql")

# Two utility routines used below
# Append paths to the classpath using the right separator for Windows or Unix
def buildClassPath(*arg):
        string = ""
        if platform.system() == "Windows" or platform.system() == "Microsoft":
                separator=";"
        else:
                separator=":"
        for a in arg:
                string = str(string)+str(a)+separator
        return string

# Move a file to a new location, deleting any old file there.
def moveReplace(arg1, arg2):
        try:
                os.remove(arg2)
        except OSError, detail:
                if detail.errno != 2:
                        raise
        os.rename(arg1, arg2)


#----------------------------------------------------------------------------------------------------------------------#
#2) setup runtime variables

# Define databasename and user in postgresSQL
pguser="usrPostgres"
pgpassword="usrPostgres"        
# password can sometimes also be in .pgpass file in user's home directory

# If the scenario directory is on the same physical machine as the PostgreSQL database server there is a faster way to load the files that doesn't need psycopg2 
isScenDirOnDatabaseServer=False 
# for Cube Voyager integration it would be better if the scenario was on the same physical machine as the PostgreSQL database server.
# if scenario is on database server, we need to tell the database server exactly where it is in the servers file system
exactpath="D:/PECASOregon/C05/" 

os.environ['PGPASSWORD']=pgpassword

# PECAS run configuration
scendir = "./" #"./" means current directory, i.e. usually the same directory where this file is located 
database="scag_db"
sd_schema="w00"
baseyear=2007
endyear=2010

host="localhost"
port=5432
mapit_schema="output"
scenario='W00'

codepath=scendir+"AllYears/Code"

classpath=buildClassPath(codepath+"/PecasV2.6_r2022.jar", codepath+"/common-base_r2022.jar",
codepath+"/or124.jar",codepath+"/mtj.jar",codepath+"/log4j-1.2.9.jar",
codepath+"/postgresql-8.4-701.jdbc4.jar",codepath+"/simple_orm_r1504.jar",
codepath+"/PecasAssign_r2042.jar",codepath+"/")

print "classpath is "+classpath


#----------------------------------------------------------------------------------------------------------------------#
#3) clean up results of any previous run, in particular reset the parcels to the base year parcels 

# delete the parcels file and reload from backup

retcode = subprocess.call([pgpath+"psql", "-c", 
        "SET search_path="+sd_schema+"; DELETE from local_effect_distances WHERE pecas_parcel_num >(SELECT MAX(pecas_parcel_num) FROM parcels_backup);",
        "--dbname="+database, "--username="+pguser]) 
retcode = subprocess.call([pgpath+"psql", "-c", 
        "SET search_path="+sd_schema+";DELETE from parcel_cost_xref WHERE pecas_parcel_num >(SELECT MAX(pecas_parcel_num) FROM parcels_backup);",
        "--dbname="+database, "--username="+pguser]) 
retcode = subprocess.call([pgpath+"psql", "-c", 
        "SET search_path="+sd_schema+";DELETE from parcel_fee_xref WHERE pecas_parcel_num >(SELECT MAX(pecas_parcel_num) FROM parcels_backup);",
        "--dbname="+database, "--username="+pguser]) 
retcode = subprocess.call([pgpath+"psql", "-c", 
        "SET search_path="+sd_schema+";DELETE from parcel_zoning_xref WHERE pecas_parcel_num >(SELECT MAX(pecas_parcel_num) FROM parcels_backup);",
        "--dbname="+database, "--username="+pguser]) 
retcode = subprocess.call([pgpath+"psql", "-c", 
        "SET search_path="+sd_schema+"; DELETE from parcels WHERE pecas_parcel_num >(SELECT MAX(pecas_parcel_num) FROM parcels_backup);",
        "--dbname="+database, "--username="+pguser])
retcode = subprocess.call([pgpath+"psql", "-c", 
        "SET search_path="+sd_schema+";UPDATE parcels p SET year_built= bak.year_built, space_type_id=bak.space_type_id, space_quantity=bak.space_quantity, land_area=bak.land_area, available_services_code=bak.available_services_code, is_derelict=bak.is_derelict, is_brownfield=bak.is_brownfield FROM parcels_backup bak WHERE p. pecas_parcel_num = bak.pecas_parcel_num;",
        "--dbname="+database, "--username="+pguser]) 

# delete any old event.log file that might be lying around from previous runs.
        
try:
        os.remove(scendir+"/event.log")
except OSError, detail:
        if detail.errno != 2:
                raise

# remove outputs from mapit
sqlstr= "SET search_path=%s; SELECT clean_up_tables_for_scenario('%s'); " % (mapit_schema, scenario)
retcode = subprocess.check_call([command, "-c", sqlstr, "--host="+host, "--port="+str(port) , "--dbname="+database, "--username="+pguser]) 


#----------------------------------------------------------------------------------------------------------------------#
#4) let year=2005


year=baseyear
                
                
#----------------------------------------------------------------------------------------------------------------------#
#5) do  (beginning of do-while loop, while year < endyear)


while year <= endyear:
        print "year is "+str(year)
        relyear=year-baseyear
        
        if year==baseyear:
                constrained=True
        else:
                constrained=False

        prevyear=year-1;
        print "prevyear ="+str(prevyear)

        #----------------------------------------------------------------------------------------------------------------------#        
        #6) Only run travel model in years divisible by 3

        # checks if year is divisable by 3, so run travel model in year 2007, 2010, etc (but not in the baseyear because we have starting skims)
        if (year % 3) == 0 and (year!=baseyear):
                skimfile=str(year)+"skim"
                skimyear=year
                networkyear=2007 # specifies where to find the network files for the travel model, this demo always uses the 2007 network but in general the network closest in time to the current year should be used.
                # if the travel model does not use different file system directories to represent different scenarios there would be additional code to select the appropriate scenario as well as the appropriate year

                #----------------------------------------------------------------------------------------------------------------------#    
                #7) update land use inputs to travel model
                # NOTE there is no code in this section because in the simple pecas assignment process the trips
                # are generated directly by the pecas AA module, in Atlanta we plan to improve this section
                # iteratively, 
                # first, just pick appropriate pre-defined land use inputs for the year,
                # second, scale the pre-defined land use inputs based on TAZ growth rates from ActivityLocations2, and
                # third, do something more sophisticated with labor categories, jobs, income, hhsize, etc.
                
                #----------------------------------------------------------------------------------------------------------------------#    
                #8) Run travel model with appropriately chosen network
                
                # our travel model is a simple frank-wolfe assignment (trip generation is actually done within the AA code for software performance reasons)
                retcode = subprocess.call([javaRunCommand, "-Xmx3000M", "-Dlog4j.configuration=log4j.xml", "-DNETWORKYEAR="+str(networkyear),
                "-DSCENDIR="+str(scendir), "-DYEAR="+str(year-1), # use last year's trip table which AA generated
                "-DSKIMYEAR="+str(skimyear), "-DSKIMFILE="+str(skimfile), # write out this year's skims for AA to use
                "-cp", buildClassPath(scendir+"AllYears/Inputs",classpath), "com.hbaspecto.pecas.assign.PecasAssign"])
                moveReplace(scendir+"/event.log", scendir+"/"+str(year)+"/assign-event.log")

                
                #----------------------------------------------------------------------------------------------------------------------#    
                #9) generate new skims and set them up for AA to use
                
                # assignment program already created a skim.csv file, all we need to do is append the external zone skims.
                fin = open(scendir+"AllYears/Inputs/Externalskims.csv", "r")
                fout = open(scendir+"/"+str(year)+"/"+str(year)+"skim.csv", "a") # open in "append" mode, so we can append the external skims 
                inFile = csv.reader(fin, excelOne)
                outFile = csv.writer(fout, excelOne)

                for row in inFile:
                    outFile.writerow(row)

                fin.close()
                fout.close()

        

        #----------------------------------------------------------------------------------------------------------------------#
        #10) RunAA for year

        # Run AA model for year 

        if year==baseyear: # setup the skims in the base year (we haven't run the travel model if it's the baseyear but the skims exist as part of the base setup)
                skimfile="scag_skim" 
                skimyear=2007
        
        # Run AA
        retcode = subprocess.call([javaRunCommand, "-Xmx3000M", "-Dlog4j.configuration=log4j.xml", "-DSCENDIR="+scendir, 
           "-DYEAR="+str(year), "-DSKIMFILE="+str(skimfile),
           "-DPREVYEAR="+str(prevyear), "-DCONSTRAINED="+str(constrained), "-cp", buildClassPath(scendir+"AllYears/Inputs",classpath),
           "com.hbaspecto.pecas.aa.control.AAControl"])
        
        moveReplace(scendir+"/event.log", scendir+"/"+str(year)+"/aa-event.log")

        #----------------------------------------------------------------------------------------------------------------------#
        #11) load AA output to output database for mapit

        print "\nLoading data for year:" + str(year)
        if isScenDirOnDatabaseServer :  # the easy way making PostgreSQL do all the work, can do this if the files are on the same machine as the database server
            folder_path = os.path.join(exactpath, str(year))
            folder_path = folder_path.replace('\\', '/')
            # Abdel wrote all the compexity into the load_aa_output routine on the database, so this is simple here in python
            sqlstr = "SET search_path="+mapit_schema+"; SELECT load_aa_output('"+ folder_path +"' , "+str(year)+", '"+ scenario +"' ); "
            print sqlstr
            retcode = subprocess.check_call([command, "-c",sqlstr, "--host="+host, "--port="+str(port) , "--dbname="+database, "--username="+pguser]) 
        else: # the hard way using the psycopg2 python library, where python is feeding the results to the database server
            folder_path = os.path.join(scendir, str(year))
            folder_path = folder_path.replace('\\', '/')
            conn = psycopg2.connect(database=database, host=host, port=port, user=pguser,  password=pgpassword ) 
            cur = conn.cursor()
            cur.execute('SELECT * FROM %s.aa_output_files;' % mapit_schema) # get the list of aa output files we are interested in
            filesrows = cur.fetchall()

            for row in filesrows: # for each output file
                csv_file_name=row[1]
                all_table_name=row[2]
                temp_table_name=row[3]
                
                cur.execute('TRUNCATE %s.%s' % (mapit_schema, temp_table_name)) # empty the temporary table
                csv_file = os.path.join(folder_path, csv_file_name)
                f = open(csv_file, 'r') # open the AA output file
                f.readline() # skip the first line for header
                cur.copy_from(f, "%s.%s" %(mapit_schema,temp_table_name), sep=',', null='') # use the psycopg2 fast copy command to copy the data into the temporary table

                cur.execute('SELECT count(*) FROM %s.%s;' %(mapit_schema, temp_table_name))
                counter = cur.fetchone()[0]

                # now insert the records from the temporary table into the full table which contains data for each year/scenario
                sqlcmd= "INSERT INTO %s.%s ( SELECT %d, '%s', tbl.* FROM %s.%s as tbl )" %(mapit_schema, all_table_name, year, scenario, mapit_schema, temp_table_name)
                cur.execute(sqlcmd)

                print "%d record(s) added to %s" % (counter, all_table_name)                                        
            cur.execute('SET search_path = %s ' % mapit_schema)
            cur.callproc('output.update_loaded_scenarios', [scenario, year, year])
            conn.commit()
        
        #----------------------------------------------------------------------------------------------------------------------#                
        #12) if year < endyear then RunSD for year (we don't run SD in the very last year)
        if year<endyear: 
                print "running SD model for " + str(year)

                retcode = subprocess.call([javaRunCommand, "-Xmx1500M",  "-DSCENDIR="+scendir, "-Ddatabase="+database,
                "-Ddatabaseuser="+pguser, "-DPGPASSWORD="+pgpassword, "-Dschema="+sd_schema, "-cp", buildClassPath(scendir+"AllYears/Inputs",classpath), 
                "com.hbaspecto.pecas.sd.StandardSDModel", str(baseyear), str(relyear)])

                # backup the parcel database every 5 years
                if (year-5) %10 == 0:
                        retcode = subprocess.call([pgpath+"psql", "-c", 
                        "SET search_path="+sd_schema+"; drop table if exists parcels_"+str(year)+"; create table parcels_"+str(year)+" as select * from parcels;",
                        "--dbname="+database, "--username="+pguser]) 

                # Copy logfile to year
                moveReplace(scendir+"/AllYears/outputs/developmentEvents.csv", scendir+"/"+str(year)+"/developmentEvents.csv")
                if year==baseyear:
                        # reset develpments_history if it's the first year, and insert new records
                        retcode = subprocess.call([pgpath+"psql", "-c",
                        "SET search_path="+sd_schema+"; TRUNCATE development_events_history; INSERT INTO development_events_history (select "+str(year)+" as year_run,* from development_events);",
                        "--dbname="+database, "--username="+pguser]) 
                else:
                        # if it's not the first year just insert this year's records
                        retcode = subprocess.call([pgpath+"psql", "-c",
                        "SET search_path="+sd_schema+"; INSERT INTO development_events_history (select "+str(year)+" as year_run,* from development_events);",
                        "--dbname="+database, "--username="+pguser]) 
                
                moveReplace(scendir+"/event.log", scendir+"/"+str(year)+"/sd-event.log")

                











        #----------------------------------------------------------------------------------------------------------------------#    
        #13) let year=year+1
        year=year+1




        #----------------------------------------------------------------------------------------------------------------------#    
        #14) loop while year<=endyear
        # loops are implied by indentation in the python language, no "while", "repeat", or curly brace required.
    






