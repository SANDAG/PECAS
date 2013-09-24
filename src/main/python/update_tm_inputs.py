import os
import random
import csv
import numpy as np
import pyodbc

random.seed(1)
np.seterr(divide = 'ignore')

# define database connection
pecas_sr13 = {
    'Driver' : 'SQL Server',
    'Server' : 'pele.sandag.org',
    'Database' : 'pecas_sr13',
    'Trusted_Connection' : 'yes'
}

def weighted_choice(weights):
    rnd = random.random() * sum(weights)
    for i, w in enumerate(weights):
        rnd -= w
        if rnd < 0:
            return i

def roundEmp(filename):
    head = []
    header = {}
    empdata = []
    cat = ['emp_agmin', 'emp_cons', 'emp_mfg', 'emp_whtrade', 'emp_retrade', 'emp_twu', 'emp_fre', 
           'emp_info', 'emp_pbs', 'emp_lh', 'emp_os', 'emp_edhs', 'emp_gov', 'emp_sedw']

    with open(filename, 'rU') as fi:
        freader = csv.reader(fi)
        head = freader.next()
        header = dict((k, v) for (v, k) in enumerate(head))

        for row in freader:
            row = [int(row[0])] + [float(x) for x in row[1:]]
            empdata.append(row)

    emparray = np.asarray(empdata)

    # round civ and mil emp to get new total
    emparray[:, map(lambda x: header[x], ['emp_civ', 'emp_mil'])] = np.around(emparray[:, map(lambda x: header[x], ['emp_civ', 'emp_mil'])], 0)
    emparray[:, header['emp']] = np.sum(emparray[:, map(lambda x: header[x], ['emp_civ', 'emp_mil'])], 1)

    # assign emp proportionally
    emparray[:, map(lambda x: header[x], cat)] = emparray[:, map(lambda x: header[x], cat)] / emparray[:, map(lambda x: header[x], cat)].sum(axis = 1)[:, np.newaxis]
    emparray[np.isnan(emparray), :] = 0

    catarray = emparray[:, map(lambda x: header[x], cat)]
    emparray[:, map(lambda x: header[x], cat)] = np.around(emparray[:, map(lambda x: header[x], cat)] * emparray[:, header["emp_civ"]][:, np.newaxis], 0)

    # loop through rows to round emp categories to emp_civ totals using weighted choice
    diff = emparray[:, header["emp_civ"]] - np.sum(emparray[:, map(lambda x: header[x], cat)], 1).tolist()
    empcat = emparray[:, map(lambda x: header[x], cat)].tolist()
    for i, row in enumerate(empcat):
        d = diff[i]
        while d != 0:
            c = weighted_choice(catarray[i, :])
            if d > 0:
                empcat[i][c] += 1
                d -= 1
            elif empcat[i][c] > 0:
                empcat[i][c] -= 1
                d += 1

    emparray[:, map(lambda x: header[x], cat)] = np.asarray(empcat)

    with open(filename, 'wb') as f:
        writer = csv.writer(f)
        head = sorted(header, key = header.get)
        writer.writerow(head)
        for line in emparray.astype(int):
            writer.writerow(line)

