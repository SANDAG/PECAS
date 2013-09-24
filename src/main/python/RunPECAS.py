# 1) configure program and define utility routines

import os
import threading
import logging
import pecas_settings as ps
import pecas_routines as pr
import reset_database as rd
import run_aa as aa
import run_sd as sd

command = os.path.join(ps.pgpath, "psql")

def myMain():

    logging.info("**********************")
    logging.info("Starting PECAS Run in " + ps.scendir + ". MapIt scenario " + ps.scenario + ", SD schema " / 
                 + ps.sd_schema)
    rd.resetdatabase()
    year = ps.startyear
    while year <= ps.endyear:
        skimyear = pr.getSkimYear(year, ps.skimyears)
        if ps.startyear <= year:
            if ps.endyear >= year:
                # Ensure last year's output has finished loading
                try:
                    # loadThreadSQL.join()
                    loadThreadPG.join()
                except NameError:
                    pass

                aa.runAA(year, "SkimsI", skimyear)

                # Start loading output while SD runs
                # loadThreadSQL=threading.Thread(name=str(year)+" SQL loader",target=pr.loadSqlAAOutputs, args=(year,))
                # loadThreadSQL.start()
                loadThreadPG = threading.Thread(name = str(year) + " PG loader", 
                                                target = pr.loadAAOutputsForYear, args = (year, command))
                loadThreadPG.start()
            # run sd for interim years with travel model
            if ps.endyear >= year and year < 2050:
                sd.runSD(year, "SkimsI", skimyear)
        year = year + 1
        # keep skim year only as most recent travel model run
        # skimyear=skimyear+1
    # pr.prepareTravelModelInputsSANDAG(2008)
# Configure basic logger that writes to file
logging.basicConfig(filename = 'run.log',
                    level = logging.INFO,
                    format = '%(asctime)s %(name)-12s %(levelname)-8s %(message)s',
                    datefmt = '%y-%m-%d %H:%M:%S',
                    filemode = 'a'
                    )

# Configure a second logger for screen usage
console = logging.StreamHandler()  # Create console logger
console.setLevel(logging.INFO)  # Set the info level
formatter = logging.Formatter('%(name)-12s: %(levelname)-8s %(message)s')  # Create a format
console.setFormatter(formatter)  # Set the format
logging.getLogger('').addHandler(console)  # Add this to the root logger


try:
    myMain()
except Exception, err:
    logging.exception("Exception was raised: " + str(err))
