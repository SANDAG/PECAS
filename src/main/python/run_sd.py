
#----------------------------------------------------------------------------------------------------------------------#
# 1) configure program and define utility routines

import os, sys, shutil
import subprocess, platform
import csv
import threading
import logging
import pecas_settings as ps
import pecas_routines as pr


def runSD(year, skimfile, skimyear):
    # 2) setup some variables that are constructed with code
    os.environ['PGPASSWORD'] = ps.pgpassword
    command = os.path.join(ps.pgpath, "psql")
    relyear = year - ps.baseyear
    classpath = classpath = pr.buildClassPath(ps.codepath + "/" + ps.pecasjar, ps.codepath + "/" + ps.commonbasejar, 
                                              ps.codepath + "/*" + ps.commonbasejar, 
                                              ps.codepath + '/*', ps.codepath + "/")

    # Apply price smoothing.
    pr.loadDistances(skimfile, skimyear)
    pr.loadExchangeResults(year)
    pr.smoothPricesSandag(year)
    # For General Smoothing
    # pr.smoothPrices()

    logging.info("running SD model for " + str(year) + " with -DSCENDIR=" + ps.scendir + " -Ddatabase=" \
                 + ps.sd_database + " -Dport=" + str(ps.sd_port) + " -Ddatabaseuser=" + ps.sd_user \
                 + "-Dhost=" + ps.sd_host + " -DSD_SCHEMA=" + ps.sd_schema)
    retcode = subprocess.call([ps.javaRunCommand, "-Xmx5000M", "-DSCENDIR=" + ps.scendir, 
                               "-Ddatabase=" + ps.sd_database, "-Dport=" + str(ps.sd_port),
                               "-Ddatabaseuser=" + ps.sd_user, "-Dhost=" + ps.sd_host, 
                               "-DSDPASSWORD=" + ps.sd_password, "-DSD_SCHEMA=" + ps.sd_schema, 
                               "-DEXACTPATH=" + ps.EXACTPATH, "-DDEVEVENTPATH=" + ps.DEVEVENTPATH, 
                               "-DLOGFILEPATH=" + ps.LOGFILEPATH, "-cp", 
                               pr.buildClassPath(ps.scendir + "AllYears/Inputs", classpath),
      "com.hbaspecto.pecas.sd.StandardSDModel", str(ps.baseyear), str(relyear)])
    pr.moveReplace(ps.scendir + "/event.log", ps.scendir + "/" + str(year) + "/sd-event.log")
    pr.logResultsFromExternalProgram("SD model Finished", "SD model did not run successfully in year " + str(year), 
                                     (retcode,))

    # Copy logfile to year
    pr.moveReplace(ps.LOGFILEPATH + "developmentEvents.csv", ps.scendir + "/" + str(year) + "/developmentEvents.csv")
    shutil.copy(ps.scendir + "/" + str(year) + "/developmentEvents.csv", 
                ps.LOGFILEPATH + "/" + "developmentEvents" + str(year) + ".csv")
    pr.insertDevelopmentEventsIntoHistory(year)

    shutil.copy(ps.scendir + "/" + str(year + 1) + "/FloorspaceI.csv", 
                ps.scendir + "/" + str(year + 1) + "/FloorspaceSD.csv")
    shutil.copy(ps.scendir + "/" + str(year + 1) + "/FloorspaceI.csv", 
                ps.scendir + "/" + str(year + 1) + "/FloorspaceO.csv")

    # Insert sitepec parcels. They will be output by SD after its next run.
    pr.applySiteSpecSANDAG(year)

    # Take snapshot of parcels on for when the next year of AA is divisible by 5
    if year != ps.baseyear and (year + 1) % 5 == 0:
        pr.parcelSnapshot(year)
