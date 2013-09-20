
#----------------------------------------------------------------------------------------------------------------------#
#1) configure program and define utility routines

import os, sys, shutil
import subprocess,platform
import csv
import threading
import logging
import pecas_settings as ps
import pecas_routines as pr

def runAA(year,skimfile,skimyear):
    #setup some variables that are constructed with code
    os.environ['PGPASSWORD']=ps.pgpassword
    command = os.path.join(ps.pgpath, "psql")
    classpath=classpath=pr.buildClassPath(ps.codepath+"/"+ps.pecasjar, ps.codepath+"/"+ps.commonbasejar,ps.codepath+"/*"+ps.commonbasejar,ps.codepath+'/*',ps.codepath+"/")
    
    #----------------------------------------------------------------------------------------------------------------------#
    #do model runs (beginning of do-while loop, while year < endyear)

    relyear=year-ps.baseyear
    
    if year==ps.baseyear:
            constrained=True #Only using 2004 activity locations produced by constrained 2012 run
    else:
            constrained=False

    prevyear=year-1;
    logging.debug("prevyear ="+str(prevyear))

    #----------------------------------------------------------------------------------------------------------------------#
    #10) RunAA for year

    # Run AA model for year 

    # Create FloorspaceI (using base parcels for initial option size calib)

    pr.writeFloorspaceI(year)

    """ Don't update because these are now fixed
    if year<>ps.baseyear:
        pr.updateExporters(year)
        pr.updateImporters(year)
        pr.writeActivityTotals(year, command)
    """

    # Run AA
    
    logging.info("Running AA with -Dscendir="+ps.scendir+" -DYEAR="+str(year)+ " -DSKIMYEAR="+str(skimyear)+ " -DSKIMFILE="+str(skimfile)+" -DPREVYEAR="+str(prevyear)+ " -DCONSTRAINED="+str(constrained))
    logging.info("classpath is "+classpath)
    retcode = subprocess.call([ps.javaRunCommand, "-Xmx2500M", "-Dlog4j.configuration=log4j.xml", "-DSCENDIR="+ps.scendir, 
       "-DYEAR="+str(year), "-DSKIMYEAR="+str(skimyear), "-DSKIMFILE="+str(skimfile),
       "-DPREVYEAR="+str(prevyear), "-DCONSTRAINED="+str(constrained), "-cp", pr.buildClassPath(ps.scendir+"AllYears/Inputs",classpath),
       "com.hbaspecto.pecas.aa.control.AAControl"])
    pr.logResultsFromExternalProgram("AA model finished", "AA model did not run successfully in year "+str(year),(retcode,))
    
    pr.moveReplace(ps.scendir+"/event.log", ps.scendir+"/"+str(year)+"/aa-event.log")
    
    #----------------------------------------------------------------------------------------------------------------------#
    #11) load AA output to output database for mapit and SQL moved to run script
    
    '''# Write Travel Model Inputs
    if year<> ps.baseyear:
                pr.prepareTravelModelInputs(year, ps.scenario)
                retcode = pr.executePostgreSQLQuery("\\copy input.tm_input to '"+ps.EXACTPATH+"/"+str(year)+"/tm_inputs.csv' CSV HEADER", ps.mapit_database, ps.mapit_port, ps.mapit_host, ps.pguser)
                pr.logResultsFromExternalProgram("Wrote out travel model inputs", "Problem writing travel model land-use inputs for "+str(year), (retcode,))'''


