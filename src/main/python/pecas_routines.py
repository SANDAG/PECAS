import csv
import logging
import os, shutil
import subprocess, platform
import pecas_settings as ps
# import psycopg2 # library for interacting directly with PostgreSQL for mapit
import bisect
import update_tm_inputs



def executePostgreSQLQuery(query, database, port, host, user):
    return subprocess.call([ps.pgpath + "psql", "-c", query, "--dbname=" + database, 
                            "--port=" + str(port), "--host=" + host, "--username=" + user])

def executeSQLServerQuery(query, database, user, password):
    return subprocess.call([ps.sqlpath + "sqlcmd", "-S", ps.sd_host, "-U", user, 
                            "-P", password, "-d", database, "-Q", query])

# Four utility routines used below

def buildClassPath(*arg):
    """ Append paths to the classpath using the right separator for Windows or Unix"""
    string = ""
    if platform.system() == "Windows" or platform.system() == "Microsoft":
        separator = ";"
    else:
        separator = ":"
    for a in arg:
        string = str(string) + str(a) + separator
    return string


def moveReplace(arg1, arg2):
    """ # Move a file to a new location, deleting any old file there."""
    try:

        os.remove(arg2)
    except OSError, detail:
        if detail.errno != 2:
            raise

    os.rename(arg1, arg2)

def getSkimYear(year, skimyears):
    """# Grab the skim year to use given a list of years with new skims"""
    i = bisect.bisect_right(skimyears, year) - 1
    return skimyears[i]

def calculateAAtoSDPriceCorrection():
    """
    Function to calculate the base year ratio between the prices that 
    AA is producing and the prices that SD has been calibrated to
    """
    
    fin = open(ps.scendir + "/" + str(baseyear) + "/ExchangeResults.csv", "rU")
    exchangeResults = csv.reader(fin)
    erHeader = exchangeResults.next()
    fin2 = open(ps.scendir + "/" + str(baseyear) + "/ExchangeResultsTargets.csv", "rU")
    erTargets = csv.reader(fin2)
    targetsHeader = erTargets.next()
    fout = open(ps.scendir + "/AllYears/Outputs/AAtoSDPriceCorrections.csv", "w")
    outWriter = csv.writer(fout, excelOne)
    outWriter.writerow(("Commodity", "LUZ", "PriceCorrection"))
    targetPrices = {}
    for row in erTargets:
        commodity = row[targetsHeader.index("Commodity")]
        zone = row[targetsHeader.index("ZoneNumber")]
        key = (commodity, zone)
        price = row[targetsHeader.index("Price")]
        targetPrices[key] = price
    for row in exchangeResults:
        commodity = row[erHeader.index("Commodity")]
        zone = row[erHeader.index("ZoneNumber")]
        key = (commodity, zone)
        if targetPrices.has_key(key):
            aaPrice = row[erHeader.index("Price")]
            targetPrice = targetPrices[key]
            priceCorrection = float(targetPrice) / float(aaPrice)
            outWriter.writerow((commodity, zone, priceCorrection))
    fin.close()
    fin2.close()
    fout.close()

def applyAAtoSDPriceCorrection():
    """# Function to multiply the AA prices by the previously calculated correction ratio."""
    fin = open(ps.scendir + "/" + str(year) + "/ExchangeResults.csv", "rU")
    exchangeResults = csv.reader(fin)
    fin2 = open(ps.scendir + "/AllYears/Outputs/AAtoSDPriceCorrections.csv", "rU")
    corrections = csv.reader(fin2)
    fout = open(ps.scendir + "/" + str(year) + "/SDPrices.csv", "w")
    outWriter = csv.writer(fout, excelOne)
    erHeader = exchangeResults.next()
    corrHeader = corrections.next()
    outWriter.writerow(erHeader)
    priceCorrections = {}
    for row in corrections:
        commodity = row[corrHeader.index("Commodity")]
        zone = row[corrHeader.index("LUZ")]
        key = (commodity , zone)
        priceCorrection = row[corrHeader.index("PriceCorrection")]
        priceCorrections[key] = priceCorrection
    for row in exchangeResults:
        commodity = row[erHeader.index("Commodity")]
        zone = row[erHeader.index("ZoneNumber")]
        key = (commodity, zone)
        if priceCorrections.has_key(key):
            price = float(row[erHeader.index("Price")])
            newprice = price * float(priceCorrections[key])
            if newprice > 100:
                newprice = 100
            row[erHeader.index("Price")] = newprice
        outWriter.writerow(row)
    fin.close()
    fin2.close()
    fout.close()

    logging.info("Replacing ExchangeResults with SD Corrected Version")

    # for now copy corrected prices to ExchangeResults.csv, but want to change SD code to read SDPrices.csv
    moveReplace(ps.scendir + "/" + str(year) + "/ExchangeResults.csv", 
                ps.scendir + "/" + str(year) + "/AAExchangeResults.csv")
    shutil.copy(ps.scendir + "/" + str(year) + "/SDPrices.csv", 
                ps.scendir + "/" + str(year) + "/ExchangeResults.csv")


