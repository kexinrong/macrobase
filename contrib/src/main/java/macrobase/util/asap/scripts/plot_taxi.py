import sys
import matplotlib.pyplot as plt
from matplotlib import dates
import datetime
import numpy as np
import scipy.stats

def plot_zscore(ts, readings, name, window, slide, binSize):
    plt.tick_params(labelsize=20)
    plt.rcParams['axes.linewidth'] = 1.5
    fig = plt.figure(figsize=(18, 15), dpi=300)

    ax3 = fig.add_subplot(313)
    ax1 = fig.add_subplot(311, sharex=ax3, sharey=ax3)
    ax2 = fig.add_subplot(312, sharex=ax3, sharey=ax3)

    zscores = scipy.stats.zscore(readings[0])
    ax1.plot(ts[0], zscores, linestyle='-', linewidth=2)
    ax1.xaxis.set_major_formatter(dates.DateFormatter('%m/%d'))
    plt.ylabel("zscore", fontsize=30)
    plt.title("NYC Taxi Passengers", fontsize=35)
    ax1.set_ylim(-3, 4)
    plt.setp(ax1.get_xticklabels(), visible=False)
    ax1.annotate('', xy=(datetime.datetime(2014, 11, 27, 12), 0),  xycoords='data',
        xytext=(0.78, 0.7), textcoords='axes fraction',
        arrowprops=dict(facecolor='black', shrink=0.05, width=5, color="red"))
    ax1.text(0.685, 0.85, "Unsmoothed",
        fontsize=36,
        fontweight="bold",
        horizontalalignment='left',
        verticalalignment='center',
        transform = ax1.transAxes)


    zscores = scipy.stats.zscore(readings[2])
    ax2.plot(ts[2], zscores, linestyle='-', linewidth=2)
    ax2.xaxis.set_major_formatter(dates.DateFormatter('%m/%d'))
    plt.setp(ax2.get_xticklabels(), visible=False)
    plt.ylabel("zscore", fontsize=30)
    ax2.annotate('', xy=(datetime.datetime(2014, 11, 27, 12), -2),  xycoords='data',
        xytext=(0.8, 0.6), textcoords='axes fraction',
        arrowprops=dict(facecolor='black', shrink=0.05, width=5, color="red"))
    ax2.text(0.685, 0.85, "ASAP (this paper)",
        fontsize=36,
        fontweight="bold",
        horizontalalignment='left',
        verticalalignment='center',
        transform = ax2.transAxes)


    zscores = scipy.stats.zscore(readings[1])
    ax3.plot(ts[1], zscores, linestyle='-', linewidth=2)
    ax3.xaxis.set_major_formatter(dates.DateFormatter('%m/%d'))
    plt.xlabel("time", fontsize=30)
    plt.ylabel("zscore", fontsize=30)
    ax3.text(0.685, 0.85, "Oversmoothed",
        fontsize=36,
        fontweight="bold",
        horizontalalignment='left',
        verticalalignment='center',
        transform = ax3.transAxes)

    for tick in ax3.xaxis.get_major_ticks():
        tick.label.set_fontsize(25)

    ax = [ax1, ax2, ax3]
    for a in ax:
        a.set_yticks([-4, -2, 0, 2, 4], minor=False)
        for tick in a.yaxis.get_major_ticks():
            tick.label.set_fontsize(25)
        a.set_axisbelow(True)
        a.yaxis.grid(color='gray', linestyle='dashed', linewidth=1.5, which='major')
        a.xaxis.grid(color='gray', linestyle='dashed', linewidth=1.5)

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
                if "Original" in name or "Oversmooth" in name:
                    readings.append(reading)
                    ts.append(t)
                t = []
                reading = []
            name = line

    if "ASAP" in name:
        readings.append(reading)
        ts.append(t)
    print len(ts)
    plot_zscore(ts, readings, name, w, s, binSize)

    f.close()
