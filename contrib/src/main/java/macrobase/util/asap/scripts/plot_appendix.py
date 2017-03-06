import sys
import matplotlib.pyplot as plt
from matplotlib import dates
import datetime
import numpy as np
import scipy.stats


def plot_zscore(ts, readings, name, window, slide, binSize):
    plt.tick_params(labelsize=20)
    plt.rcParams['axes.linewidth'] = 1.5
    fig = plt.figure(figsize=(18, 9), dpi=300)
    ax1 = fig.add_subplot(211)
    ax2 = fig.add_subplot(212, sharex=ax1, sharey=ax1)

    zscores = scipy.stats.zscore(readings[0])
    ax1.plot(ts[0], zscores, linestyle='-', linewidth=2)
    if not time_format[dataset_id] == "sec":
        ax1.xaxis.set_major_formatter(dates.DateFormatter(time_format[dataset_id]))
        ax2.xaxis.set_major_formatter(dates.DateFormatter(time_format[dataset_id]))
    ax1.set_xlim(min(ts[1]), max(ts[1]))
    ax1.set_ylabel("zscore", fontsize=30)
    #ax1.set_title(titles[dataset_id], fontsize=35)
    plt.setp(ax1.get_xticklabels(), visible=False)

    zscores = scipy.stats.zscore(readings[1])
    ax2.plot(ts[1], zscores, linestyle='-', linewidth=2)
    plt.xlabel("Time", fontsize=30)
    plt.ylabel("zscore", fontsize=30)
    ax1.text(0.05, 0.14, "Original",
        fontsize=36,
        fontweight=5,
        horizontalalignment='left',
        verticalalignment='center',
        transform = ax1.transAxes)
    ax2.text(0.05, 0.14, "ASAP",
        fontsize=36,
        fontweight=5,
        horizontalalignment='left',
        verticalalignment='center',
        transform = ax2.transAxes)
    for tick in ax2.xaxis.get_major_ticks():
        tick.label.set_fontsize(25)

    ax = [ax1, ax2]
    for a in ax:
        if dataset_id == 22:
            a.set_yticks([-4, -2, 0, 2], minor=False)
        else:
            a.set_yticks([-2, 0, 2], minor=False)
        for tick in a.yaxis.get_major_ticks():
            tick.label.set_fontsize(25)
        a.set_axisbelow(True)
        #a.yaxis.grid(color='gray', linestyle='dashed', linewidth=1.5, which='major')
        #a.xaxis.grid(color='gray', linestyle='dashed', linewidth=1.5)

    plt.tight_layout()
    plt.savefig("../plots/%d.pdf" %(dataset_id))
    plt.close(fig)

if __name__ == '__main__':
    datasets = [8, 10, 15, 22, 24]
    titles = {8: "sim_daily", 10: "gas_sensor", 15:"ramp_traffic",
        22:"machine_temp", 24:"traffic_data"}
    time_format = {8: "%m/%d", 10: "sec", 15: "%m/%d", 22:"%m/%d",
        24:"%m/%d"}
    for dataset_id in datasets:
        filename = "%d_batch" % dataset_id
        f = open("../plots/%s.txt" %filename, "r")

        name = "Default"
        w = 1
        s = 1
        binSize = 60
        ts = []
        readings = []
        reading = []
        t = []

        for line in f.readlines():
            if line[0] >= '0' and line[0] <= '9' and len(line.split()) == 3:
                binSize = float(line.split()[0]) / 1000 / 60
                w = float(line.split()[1])
            elif "," in line:
                ms = float(line.split(",")[0])
                if time_format[dataset_id] != "sec":
                    time = datetime.datetime.fromtimestamp(ms/1000.0)
                    t.append(time)
                else:
                    t.append(ms/1000)
                reading.append(float(line.split(",")[1]))
            else:
                if len(t) > 0:
                    if "Original" in name or "ASAP" in name:
                        readings.append(reading)
                        ts.append(t)
                    t = []
                    reading = []
                name = line

        if "ASAP" in name:
            readings.append(reading)
            ts.append(t)
        plot_zscore(ts, readings, name, w, s, binSize)

        f.close()