def loadAAOutputsForYear(year, command):
    logging.info("Loading data for year:" + str(year))
    # the easy way making PostgreSQL do all the work, 
    # can do this if the files are on the same machine as the database server
    if ps.isScenDirOnDatabaseServer :  
        folder_path = os.path.join(EXACTPATH, str(year))
        folder_path = folder_path.replace('\\', '/')
        # Abdel wrote all the compexity into the load_aa_output 
        # routine on the database, so this is simple here in python
        sqlstr = "SET search_path=" + ps.mapit_schema + "; SELECT load_aa_output('" \
            + folder_path + "' , " + str(year) + ", '" + ps.scenario + "' ); "
        retcode = subprocess.check_call([command, "-c", sqlstr, "--host=" + ps.mapit_host, 
                                         "--port=" + str(ps.mapit_port) , 
                                         "--dbname=" + ps.mapit_database, "--username=" + ps.pguser])
        logResultsFromExternalProgram("loaded AA Data", "Failed to load AA data", (retcode,))

        # aggregate mgra floorspace to TAZ to view in mapit

        sqlstr = "INSERT INTO " + ps.mapit_schema + ".all_floorspace_taz(year_run, scenario, taz, commodity, quantity) \
                SELECT a.year_run, a.scenario, x.taz, a.aa_commodity AS commodity, \
                sum(a.quantity) AS quantity \
                FROM output.all_floorspacei a, public.mgra_to_taz x \
                WHERE a.taz = x.mgra and a.scenario='" + ps.scenario + "' and a.year_run =" + str(year) + \
                " GROUP BY a.year_run, a.scenario, x.taz, a.aa_commodity;"
        retcode = subprocess.check_call([command, "-c", sqlstr, "--host=" + ps.mapit_host, 
                                         "--port=" + str(ps.mapit_port) , 
                                         "--dbname=" + ps.mapit_database, "--username=" + ps.pguser])
        logResultsFromExternalProgram("loaded all_floorspace_taz", "Failed to load all_floorspace_taz", (retcode,))

    else:  # the hard way using the psycopg2 python library, where python is feeding the results to the database server
        folder_path = os.path.join(ps.scendir, str(year))
        folder_path = folder_path.replace('\\', '/')
        conn = psycopg2.connect(database = ps.mapit_database, host = ps.mapit_host, 
                                port = ps.mapit_port, user = ps.pguser, password = ps.pgpassword)
        cur = conn.cursor()
        # get the list of aa output files we are interested in
        cur.execute('SELECT * FROM %s.aa_output_files;' % ps.mapit_schema)  
        filesrows = cur.fetchall()

        for row in filesrows:  # for each output file
            csv_file_name = row[1]
            all_table_name = row[2]
            temp_table_name = row[3]

            cur.execute('TRUNCATE %s.%s' % (ps.mapit_schema, temp_table_name))  # empty the temporary table
            csv_file = os.path.join(folder_path, csv_file_name)
            f = open(csv_file, 'r')  # open the AA output file
            f.readline()  # skip the first line for header
            # use the psycopg2 fast copy command to copy the data into the temporary table
            cur.copy_from(f, "%s.%s" % (ps.mapit_schema, temp_table_name), sep = ',', null = '')  

            cur.execute('SELECT count(*) FROM %s.%s;' % (ps.mapit_schema, temp_table_name))
            counter = cur.fetchone()[0]

            # now insert the records from the temporary table into the full table 
            # which contains data for each year/scenario
            sqlcmd = "INSERT INTO %s.%s ( SELECT %d, '%s', tbl.* FROM %s.%s as tbl )" \
                % (ps.mapit_schema, all_table_name, year, ps.scenario, ps.mapit_schema, temp_table_name)
            cur.execute(sqlcmd)

            logging.debug("%d record(s) added to %s" % (counter, all_table_name))
        cur.execute('SET search_path = %s ' % ps.mapit_schema)
        cur.callproc('output.update_loaded_scenarios', [ps.scenario, year, year])
        conn.commit()
        conn.close()

        sqlstr = "INSERT INTO " + ps.mapit_schema + ".all_floorspace_taz(year_run, scenario, taz, commodity, quantity) \
                SELECT all_floorspacei.year_run, all_floorspacei.scenario, mgra_to_taz.taz, \
                all_floorspacei.aa_commodity AS commodity, \
                sum(all_floorspacei.quantity) AS quantity \
                FROM output.all_floorspacei, public.mgra_to_taz \
                WHERE all_floorspacei.taz = mgra_to_taz.mgra and all_floorspacei.scenario='" \
                + ps.scenario + "' and all_floorspacei.year_run =" + str(year) + \
                " GROUP BY all_floorspacei.year_run, all_floorspacei.scenario, \
                mgra_to_taz.taz, all_floorspacei.aa_commodity;"
        retcode = subprocess.check_call([command, "-c", sqlstr, "--host=" + ps.mapit_host, 
                                         "--port=" + str(ps.mapit_port) , "--dbname=" + ps.mapit_database, 
                                         "--username=" + ps.pguser])
        logResultsFromExternalProgram("loaded all_floorspace_taz", "Failed to load all_floorspace_taz", (retcode,))

    logging.info("Finished loading data for year:" + str(year))


def loadSqlAAOutputs(year):
    query = "EXECUTE aa.load_aa_output '%s', %s, %s" % (os.path.abspath(ps.local_unc_path), ps.aa_scenario_id, year)
    retcode = executeSQLServerQuery(query, ps.aa_database, ps.sd_user, ps.sd_password)
    logResultsFromExternalProgram("AA results loaded into SQL Server", "Failed in loading AA outputs to SQL Server ", 
                                  (retcode,))



def writeFloorspaceI(year):
    """# Function to adjust the Floorspace by the (previously calculated) correction delta."""
    # Read Floorspace O
    floorOIn = open(ps.scendir + str(year) + "/FloorspaceO.csv", "r")
    floorOInFile = csv.reader(floorOIn)
    header = floorOInFile.next()
    floorODict = {}
    for row in floorOInFile:
        key = (row[header.index(ps.flItaz)], row[header.index(ps.flIcommodity)])
        if floorODict.has_key(key):
            logging.warn("WARNING: Line duplicated in FloorspaceO file:", key)
            floorODict[key] = floorODict[key] + float(row[header.index(ps.flIquantity)])
        else:
            floorODict[key] = float(row[header.index(ps.flIquantity)])
    floorOIn.close()

    hasC = False
    try:
        floorCIn = open(ps.scendir + str(year) + "/FloorspaceCalc.csv", "r")
        hasC = True
    except IOError as e:
        logging.info("NOTICE: FloorspaceCalc not found, using FloorspaceDelta file.")

    if hasC:
        # Read floorspace Calc
        floorCInFile = csv.reader(floorCIn)
        header = floorCInFile.next()
        floorCDict = {}
        for row in floorCInFile:
            key = (row[header.index(ps.flItaz)], row[header.index(ps.flIcommodity)])
            if floorCDict.has_key(key):
                logging.warn("WARNING: Line duplicated in FloorspaceCalc file:", key)
                floorCDict[key] = floorCDict[key] + float(row[header.index(ps.flIquantity)])
            else:
                floorCDict[key] = float(row[header.index(ps.flIquantity)])
        floorCIn.close()

        # Write floorspace Delta
        floorDOut = open(ps.scendir + str(year) + "/FloorspaceDelta.csv", "w")
        floorDOutFile = csv.writer(floorDOut, excelOne)
        header = [ps.flItaz, ps.flIcommodity, ps.flIquantity]
        floorDOutFile.writerow(header)
        keyList = floorCDict.keys()
        keyList.sort()
        for key in keyList:
            if floorODict.has_key(key):
                delta = floorCDict[key] - floorODict[key]
            else:
                delta = floorCDict[key]
            outRow = list(key)
            outRow.append(delta)
            floorDOutFile.writerow(outRow)

        # Add in ODict values not in CDict; set delta to -ve of ODict value
        keyList = floorODict.keys()
        for key in keyList:
            if floorCDict.has_key(key):
                pass
            else:
                delta = -1 * floorODict[key]
                outRow = list(key)
                outRow.append(delta)
                floorDOutFile.writerow(outRow)
        floorDOut.close()
    else:
        # Copy from previous year
        shutil.copy(ps.scendir + "/" + str(year - 1) + "/FloorspaceDelta.csv", 
                    ps.scendir + "/" + str(year) + "/FloorspaceDelta.csv")


    # Read floorspace Delta
    floorDIn = open(ps.scendir + str(year) + "/FloorspaceDelta.csv", "r")
    floorDInFile = csv.reader(floorDIn)
    header = floorDInFile.next()
    floorDDict = {}
    for row in floorDInFile:
        key = (row[header.index(ps.flItaz)], row[header.index(ps.flIcommodity)])
        if floorDDict.has_key(key):
            logging.warn("WARNING: Line duplicated in FloorspaceDelta file:", key)
            floorDDict[key] = floorDDict[key] + float(row[header.index(ps.flIquantity)])
        else:
            floorDDict[key] = float(row[header.index(ps.flIquantity)])
    floorDIn.close()

    # Write floorspace I
    if hasC:
        # copy FloorspaceCalc as FloorspaceI
        shutil.copy(ps.scendir + "/" + str(year) + "/FloorspaceCalc.csv", 
                    ps.scendir + "/" + str(year) + "/FloorspaceI.csv")
    else:
        floorIOut = open(ps.scendir + str(year) + "/FloorspaceI.csv", "w")
        floorIOutFile = csv.writer(floorIOut, excelOne)
        header = [ps.flItaz, ps.flIcommodity, ps.flIquantity]
        floorIOutFile.writerow(header)
        keyList = floorDDict.keys()
        keyList.sort()
        for key in keyList:
            if floorODict.has_key(key):
                net = floorDDict[key] + floorODict[key]
                if net < 0:
                    logging.debug("  Negative value for floorspace in", key)
                    net = 0
            else:
                # print "WARNING: Key", key, "in floorspaceO, but not in floorspaceDelta."
                net = floorDDict[key]
                if net < 0:
                    logging.debug("  WARNING: Negative value for floorspace in", key)
                    net = 0
            outRow = list(key)
            outRow.append(net)
            floorIOutFile.writerow(outRow)

        # Add in ODict values not in DDict; set net to ODict value
        keyList = floorODict.keys()
        for key in keyList:
            if floorDDict.has_key(key):
                pass
            else:
                net = floorODict[key]
                if net < 0:
                    logging.debug("  WARNING: Negative value for floorspace in", key)
                    net = 0
                outRow = list(key)
                outRow.append(net)
                floorIOutFile.writerow(outRow)
        floorIOut.close()



