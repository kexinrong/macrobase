import sys
import matplotlib.pyplot as plt
from matplotlib import dates
import datetime
import numpy as np
import scipy.stats

def plot_zscore(ts, readings, name, window, slide, binSize):
    plt.tick_params(labelsize=20)
    plt.rcParams['axes.linewidth'] = 1.5
    fig = plt.figure(figsize=(18, 10), dpi=300)
    ax1 = fig.add_subplot(211)
    zscores = scipy.stats.zscore(readings[0])
    ax1.plot(ts[0], zscores, linestyle='-', linewidth=2)
    plt.ylabel("zscore", fontsize=30)
    plt.title("CPU Utilization", fontsize=35)
    plt.setp(ax1.get_xticklabels(), visible=False)


    ax2 = fig.add_subplot(212, sharex=ax1, sharey=ax1)
    zscores = scipy.stats.zscore(readings[1])
    ax2.plot(ts[1], zscores, linestyle='-', linewidth=2)
    ax2.xaxis.set_major_formatter(dates.DateFormatter('%m/%d'))
    plt.ylabel("zscore", fontsize=30)
    plt.xlabel("Time", fontsize=30)
    for tick in ax2.xaxis.get_major_ticks():
        tick.label.set_fontsize(25)


    ax = [ax1, ax2]
    for a in ax:
        a.set_yticks([-2, 0, 2, 4, 6], minor=False)
        for tick in a.yaxis.get_major_ticks():
            tick.label.set_fontsize(25)
        a.set_axisbelow(True)
        a.yaxis.grid(color='gray', linestyle='dashed', linewidth=1.5, which='major')
        a.xaxis.grid(color='gray', linestyle='dashed', linewidth=1.5)

    ax2.text(0.01, 0.85, "ASAP",
        fontsize=36,
        fontweight="bold",
        horizontalalignment='left',
        verticalalignment='center',
        transform = ax2.transAxes)

    plt.tight_layout()
    plt.savefig("../plots/%s_zscores.pdf" %(filename))
    plt.close(fig)

if __name__ == '__main__':
    if len(sys.argv) == 1:
        exit(0)
    filename = sys.argv[1]
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
            time = datetime.datetime.fromtimestamp(ms/1000.0)
            t.append(time)
            reading.append(float(line.split(",")[1]))
        else:
            if len(t) > 0:
                if "Original" in name:
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
