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
    ax3.set_xlim(datetime.datetime(2014, 10, 1), datetime.datetime(2014, 12, 10))

    zscores = scipy.stats.zscore(readings[0])
    ax1.plot(ts[0], zscores, linestyle='-', linewidth=3)
    ax1.xaxis.set_major_formatter(dates.DateFormatter('%m/%d'))
    ax1.set_ylabel("zscore", fontsize=35)
    ax1.set_title("NYC Taxi Passengers", fontsize=40)
    ax1.set_ylim(-3, 4)
    plt.setp(ax1.get_xticklabels(), visible=False)
    ax1.annotate('', xy=(datetime.datetime(2014, 11, 27, 12), 0),  xycoords='data',
        xytext=(0.83, 0.82), textcoords='axes fraction',
        arrowprops=dict(facecolor='black', shrink=0.05, width=5, color="red"))
    ax1.text(0.685, 0.85, "Unsmoothed",
        fontsize=36,
        fontweight="bold",
        horizontalalignment='left',
        verticalalignment='center',
        transform = ax1.transAxes)


    zscores = scipy.stats.zscore(readings[1])
    ax2.plot(ts[1], zscores, linestyle='-', linewidth=3)
    ax2.xaxis.set_major_formatter(dates.DateFormatter('%m/%d'))
    plt.setp(ax2.get_xticklabels(), visible=False)
    ax2.set_ylabel("zscore", fontsize=35)
    ax2.annotate('', xy=(datetime.datetime(2014, 11, 27, 12), -1.5),  xycoords='data',
        xytext=(0.83, 0.7), textcoords='axes fraction',
        arrowprops=dict(facecolor='black', shrink=0.05, width=5, color="red"))
    ax2.text(0.685, 0.85, "ASAP (this paper)",
        fontsize=36,
        fontweight="bold",
        horizontalalignment='left',
        verticalalignment='center',
        transform = ax2.transAxes)


    zscores = scipy.stats.zscore(readings[2])
    ax3.plot(ts[2], zscores, linestyle='-', linewidth=3)
    ax3.xaxis.set_major_formatter(dates.DateFormatter('%m/%d'))
    ax3.set_xlabel("time", fontsize=35)
    ax3.set_ylabel("zscore", fontsize=35)
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

    plt.tight_layout()
    plt.savefig("../plots/nyc_taxi.pdf")
    plt.close(fig)

if __name__ == '__main__':
    f = open("../plots/0_batch.txt", "r")

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
                if not "Original" in name:
                    readings.append(reading)
                    ts.append(t)
                t = []
                reading = []
            name = line
            print name

    readings.append(reading)
    ts.append(t)
    plot_zscore(ts, readings, name, w, s, binSize)

    f.close()