def roundHHData(filename):
    head = []
    header = {}
    hhdata = []
    vac = []
    hh = ['hh_sf', 'hh_mf', 'hh_mh']
    struct = ['hs_sf', 'hs_mf', 'hs_mh']
    inc = ['i1', 'i2', 'i3', 'i4', 'i5', 'i6', 'i7', 'i8', 'i9', 'i10']

    with open(filename, 'rU') as fi:
        freader = csv.reader(fi)
        head = freader.next()
        header = dict((k, v) for (v, k) in enumerate(head))

        for row in freader:
            row = [int(row[0])] + [float(x) for x in row[1:]]
            hhdata.append(row)

    hharray = np.asarray(hhdata)

    # save original mgra vacancy rates (numpy puts NaN for division by 0)
    vac = hharray[:, map(lambda x: header[x], hh)] / hharray[:, map(lambda x: header[x], struct)]
    vac[np.isnan(vac), :] = 0

    # round hh by structure and reset the hh total
    hharray[:, map(lambda x: header[x], hh)] = np.around(hharray[:, map(lambda x: header[x], hh)], 0)
    hharray[:, header["hh"]] = np.sum(hharray[:, map(lambda x: header[x], hh)], 1)

    # get new hs from the new hh and vac rates
    hharray[:, map(lambda x: header[x], struct)] = np.around(hharray[:, map(lambda x: header[x], hh)] + (1 - vac) * hharray[:, map(lambda x: header[x], hh)], 0)
    hharray[:, header["hs"]] = np.sum(hharray[:, map(lambda x: header[x], struct)], 1)

    # round gq and hhp; update pop
    hharray[:, [header["gq_civ"], header['gq_mil'], header['hhp']]] = np.around(hharray[:, [header['gq_civ'], header['gq_mil'], header['hhp']]], 0)
    hharray[:, [header["pop"]]] = np.sum(hharray[:, [header['gq_civ'], header['gq_mil'], header['hhp']]], 1)[:, np.newaxis]

    # turn income categories into proportions and save to use as weights
    hharray[:, map(lambda x: header[x], inc)] = hharray[:, map(lambda x: header[x], inc)] / hharray[:, map(lambda x: header[x], inc)].sum(axis = 1)[:, np.newaxis]
    hharray[np.isnan(hharray), :] = 0
    incarray = hharray[:, map(lambda x: header[x], inc)]
    hharray[:, map(lambda x: header[x], inc)] = np.around(hharray[:, map(lambda x: header[x], inc)] * hharray[:, header["hh"]][:, np.newaxis], 0)

    # loop through rows to round income to match hh totals using weighted choice
    diff = hharray[:, header["hh"]] - np.sum(hharray[:, map(lambda x: header[x], inc)], 1).tolist()
    hhinc = hharray[:, map(lambda x: header[x], inc)].tolist()
    for i, row in enumerate(hhinc):
        d = diff[i]
        while d != 0:
            c = weighted_choice(incarray[i, :])
            if d > 0:
                hhinc[i][c] += 1
                d -= 1
            elif hhinc[i][c] > 0:
                hhinc[i][c] -= 1
                d += 1

    hharray[:, map(lambda x: header[x], inc)] = np.asarray(hhinc)

    with open(filename, 'wb') as f:
        writer = csv.writer(f)
        head = sorted(header, key = header.get)
        writer.writerow(head)
        for line in hharray.astype(int):
            writer.writerow(line)

