

#----------------------------------------------------------------------------------------------------------------------#
# 1) configure program and define utility routines

import os, sys, shutil
import subprocess, platform
import csv
import threading
import logging
import pecas_settings as ps
import pecas_routines as pr


def resetdatabase():
    os.environ['PGPASSWORD'] = ps.pgpassword
    command = os.path.join(ps.pgpath, "psql")
    classpath = pr.buildClassPath(ps.codepath + "/" + ps.pecasjar, ps.codepath + "/" + ps.commonbasejar,
        ps.codepath + "/or124.jar", ps.codepath + "/mtj.jar", ps.codepath + "/log4j-1.2.9.jar",
        ps.codepath + "/postgresql-8.4-701.jdbc4.jar", ps.codepath + "/sqljdbc4.jar", ps.codepath + "/" + ps.simpleormjar,
        ps.codepath + "/")

    # 3) clean up results of any previous run, in particular reset the parcels to the base year parcels

    # delete the parcels file and reload from backup
    pr.resetParcelDatabase()

    # Insert inputs to database - ActivityTotals
    # pr.loadAATotals()

    """ # remove AA outputs from SQL Server for year > ps.startyear. Need to change along with other scripts to not reset when replay.
    logging.info("resetting parcel database in database " +ps.sd_database+ " and schema "+ps.sd_schema)
    sqlstr = "EXEC aa.clean_aa_output %s, %s" % (ps.aa_scenario_id, ps.baseyear)
    retcode = pr.executeSQLServerQuery(sqlstr, ps.aa_database, ps.sd_user, ps.sd_password)
    pr.logResultsFromExternalProgram(None, "Problem removing results for "+ps.scenario+" from SQL Server database",(retcode,))
    """
    # remove outputs from mapit
    logging.info("Removing results from scenario " + ps.scenario + " from MapIt database " + ps.mapit_database + " on host " + ps.mapit_host);
    sqlstr = "SET search_path=%s; SELECT clean_up_tables_for_scenario('%s'); " % (ps.mapit_schema, ps.scenario)

    # retcode = subprocess.check_call([command, "-c", sqlstr, "--host="+ps.mapit_host, "--port="+str(ps.mapit_port) , "--dbname="+ps.mapit_database, "--username="+ps.pguser])
    # pr.logResultsFromExternalProgram(None, "Problem removing results for "+ps.scenario+" from MapIt database",(retcode,))

    # remove records from all_floorspace_taz
    # sqlstr= "SET search_path=%s; DELETE FROM all_floorspace_taz WHERE scenario='%s'; " % (ps.mapit_schema, ps.scenario)
    # retcode = subprocess.check_call([command, "-c", sqlstr, "--host="+ps.mapit_host, "--port="+str(ps.mapit_port) , "--dbname="+ps.mapit_database, "--username="+ps.pguser])
    # pr.logResultsFromExternalProgram(None, "Problem removing all_floorspace_taz for "+ps.scenario+" from MapIt database",(retcode,))

    # delete any old event.log file that might be lying around from previous runs.

    try:
            os.remove(ps.scendir + "/event.log")
    except OSError, detail:
            if detail.errno != 2:
                    raise

    #----------------------------------------------------------------------------------------------------------------------#
    # 4) let year=baseyear

    year = ps.baseyear

    # 5) while year < startyear, replay old events.
    while year < ps.startyear:
            # Fast forward to startyear by replaying events, redoing mapit load and development events application
            logging.info("Replaying PECAS Model run for year " + str(year))
            prevyear = year - 1;
            # Need to include SQL Server outputs here
            try:
                # loadThreadSQL.join()  # Mapit is not loading and multiple runs may conflict on temp tables.
                loadThreadPG.join()
            except NameError:
                pass

            # loadThreadSQL=threading.Thread(name=str(year)+" SQL loader",target=pr.loadSqlAAOutputs, args=(year,))
            # loadThreadSQL.start()
            loadThreadPG = threading.Thread(name = str(year) + " PG loader", target = pr.loadAAOutputsForYear, args = (year, command))
            loadThreadPG.start()

            if year < ps.endyear:
                pr.loadDevelopmentEvents(year)
                pr.replayDevelopmentEvents()
                pr.insertDevelopmentEventsIntoHistory(year)
                pr.applySiteSpecSANDAG(year)
                if year != ps.baseyear and (year + 1) % 5 == 0:
                    pr.parcelSnapshot(year)

            if year == (ps.startyear - 1):
                # setup floorspacesd and floorspaceo files for AA which is going to run next year
                # shouldn't this have been done from run being replayed? Currently uses EXACTPATH and won't work.
                pr.writeFloorspaceSummaryFromParcelFile(str(year + 1))
                shutil.copy(ps.scendir + "/" + str(year + 1) + "/FloorspaceI.csv", ps.scendir + "/" + str(year + 1) + "/FloorspaceSD.csv")
                shutil.copy(ps.scendir + "/" + str(year + 1) + "/FloorspaceI.csv", ps.scendir + "/" + str(year + 1) + "/FloorspaceO.csv")
            year = year + 1

    #----------------------------------------------------------------------------------------------------------------------#