def updateImportersOrExporters(year, activitiesToExclude, activitiesToUpdate, updateUsingMorU):
    """
    Function to calculate and update total imports and exports in ActivityTotalsI 
    in future years (base year has to be set before running.
    """
    sqlstr = "update input.activity_totals \n\
    set total_amount=(case when '" + updateUsingMorU + "'='U' then -im.amount else im.amount end) from \n\
        -- select mu.activity, im.amount from \n\
        ( \n\
               -- inner query is amount made (or used in the case of updating imports) internally last year \n\
                select commodity,moru, sum(amount) as amount from output.all_makeuse \n\
                where scenario='" + ps.scenario + "' and year_run=" + str(year - 1) + " and moru='" + updateUsingMorU \
                + "' and activity not like '" + activitiesToExclude + "' \n\
        group by commodity, moru \n\
     ) im, output.all_makeuse mu \n\
    where mu.scenario='" + ps.scenario + "' and \n\
    mu.year_run=" + str(year - 1) + " and \n\
    -- next two lines find the exporting activity that used (or importing commodity that made) \
    the commodity we measured in the inner query \n\
    mu.activity like '" + activitiesToUpdate + "' and  \n\
    mu.commodity=im.commodity \n\
    -- next three lines specify the values we are updating, i.e. the appropriate activity/scneario/year combination \n\
    and mu.activity=input.activity_totals.activity \n\
    and input.activity_totals.year_run=" + str(year) + " \n\
    and input.activity_totals.scenario='" + ps.scenario + "';"
    logging.debug("Updating " + activitiesToUpdate + " with \n" + sqlstr)
    # if ps.sql_system == ps.postgres:
    # Will be using postgresSQL irrespective of the sql_system
    retcode = executePostgreSQLQuery(sqlstr, ps.mapit_database, ps.mapit_port, ps.mapit_host, ps.pguser)
    logResultsFromExternalProgram(None, "Failed in updating " + activitiesToUpdate, (retcode,))

def updateImporters(year):
    updateImportersOrExporters(year, ps.exporter_string, ps.importer_string, 'U')
def updateExporters(year):
    updateImportersOrExporters(year, ps.importer_string, ps.exporter_string, 'M')