def roundLUData(filename, scenario_id, yr):
    """ emp on ludata table sums to 1 so it can be distributed using acres as weights """
    head = []
    header = {}
    ludata = []
    vac = []
    nummgra = 21633
    emptotal = {}
    miladd = []

    with open(filename, 'rU') as fi:
        freader = csv.reader(fi)
        head = freader.next()
        header = dict((k, v) for (v, k) in enumerate(head))

        for row in freader:
            row = [int(row[0])] + [float(x) for x in row[1:]]
            ludata.append(row)

    luarray = np.asarray(ludata)

    conn = pyodbc.connect(**pecas_sr13)
    curs = conn.cursor()

    sql = " \
        SELECT mgra, emp_civ, emp_mil \
        FROM travel.tm_emp_mgra \
        WHERE scenario_id = " + str(scenario_id) + "\
        AND  yr = " + str(yr)

    curs.execute(sql)

    for row in curs.fetchall():
        emptotal[row[0]] = [row[1], row[2]]

    conn.close()

    for mgra in range(1, nummgra + 1):
        if mgra % 500 == 0:
            print mgra
        emp = emptotal[mgra][0]
        milemp = emptotal[mgra][1]
        # if milemp == 0: continue
        filterarray = luarray[:, 0] == mgra
        tmparray = luarray[filterarray, :]

        if emp + milemp == 0:
            luarray[filterarray, header["EMP"]] = 0
            continue

        if sum(tmparray[:, header["EMP"]]) == 0:
            acrewgts = np.copy(tmparray[:, header["ACRES"]])
        else:
            acrewgts = np.copy(tmparray[:, header["EMP"]])

        acrewgts = acrewgts / sum(acrewgts)

        tmparray[:, header["EMP"]] = np.round(tmparray[:, header["EMP"]] * emp, 0)
        diff = emp - sum(tmparray[:, header["EMP"]])

        while diff != 0:
            if diff < 0:
                c = weighted_choice(tmparray[:, header["EMP"]])
                tmparray[c, header["EMP"]] -= 1
                diff += 1
            if diff > 0:
                c = weighted_choice(acrewgts)
                tmparray[c, header["EMP"]] += 1
                diff -= 1

        if milemp > 0:
            milfilter = tmparray[:, header["LU"]] == 6701
            if sum(milfilter) > 0:
                tmparray[milfilter, header["EMP"]] += milemp
            else:
                print mgra
                miladd.append([mgra, 6701, milemp / 4.0, milemp / 4.0, milemp])

        luarray[filterarray, header["EMP"]] = tmparray[:, header["EMP"]]

    with open(filename, 'wb') as f:
        writer = csv.writer(f)
        head = sorted(header, key = header.get)
        writer.writerow(head)
        for line in luarray:
            writer.writerow([int(line[0]), int(line[1]), float(line[2]), float(line[3]), int(line[4])])
        for line in miladd:
            writer.writerow(line)

def roundPASEF(pfn, hhfn):
    mgrahh = {}
    hhheader = {}
    pheader = {}
    pasef = []

    # use rounded hh totals by mgra from roundHHData
    with open(hhfn, 'rb') as fi:
        freader = csv.reader(fi)
        head = freader.next()
        hhheader = dict((k, v) for (v, k) in enumerate(head))
        for line in (map(int, v) for v in freader):
            if mgrahh.has_key(line[hhheader["mgra"]]):
                mgrahh[line[hhheader["mgra"]]] += line[hhheader["pop"]]
            else:
                mgrahh[line[hhheader["mgra"]]] = line[hhheader["pop"]]

    with open(pfn, 'rb') as fi:
        freader = csv.reader(fi)
        head = freader.next()
        pheader = dict((k, v) for (v, k) in enumerate(head))
        cat = sorted(pheader, key = pheader.get)[1:]
        for line in (map(float, v) for v in freader):
            pasef.append(line)

    parray = np.asarray(pasef)
    hharray = np.asarray([mgrahh[k] for k in sorted(mgrahh.iterkeys())])

    # assign emp proportionally
    agecatsarray = np.copy(parray[:, map(lambda x: pheader[x], cat)])
    parray[:, map(lambda x: pheader[x], cat)] = np.around(parray[:, map(lambda x: pheader[x], cat)] * hharray[:, np.newaxis], 0)

    # loop through rows to round emp categories to emp_civ totals using weighted choice
    diff = hharray - np.sum(parray[:, map(lambda x: pheader[x], cat)], 1)
    agecat = parray[:, map(lambda x: pheader[x], cat)].tolist()
    for i, row in enumerate(hharray):
        d = diff[i]
        while d != 0:
            c = weighted_choice(agecatsarray[i, :])
            if d > 0:
                agecat[i][c] += 1
                d -= 1
            elif agecat[i][c] > 0:
                agecat[i][c] -= 1
                d += 1

    parray[:, map(lambda x: pheader[x], cat)] = np.asarray(agecat)

    with open(pfn, 'wb') as f:
        writer = csv.writer(f)
        head = sorted(pheader, key = pheader.get)
        writer.writerow(head)
        for line in parray.astype(int):
            writer.writerow(line)
