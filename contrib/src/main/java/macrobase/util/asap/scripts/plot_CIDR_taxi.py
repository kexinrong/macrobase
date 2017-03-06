import sys
import matplotlib.pyplot as plt
from matplotlib import dates
import datetime
import numpy as np
import scipy.stats


def plot_zscore(ts, readings, name, window, slide, binSize):
    plt.tick_params(labelsize=20)
    plt.rcParams['axes.linewidth'] = 3
    fig = plt.figure(figsize=(18, 9), dpi=300)

    titles = {0: "Raw Data", 1: "Smoothed", 2: "Oversmoothed"}

    ax3 = fig.add_subplot(313)
    ax1 = fig.add_subplot(311, sharex=ax3)
    ax2 = fig.add_subplot(312, sharex=ax3)
    ax3.set_xlim(datetime.datetime(2014, 10, 1), datetime.datetime(2014, 12, 10))

    zscores = scipy.stats.zscore(readings[0])
    ax1.plot(ts[0], zscores, linestyle='-', linewidth=3)
    plt.setp(ax1.get_xticklabels(), visible=False)
    plt.setp(ax1.get_yticklabels(), visible=False)
    ax1.xaxis.set_ticks_position('none') 
    ax1.yaxis.set_ticks_position('none') 
    ax1.spines['top'].set_visible(False)
    ax1.spines['right'].set_visible(False)
    ax1.spines['bottom'].set_visible(False)
    ax1.spines['left'].set_color('gray')
    ax1.set_ylim(-2.2, 2.5)
    ax1.text(0.77, 0.87, "Raw Data",
        fontsize=35,
        horizontalalignment='left',
        verticalalignment='center',
        transform = ax1.transAxes)


    zscores = scipy.stats.zscore(readings[1])
    ax2.plot(ts[1], zscores, linestyle='-', linewidth=3)
    ax2.set_ylim(min(zscores), max(zscores))
    ax2.xaxis.set_ticks_position('none') 
    ax2.yaxis.set_ticks_position('none') 
    plt.setp(ax2.get_xticklabels(), visible=False)
    plt.setp(ax2.get_yticklabels(), visible=False)
    ax2.spines['top'].set_visible(False)
    ax2.spines['right'].set_visible(False)
    ax2.spines['bottom'].set_visible(False)
    ax2.spines['left'].set_color('gray')
    ax2.set_ylim(-2.2, 2.5)
    ax2.text(0.77, 0.7, "Smoothed",
        fontsize=35,
        horizontalalignment='left',
        verticalalignment='center',
        transform = ax2.transAxes)


    zscores = scipy.stats.zscore(readings[2])
    ax3.plot(ts[2], zscores, linestyle='-', linewidth=3)
    ax3.yaxis.set_ticks_position('none') 
    ax3.xaxis.set_ticks_position('bottom')
    plt.setp(ax3.get_yticklabels(), visible=False)
    ax3.xaxis.set_major_formatter(dates.DateFormatter('%m/%d'))
    ax3.spines['top'].set_visible(False)
    ax3.spines['right'].set_visible(False)
    ax3.spines['bottom'].set_visible(False)
    ax3.spines['left'].set_color('gray')
    ax3.set_ylim(-2.2, 2.5)
    ax3.tick_params('both', width=1.5, color="gray")
    ax3.text(0.77, 0.7, "Oversmoothed",
        fontsize=35,
        horizontalalignment='left',
        verticalalignment='center',
        transform = ax3.transAxes)

    for tick in ax3.xaxis.get_major_ticks():
        tick.label.set_fontsize(30)

    ax = [ax1, ax2, ax3]
    for a in ax:
        a.set_yticks([0], minor=False)
        a.set_axisbelow(True)
        a.yaxis.grid(color='gray', linestyle='dashed', linewidth=3, which='major')

    plt.tight_layout()
    plt.savefig("../plots/CIDR_ASAP.pdf")
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

    if "Oversmooth" in name:
        readings.append(reading)
        ts.append(t)
    plot_zscore(ts, readings, name, w, s, binSize)

    f.close()
