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
    ax2 = fig.add_subplot(212)
    ax1 = fig.add_subplot(211, sharex=ax2, sharey=ax2)
    ax1.set_xlim(min(ts[1]), max(ts[1]))
    ax2.set_xlim(min(ts[1]), max(ts[1]))
    zscores = scipy.stats.zscore(readings[0])
    ax1.plot(ts[0], zscores, linestyle='-', linewidth=2)
    ax1.xaxis.set_major_formatter(dates.DateFormatter('%m/%Y'))
    plt.ylabel("zscore", fontsize=30)
    plt.title("Average Temperature in England", fontsize=35)
    plt.setp(ax1.get_xticklabels(), visible=False)

    zscores = scipy.stats.zscore(readings[1])
    ax2.plot(ts[1], zscores, linestyle='-', linewidth=2)
    ax2.xaxis.set_major_formatter(dates.DateFormatter('%Y'))
    ax2.set_xticks([datetime.datetime(1750, 1, 1), datetime.datetime(1800, 1, 1),
        datetime.datetime(1850, 1, 1), datetime.datetime(1900, 1, 1),
        datetime.datetime(1950, 1, 1)], minor=False)
    plt.xlabel("Time", fontsize=30)
    plt.ylabel("zscore", fontsize=30)
    ax2.text(0.01, 0.85, "ASAP",
        fontsize=36,
        fontweight="bold",
        horizontalalignment='left',
        verticalalignment='center',
        transform = ax2.transAxes)
    for tick in ax2.xaxis.get_major_ticks():
        tick.label.set_fontsize(25)

    ax = [ax1, ax2]
    for a in ax:
        a.set_yticks([-2, 0, 2], minor=False)
        for tick in a.yaxis.get_major_ticks():
            tick.label.set_fontsize(25)
        a.set_axisbelow(True)
        #a.yaxis.grid(color='gray', linestyle='dashed', linewidth=1.5, which='major')
        #a.xaxis.grid(color='gray', linestyle='dashed', linewidth=1.5)

    plt.tight_layout()
    plt.savefig("../plots/temperature.pdf")
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