def loadDistances(skimFileName, skimyear):
    query = "truncate table " + ps.sd_schema + ".distances"
    if ps.sql_system == ps.postgres:
        retcode = executePostgreSQLQuery(query, ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
    elif ps.sql_system == ps.sqlserver:
        retcode = executeSQLServerQuery(query, ps.sd_database, ps.sd_user, ps.sd_password)
    else:
        logging.error("Invalid database system: " + ps.sql_system)
        raise ValueError
    logResultsFromExternalProgram("Deleted old distance data from database", 
                                  "Problem deleting old distance data from database", (retcode,))
    skimFile = open(ps.scendir + str(skimyear) + "/" + skimFileName + ".csv", "r")
    skimFileReader = csv.reader(skimFile)
    header = skimFileReader.next()
    query = "insert into " + ps.sd_schema + ".distances (origin_luz, destination_luz, distance) values "
    first = True
    counter = 0
    for row in skimFileReader:
        if not first:
            query += ","
        first = False
        origin = row[header.index("Origin")]
        destination = row[header.index("Destination")]
        distance = float(row[header.index(ps.distance_column)])
        if distance == 0:
            distance = 1E99
        query += "(" + str(origin) + "," + str(destination) + "," + str(distance) + ")"
        counter = counter + 1
        if (counter >= 500):
            query += ";"
            # print query
            if ps.sql_system == ps.postgres:
                executePostgreSQLQuery(query, ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
            elif ps.sql_system == ps.sqlserver:
                executeSQLServerQuery(query, ps.sd_database, ps.sd_user, ps.sd_password)
            else:
                logging.error("Invalid database system: " + ps.sql_system)
                raise ValueError
            query = "insert into " + ps.sd_schema + ".distances  (origin_luz, destination_luz, distance) values "
            first = True
            counter = 0
    if not first:
        query += ";"
        # print query
        if ps.sql_system == ps.postgres:
            executePostgreSQLQuery(query, ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
        elif ps.sql_system == ps.sqlserver:
            executeSQLServerQuery(query, ps.sd_database, ps.sd_user, ps.sd_password)
        else:
            logging.error("Invalid database system: " + ps.sql_system)
            raise ValueError

def loadExchangeResults(year):
    query = "truncate table " + ps.sd_schema + ".exchange_results"
    if ps.sql_system == ps.postgres:
        retcode = executePostgreSQLQuery(query, ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
    elif ps.sql_system == ps.sqlserver:
        retcode = executeSQLServerQuery(query, ps.sd_database, ps.sd_user, ps.sd_password)
    else:
        logging.error("Invalid database system: " + ps.sql_system)
        raise ValueError
    logResultsFromExternalProgram(None, "Problem deleting old exchange results from database", (retcode,))
    exresults = open(ps.scendir + str(year) + "/ExchangeResults.csv", "r")
    exresultsReader = csv.reader(exresults)
    header = exresultsReader.next()
    query = "insert into " + ps.sd_schema + ".exchange_results (yr, commodity, luz, price, internal_bought) values "
    first = True
    counter = 0
    for row in exresultsReader:
        if not first:
            query += ","
        first = False
        commodity = row[header.index("Commodity")]
        luz = row[header.index("ZoneNumber")]
        price = row[header.index("Price")]
        internal_bought = row[header.index("InternalBought")]
        query += "(" + str(year) + ",'" + commodity + "'," + luz + "," + price + "," + internal_bought + ")"
        counter = counter + 1
        if (counter >= 250):
            query += ";"
            # print query
            if ps.sql_system == ps.postgres:
                executePostgreSQLQuery(query, ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
            elif ps.sql_system == ps.sqlserver:
                executeSQLServerQuery(query, ps.sd_database, ps.sd_user, ps.sd_password)
            else:
                logging.error("Invalid database system: " + ps.sql_system)
                raise ValueError
            query = "insert into " + ps.sd_schema + ".exchange_results  \
                    (yr, commodity, luz, price, internal_bought) values "
            first = True
            counter = 0
    if not first:
        query += ";"
        # print query
        if ps.sql_system == ps.postgres:
            executePostgreSQLQuery(query, ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
        elif ps.sql_system == ps.sqlserver:
            executeSQLServerQuery(query, ps.sd_database, ps.sd_user, ps.sd_password)
        else:
            logging.error("Invalid database system: " + ps.sql_system)
            raise ValueError

def smoothPrices():
    logging.info("Applying price smoothing")
    query = "update " + ps.sd_schema + ".exchange_results\n\
    set price=new_price from\n\
    ( select commodity, origin_luz, sum(internal_bought) as total_bought, \
    sum(internal_bought * price * power(distance, " + str(ps.gravity_exponent) + \
    ")) / sum(internal_bought * power (distance, " + str(ps.gravity_exponent) + ")) as new_price\n\
    from " + ps.sd_schema + ".exchange_results\n\
    inner join " + ps.sd_schema + ".space_to_commodity\n\
    on exchange_results.commodity=space_to_commodity.aa_commodity\n\
    inner join " + ps.sd_schema + ".distances\n\
    on exchange_results.luz=distances.destination_luz\n\
    group by commodity, origin_luz\n\
    ) new_prices\n\
    where exchange_results.commodity=new_prices.commodity\n\
    and exchange_results.luz=new_prices.origin_luz\n\
    and total_bought > 0"
    if ps.sql_system == ps.postgres:
        executePostgreSQLQuery(query, ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
    elif ps.sql_system == ps.sqlserver:
        executeSQLServerQuery(query, ps.sd_database, ps.sd_user, ps.sd_password)
    else:
        logging.error("Invalid database system: " + ps.sql_system)
        raise ValueError

def smoothPricesSandag(year):
    logging.info("Applying price smoothing")
    query = "EXECUTE dbo.smooth_prices_sr13 @yr = %s, @sd_schema= '%s'" % (year, ps.sd_schema)
    if ps.sql_system == ps.postgres:
        executePostgreSQLQuery(query, ps.sd_database, ps.sd_host, ps.sd_port, ps.pguser)
    elif ps.sql_system == ps.sqlserver:
        executeSQLServerQuery(query, ps.sd_database, ps.sd_user, ps.sd_password)
    else:
        logging.error("Invalid database system: " + ps.sql_system)
        raise ValueError
"""
def writeActivityTotals(year, pgcmd):
    sqlstr = "\\copy (select activity as \"Activity\", total_amount as \"TotalAmount\" from input.activity_totals \
        where year_run="+str(year)+" and scenario='"+ps.scenario+"') to '"+ ps.EXACTPATH + str(year) +"/ActivityTotalsI.csv' csv header"
    retcode = subprocess.check_call([pgcmd, "-c",sqlstr, "--host="+ps.mapit_host, "--port="+str(ps.mapit_port) , "--dbname="+ps.mapit_database, "--username="+ps.pguser])
    logResultsFromExternalProgram(None, "Problem updating ActivityTotalsI in year "+str(year), (retcode,))
"""
class excelOne(csv.excel):
    # define CSV dialect for Excel to avoid blank lines from default \r\n
    lineterminator = "\n"

def loadDevelopmentEvents(year):
    logging.info("Loading development events for year:" + str(year))
    if ps.sql_system == ps.postgres:
        folder_path = os.path.join(ps.scendir, str(year))
        folder_path = folder_path.replace('\\', '/')
        csv_file = os.path.join(folder_path, "developmentEvents.csv")
        conn = psycopg2.connect(database = ps.sd_database, host = ps.sd_host, port = ps.sd_port, 
                                user = ps.pguser, password = ps.pgpassword)
        cur = conn.cursor()
        cur.execute('TRUNCATE %s.%s' % (ps.sd_schema, "development_events"))  # empty the temporary table
        f = open(csv_file, 'r')  # open the AA output file
        f.readline()  # skip the first line for header
        # use the psycopg2 fast copy command to copy the data into the temporary table
        cur.copy_from(f, "%s.%s" % (ps.sd_schema, "development_events"), sep = ',', null = '')  
        cur.execute('SELECT count(*) FROM %s.%s;' % (ps.sd_schema, "development_events"))
        counter = cur.fetchone()[0]
        logging.info("Loaded %s development events from file %s" % (counter, str(csv_file)))
        conn.commit()
        conn.close()
    elif ps.sql_system == ps.sqlserver:
        # TODO Add logging with pyodbc or _mssql
        folder_path = os.path.join(ps.DEVEVENTPATH)
        folder_path = folder_path.replace('/', '\\')
        csv_file = os.path.join(folder_path, "developmentEvents" + str(year) + ".csv")
        # csv_file = os.path.join(folder_path, "developmentEvents.csv")
        query = 'TRUNCATE TABLE %s.%s' % (ps.sd_schema, "development_events")
        executeSQLServerQuery(query, ps.sd_database, ps.sd_user, ps.sd_password)
        f = open(csv_file, 'r')  # open the AA output file
        f.readline()  # skip the first line for header
        query = "BULK INSERT %s.%s FROM " % (ps.sd_schema, "development_events") + "'" + csv_file \
            + "' WITH (FIELDTERMINATOR = ',',ROWTERMINATOR = '0x0a', FIRSTROW = 2)"
        executeSQLServerQuery(query, ps.sd_database, ps.sd_user, ps.sd_password)
        logging.info("Loaded development events for year:" + str(year))
    else:
        logging.error("Invalid database system: " + ps.sql_system)
        raise ValueError

def replayDevelopmentEvents():
    logging.info("Replaying development events")
    sqlstr1 = "UPDATE " + ps.sd_schema \
                    + '''.parcels SET        space_quantity   = development_events.new_space_quantity,
                       space_type_id    = development_events.new_space_type_id,
                       year_built       = development_events.new_year_built,
                       land_area        = development_events.land_area,
                       is_derelict      = development_events.new_is_derelict,
                      is_brownfield    = development_events.new_is_brownfield
                FROM ''' + ps.sd_schema + '''.development_events
                WHERE parcels.pecas_parcel_num = development_events.original_pecas_parcel_num
                           AND (development_events.event_type = 'C' OR
                                        development_events.event_type = 'R' OR
                                        development_events.event_type = 'D' OR
                           development_events.event_type = 'A' OR
                           development_events.event_type = 'L' OR
                           development_events.event_type = 'US' );'''




    sqlstr2 = "INSERT INTO " + ps.sd_schema + '''.parcels
                        SELECT  parcel_id,
                                new_pecas_parcel_num,
                                new_year_built,
                                taz,
                                new_space_type_id,
                                new_space_quantity,
                                land_area,
                                available_services,
                                new_is_derelict,
                                new_is_brownfield
                        FROM ''' + ps.sd_schema + '''.development_events
                        WHERE
                             development_events.event_type = 'CS'
                        OR   development_events.event_type = 'AS'
                        OR   development_events.event_type = 'RS'
                        OR   development_events.event_type = 'DS'
                        OR   development_events.event_type = 'LS'
                        ; '''

    sqlstr3 = "INSERT INTO " + ps.sd_schema + '''.parcel_cost_xref
                 SELECT development_events.new_pecas_parcel_num, parcel_cost_xref.cost_schedule_id, parcel_cost_xref.year_effective
                 FROM ''' + ps.sd_schema + '''.parcel_cost_xref, ''' + ps.sd_schema + '''.development_events
                 WHERE parcel_cost_xref.pecas_parcel_num=development_events.original_pecas_parcel_num
                 AND (
                                 event_type = 'CS'
                        OR   event_type = 'AS'
                        OR   event_type = 'RS'
                        OR   event_type = 'DS'
                        OR   event_type = 'LS'
                        ); '''

    sqlstr4 = "INSERT INTO " + ps.sd_schema + '''.parcel_fee_xref
                 SELECT development_events.new_pecas_parcel_num, parcel_fee_xref.fee_schedule_id, parcel_fee_xref.year_effective
                 FROM ''' + ps.sd_schema + '''.parcel_fee_xref, ''' + ps.sd_schema + '''.development_events
                 WHERE parcel_fee_xref.pecas_parcel_num=development_events.original_pecas_parcel_num
                 AND (
                                 event_type = 'CS'
                        OR   event_type = 'AS'
                        OR   event_type = 'RS'
                        OR   event_type = 'DS'
                        OR   event_type = 'LS'
                        ); '''
    sqlstr5 = '''INSERT INTO ''' + ps.sd_schema + '''.parcel_zoning_xref
                 SELECT development_events.new_pecas_parcel_num, parcel_zoning_xref.zoning_rules_code, parcel_zoning_xref.year_effective
                 FROM ''' + ps.sd_schema + '''.parcel_zoning_xref, ''' + ps.sd_schema + '''.development_events
                 WHERE parcel_zoning_xref.pecas_parcel_num=development_events.original_pecas_parcel_num
                         AND (
                                 event_type = 'CS'
                                OR   event_type = 'AS'
                                OR   event_type = 'RS'
                                OR   event_type = 'DS'
                                OR   event_type = 'LS'
                                );'''
    sqlstr6 = '''INSERT INTO ''' + ps.sd_schema + '''.local_effect_distances
SELECT development_events.new_pecas_parcel_num,
local_effect_distances.local_effect_id,
local_effect_distances.local_effect_distance,
local_effect_distances.year_effective
FROM ''' + ps.sd_schema + '''.local_effect_distances, ''' + ps.sd_schema + '''.development_events
WHERE
local_effect_distances.pecas_parcel_num=development_events.original_pecas_parcel_num
AND
(        event_type = 'CS'
        OR   event_type = 'AS'
                OR   event_type = 'RS'
                        OR   event_type = 'DS'
                        OR   event_type = 'LS'
 );'''

    if ps.sql_system == ps.postgres:
        retcode1 = executePostgreSQLQuery("SET search_path=" + ps.sd_schema + "; " + sqlstr1, 
                                          ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
        retcode2 = executePostgreSQLQuery("SET search_path=" + ps.sd_schema + "; " + sqlstr2, 
                                          ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
        retcode3 = executePostgreSQLQuery("SET search_path=" + ps.sd_schema + "; " + sqlstr3, 
                                          ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
        retcode4 = executePostgreSQLQuery("SET search_path=" + ps.sd_schema + "; " + sqlstr4, 
                                          ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
        retcode5 = executePostgreSQLQuery("SET search_path=" + ps.sd_schema + "; " + sqlstr5, 
                                          ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
        retcode6 = executePostgreSQLQuery("SET search_path=" + ps.sd_schema + "; " + sqlstr6, 
                                          ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
    elif ps.sql_system == ps.sqlserver:
        retcode1 = executeSQLServerQuery(sqlstr1, ps.sd_database, ps.sd_user, ps.sd_password)
        retcode2 = executeSQLServerQuery(sqlstr2, ps.sd_database, ps.sd_user, ps.sd_password)
        retcode3 = executeSQLServerQuery(sqlstr3, ps.sd_database, ps.sd_user, ps.sd_password)
        retcode4 = executeSQLServerQuery(sqlstr4, ps.sd_database, ps.sd_user, ps.sd_password)
        retcode5 = executeSQLServerQuery(sqlstr5, ps.sd_database, ps.sd_user, ps.sd_password)
        retcode6 = executeSQLServerQuery(sqlstr6, ps.sd_database, ps.sd_user, ps.sd_password)
    else:
        logging.error("Invalid database system: " + ps.sql_system)
        raise ValueError
    logResultsFromExternalProgram("Replayed development events into parcels table", 
                                  "Problem replaying development events into parcels table", 
                                  (retcode1, retcode2, retcode3, retcode4, retcode5, retcode6))


def insertDevelopmentEventsIntoHistory(year):
    logging.info("Start Inserting Development Events to History" + str(year))
    if year == ps.baseyear:
        if ps.sql_system == ps.postgres:
            query = "SET search_path=" + ps.sd_schema + "; TRUNCATE development_events_history; \
                    INSERT INTO development_events_history (select " + str(year) \
                    + " as year_run,* from development_events);"
            retcode = executePostgreSQLQuery(query, ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
            logging.info("Completed Inserting Development Events to History: " + str(year))
        elif ps.sql_system == ps.sqlserver:
            query = "TRUNCATE table development_events_history; \
            INSERT INTO development_events_history \
            (year_run, \
            event_type, parcel_id, original_pecas_parcel_num, new_pecas_parcel_num, available_services, \
            old_space_type_id, new_space_type_id, old_space_quantity, new_space_quantity, \
            old_year_built, new_year_built, land_area,\
            old_is_derelict, new_is_derelict, \
            old_is_brownfield, new_is_brownfield, \
            zoning_rules_code, taz) (select " + str(year) + " as year_run,* from development_events);"
            retcode = executeSQLServerQuery(query, ps.sd_database, ps.sd_user, ps.sd_password)
            logging.info("Completed Inserting Development Events to History: " + str(year))
        else:
            logging.error("Invalid database system: " + ps.sql_system)
            raise ValueError
    else:
        if ps.sql_system == ps.postgres:
            query = "SET search_path=" + ps.sd_schema + "; INSERT INTO development_events_history (select " \
                + str(year) + " as year_run,* from development_events);"
            retcode = executePostgreSQLQuery(query, ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
            logging.info("Completed Inserting Development Events to History: " + str(year))
        elif ps.sql_system == ps.sqlserver:
            query = "INSERT INTO development_events_history \
            (year_run, \
            event_type, parcel_id, original_pecas_parcel_num, new_pecas_parcel_num, available_services, \
            old_space_type_id, new_space_type_id, old_space_quantity, new_space_quantity, \
            old_year_built, new_year_built, land_area,\
            old_is_derelict, new_is_derelict, \
            old_is_brownfield, new_is_brownfield, \
            zoning_rules_code, taz) (select " + str(year) + " as year_run,* from development_events);"
            retcode = executeSQLServerQuery(query, ps.sd_database, ps.sd_user, ps.sd_password)
            logging.info("Completed Inserting Development Events to History: " + str(year))
        else:
            logging.error("Invalid database system: " + ps.sql_system)
            raise ValueError
    logResultsFromExternalProgram(None, "Problem updating development events history for " + str(year), (retcode,))

def writeFloorspaceSummaryFromParcelFile(sd_output_year):
    # TODO  pyscopg2 version for if database isn't on same server
    if ps.sql_system == ps.postgres:
        sqlstr = "\\copy (select * from " + ps.sd_schema + ".floorspacei_view) to '" \
            + ps.EXACTPATH + str(sd_output_year) + "/FloorspaceI.csv' csv header"
        print sqlstr
        retcode = executePostgreSQLQuery(sqlstr, ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
        logResultsFromExternalProgram("Wrote FloorspaceI from Parcel File for use in year " + str(sd_output_year), 
                                      "Problem writing FloorspaceI from Parcel File for use in year " \
                                      + str(sd_output_year), (retcode,))
    elif ps.sql_system == ps.sqlserver:
        folder_path = os.path.join(ps.EXACTPATH, str(sd_output_year))
        file_path = os.path.join(folder_path, 'FloorspaceI.csv')
        file_path = file_path.replace('/', '\\')
        sqlstr = "select 'TAZ' as taz, 'Commodity' as commodity, 'Quantity' as quantity union all select \
            cast(TAZ as nvarchar), cast(Commodity as nvarchar(50)), cast(Quantity as nvarchar) from " \
            + ps.sd_database + "." + ps.sd_schema + ".floorspacei_view"
        subprocess.call(["bcp", sqlstr, "queryout", file_path, "-S", ps.sd_host, "-T", "-c", "-t,"])
    else:
        logging.error("Invalid database system: " + ps.sql_system)
        raise ValueError

def resetParcelDatabase():
    """ delete the parcels file and reload from backup """

    logging.info("resetting parcel database in database " + ps.sd_database + " and schema " + ps.sd_schema)
    query1 = "DELETE from local_effect_distances WHERE pecas_parcel_num >(SELECT MAX(pecas_parcel_num) \
        FROM parcels_backup);"
    query2 = "DELETE from parcel_cost_xref WHERE pecas_parcel_num >(SELECT MAX(pecas_parcel_num) FROM parcels_backup);"
    query3 = "DELETE from parcel_fee_xref WHERE pecas_parcel_num >(SELECT MAX(pecas_parcel_num) FROM parcels_backup);"
    query4 = "DELETE from parcel_zoning_xref WHERE pecas_parcel_num > \
        (SELECT MAX(pecas_parcel_num) FROM parcels_backup);"
    query5 = "DELETE from parcels WHERE pecas_parcel_num >(SELECT MAX(pecas_parcel_num) FROM parcels_backup);"
    query5A = "VACUUM FULL " + ps.sd_schema + ".parcels;"
    query6 = "UPDATE parcels  SET year_built= parcels_backup.year_built, space_type_id=parcels_backup.space_type_id, \
        space_quantity=parcels_backup.space_quantity, land_area=parcels_backup.land_area, \
        available_services_code=parcels_backup.available_services_code, is_derelict=parcels_backup.is_derelict, \
        is_brownfield=parcels_backup.is_brownfield FROM parcels_backup  WHERE parcels.pecas_parcel_num = \
        parcels_backup.pecas_parcel_num;"
    query7 = "TRUNCATE TABLE " + ps.sd_schema + ".parcels_snapshot"

    retcode5A = 0
    if ps.sql_system == ps.postgres:
        retcode1 = executePostgreSQLQuery("SET search_path=" + ps.sd_schema + "; " + query1, 
                                          ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
        retcode2 = executePostgreSQLQuery("SET search_path=" + ps.sd_schema + "; " + query2, 
                                          ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
        retcode3 = executePostgreSQLQuery("SET search_path=" + ps.sd_schema + "; " + query3, 
                                          ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
        retcode4 = executePostgreSQLQuery("SET search_path=" + ps.sd_schema + "; " + query4,
                                          ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
        retcode5 = executePostgreSQLQuery("SET search_path=" + ps.sd_schema + "; " + query5, 
                                          ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
        retcode5A = executePostgreSQLQuery(query5A, ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
        retcode6 = executePostgreSQLQuery("SET search_path=" + ps.sd_schema + "; " + query6, 
                                          ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
        retcode7 = executePostgreSQLQuery("SET search_path=" + ps.sd_schema + "; " + query7, 
                                          ps.sd_database, ps.sd_port, ps.sd_host, ps.pguser)
        logResultsFromExternalProgram("Reset parcels", "Problem resetting parcels", 
                                      (retcode1, retcode2, retcode3, retcode4, retcode5, retcode5A, retcode6, retcode7))
    elif ps.sql_system == ps.sqlserver:
        retcode1 = executeSQLServerQuery(query1, ps.sd_database, ps.sd_user, ps.sd_password)
        retcode2 = executeSQLServerQuery(query2, ps.sd_database, ps.sd_user, ps.sd_password)
        retcode3 = executeSQLServerQuery(query3, ps.sd_database, ps.sd_user, ps.sd_password)
        retcode4 = executeSQLServerQuery(query4, ps.sd_database, ps.sd_user, ps.sd_password)
        retcode5 = executeSQLServerQuery(query5, ps.sd_database, ps.sd_user, ps.sd_password)
        retcode6 = executeSQLServerQuery(query6, ps.sd_database, ps.sd_user, ps.sd_password)
        retcode7 = executeSQLServerQuery(query7, ps.sd_database, ps.sd_user, ps.sd_password)
        logResultsFromExternalProgram("Reset parcels", "Problem resetting parcels", 
                                      (retcode1, retcode2, retcode3, retcode4, retcode5, retcode6, retcode7))
    else:
        logging.error("Invalid database system: " + ps.sql_system)
        raise ValueError



def getSkimFileForYear(year):
    """ Get the skim file name."""
    return "SkimsI"


def getSkimYearForYear(year):
    """ Get the year whose skims should be used for the current year."""
    return year
"""
def loadAATotals():
    logging.info("Loading Activity Totals for scenario:" + ps.scenario)
    folder_path = os.path.join(ps.scendir, 'AllYears/Inputs')
    folder_path = folder_path.replace('\\', '/')
    conn = psycopg2.connect(database=ps.mapit_database, host=ps.mapit_host, port=ps.mapit_port, user=ps.pguser,  password=ps.pgpassword )
    cur = conn.cursor()
    cur.execute('TRUNCATE %s.%s' % ('input', "activity_totals_temp")) # empty the temporary table
    csv_file = os.path.join(folder_path, "All_ActivityTotalsI.csv")
    f = open(csv_file, 'r') # open the AA input file
    f.readline() # skip the first line for header
    cur.copy_from(f, "%s.%s" %('input', "activity_totals_temp"), sep=',', null='') # use the psycopg2 fast copy command to copy the data into the temporary table
    cur.execute("DELETE FROM input.activity_totals WHERE scenario= '%s';" %(ps.scenario))
    cur.execute("INSERT INTO input.activity_totals (select year_run, '%s', activity, total_amount from input.activity_totals_temp);" %(ps.scenario))
    cur.execute('SELECT count(*) FROM %s.%s;' %('input', "activity_totals_temp"))
    counter = cur.fetchone()[0]
    logging.info("Loaded %s activity totals from file %s" % (counter,str(csv_file)))
    conn.commit()
    conn.close()
"""

def runTravelModelForYear(year):
    skimfile = str(year) + "Skims"
    skimyear = year
     # specifies where to find the network files for the travel model, this demo always uses the 2007 network 
     # but in general the network closest in time to the current year should be used.
    networkyear = 2007 
    # if the travel model does not use different file system directories to represent different 
    # scenarios there would be additional code to select the appropriate scenario as well as the appropriate year

    #------------------------------------------------------------------------------------------------------------------#
    # 7) update land use inputs to travel model
    # NOTE there is no code in this section because in the simple pecas assignment process the trips
    # are generated directly by the pecas AA module, in Atlanta we plan to improve this section
    # iteratively,
    # first, just pick appropriate pre-defined land use inputs for the year,
    # second, scale the pre-defined land use inputs based on TAZ growth rates from ActivityLocations2, and
    # third, do something more sophisticated with labor categories, jobs, income, hhsize, etc.

    #------------------------------------------------------------------------------------------------------------------#
    # 8) Run travel model with appropriately chosen network

    # our travel model is a simple frank-wolfe assignment (trip generation is actually done within the 
    # AA code for software performance reasons)
    retcode = subprocess.call([javaRunCommand, "-Xmx3000M", "-Dlog4j.configuration=log4j.xml", 
                               "-DNETWORKYEAR=" + str(networkyear),
    "-DSCENDIR=" + str(ps.ps.scendir), "-DYEAR=" + str(year - 1),  # use last year's trip table which AA generated
    "-DSKIMYEAR=" + str(skimyear), "-DSKIMFILE=" + str(skimfile),  # write out this year's skims for AA to use
    "-cp", buildClassPath(ps.scendir + "AllYears/Inputs", classpath), "com.hbaspecto.pecas.assign.PecasAssign"])
    moveReplace(ps.scendir + "/event.log", ps.scendir + "/" + str(year) + "/assign-event.log")


    #------------------------------------------------------------------------------------------------------------------#
    # 9) generate new skims and set them up for AA to use

    # assignment program already created a skim.csv file, all we need to do is append the external zone skims.
    fin = open(ps.scendir + "AllYears/Inputs/Externalskims.csv", "r")
    # open in "append" mode, so we can append the external skims
    fout = open(ps.scendir + "/" + str(year) + "/" + str(year) + "skim.csv", "a")  
    inFile = csv.reader(fin, excelOne)
    outFile = csv.writer(fout, excelOne)

    for row in inFile:
        outFile.writerow(row)

    fin.close()
    fout.close()

class ExternalProgramError(Exception):
    def __init__(self, value):
        self.value = value
    def __str__(self):
        return repr(self.value)

def logResultsFromExternalProgram(okmsg, notokmsg, resultsArray):
    ok = True
    for result in resultsArray:
        if result != 0:
            ok = False
    if ok:
        if okmsg != None:
            logging.info(okmsg)
    else:
        logging.error(notokmsg + ", return codes " + str(resultsArray))
        raise ExternalProgramError(notokmsg)

def prepareTravelModelInputs(year, scenario):
    logging.info("Executing query: select input.generate_tm_inputs(" + str(year) + ",'" + scenario + "');")
    retcode = executePostgreSQLQuery("select input.generate_tm_inputs(" + str(year) + ",'" + scenario + "');",
                                      ps.mapit_database, ps.mapit_port, ps.mapit_host, ps.pguser)
    logResultsFromExternalProgram(None, "Problem preparing table of travel model inputs in database", (retcode,))

def prepareTravelModelInputsSANDAG(year):
    # load the data into sql server
    logging.info("Executing query: travel.tm_generate_4step_inputs " + str(year))
    sql = "EXEC travel.tm_generate_4step_inputs '%s', %s, '%s', %s" % (os.path.abspath(ps.local_unc_path) \
                                                                       + os.sep, ps.aa_scenario_id, ps.sd_schema, year)
    retcode = executeSQLServerQuery(sql, ps.aa_database, ps.sd_user, ps.sd_password)
    logResultsFromExternalProgram(None, "Problem preparing table of travel model inputs in database", (retcode,))

    # call stored procedures to format the data and transfer to CSVs
    filepath1 = os.path.join(ps.tm_input_dir, "pasef" + str(year) + ".csv")
    filepath2 = os.path.join(ps.tm_input_dir, "ludata" + str(year) + ".csv")
    filepath3 = os.path.join(ps.tm_input_dir, "hhdata" + str(year) + ".csv")
    filepath4 = os.path.join(ps.tm_input_dir, "emp" + str(year) + ".csv")
    sqlstr1 = "EXEC travel.tm_pasef %s, %s" % (ps.aa_scenario_id, year)
    sqlstr2 = "EXEC travel.tm_ludata %s, %s" % (ps.aa_scenario_id, year)
    sqlstr3 = "EXEC travel.tm_hhdata %s, %s" % (ps.aa_scenario_id, year)
    sqlstr4 = "EXEC travel.tm_emp %s, %s" % (ps.aa_scenario_id, year)

    logging.info("BCP tm_pasef for " + str(year))
    retcode = subprocess.call(["bcp", sqlstr1, "queryout", filepath1, "-S", ps.sd_host,
                                "-d", "pecas_sr13", "-U", ps.sd_user, "-P", ps.sd_password, "-c", "-t,"])
    logResultsFromExternalProgram(None, "Problem preparing tm_pasef travel model inputs in database", (retcode,))

    logging.info("BCP tm_ludata for " + str(year))
    retcode = subprocess.call(["bcp", sqlstr2, "queryout", filepath2, "-S", ps.sd_host, 
                               "-d", "pecas_sr13", "-U", ps.sd_user, "-P", ps.sd_password, "-c", "-t,"])
    logResultsFromExternalProgram(None, "Problem preparing tm_ludata travel model inputs in database", (retcode,))

    logging.info("BCP tm_hhdata for " + str(year))
    retcode = subprocess.call(["bcp", sqlstr3, "queryout", filepath3, "-S", ps.sd_host, 
                               "-d", "pecas_sr13", "-U", ps.sd_user, "-P", ps.sd_password, "-c", "-t,"])
    logResultsFromExternalProgram(None, "Problem preparing tm_hhdata travel model inputs in database", (retcode,))

    logging.info("BCP tm_emp for " + str(year))
    retcode = subprocess.call(["bcp", sqlstr4, "queryout", filepath4, "-S", ps.sd_host, 
                               "-d", "pecas_sr13", "-U", ps.sd_user, "-P", ps.sd_password, "-c", "-t,"])
    logResultsFromExternalProgram(None, "Problem preparing tm_hhdata travel model inputs in database", (retcode,))

    # Get final values to use in travel model (integers that sum to proper total).
    # Order is important because PASEF relies on HH totals
    logging.info("Running update_tm_inputs.py")
    update_tm_inputs.roundEmp(filepath4)
    update_tm_inputs.roundHHData(filepath3)
    update_tm_inputs.roundLUData(filepath2, ps.aa_scenario_id, year)
    update_tm_inputs.roundPASEF(filepath1, filepath3)

def prepareTravelModelOutputsSANDAG(year):
    """use skims from previous year travel model to load to current year for PECAS """
    logging.info("Loading TM skims into SQL Server database")
    sqlstr1 = "EXEC aa.load_skims '%s', %s, %s" % (os.path.abspath(ps.tm_output_dir) + os.sep, 
                                                   ps.aa_scenario_id, str(year - 1))
    retcode = executeSQLServerQuery(sqlstr1, "pecas_sr13", ps.sd_user, ps.sd_password)
    logResultsFromExternalProgram(None, "Problem preparing loading skims into database", (retcode,))

    # dump them to csv with bcp
    logging.info("BCP skims " + str(year))
    filepath1 = os.path.join(ps.scendir, str(year), "SkimsI.csv")
    sqlstr1 = "EXEC aa.SkimsI %s, %s" % (ps.aa_scenario_id, str(year))
    retcode = subprocess.call(["bcp", sqlstr1, "queryout", filepath1, "-S", ps.sd_host, 
                               "-d", "pecas_sr13", "-U", ps.sd_user, "-P", ps.sd_password, "-c", "-t,"])
    logResultsFromExternalProgram(None, "Problem preparing loading skims into database", (retcode,))

def applySiteSpecSANDAG(year):
    """
    Execute dbo.sitespec procedure in pecas_sd_run. Future year zoning is already in the parcel_zoning_xref table
    so no need to update here.
    """
    sql = "EXEC dbo.sitespec '%s', %s" % (ps.sd_schema, str(year))
    retcode = executeSQLServerQuery(sql, ps.sd_database, ps.sd_user, ps.sd_password)
    logResultsFromExternalProgram(None, "Problem applying sitespec", (retcode,))

def parcelSnapshot(year):
    """
    Backup parcels every 5 years. This is run after SD, so it inserts year+1 to show
    the parcels as seen by AA in the given year
    """
    logging.info("loading " + ps.sd_schema + ".parcel_snapshot after SD run in " + str(year))
    sql = "INSERT INTO " + ps.sd_schema + ".[parcels_snapshot] \n\
           ([yr],[parcel_id],[pecas_parcel_num],[year_built],[taz],[space_type_id] \n\
           ,[space_quantity],[land_area],[available_services_code],[is_derelict],[is_brownfield]) \n\
    SELECT " + str(year + 1) + ",[parcel_id],[pecas_parcel_num],[year_built],[taz],[space_type_id] \n\
           ,[space_quantity],[land_area],[available_services_code],[is_derelict],[is_brownfield] \n\
    FROM " + ps.sd_schema + ".parcels"
    retcode = executeSQLServerQuery(sql, ps.sd_database, ps.sd_user, ps.sd_password)
    logResultsFromExternalProgram(None, "Problem taking parcel snapshot", (retcode,))
