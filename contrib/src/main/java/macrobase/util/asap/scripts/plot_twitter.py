import sys
import matplotlib.pyplot as plt
from matplotlib import dates
import datetime
import numpy as np
import scipy.stats

def plot_zscore(ts, readings, name, window, slide, binSize):
    plt.tick_params(labelsize=20)
    plt.rcParams['axes.linewidth'] = 1.5
    fig = plt.figure(figsize=(18, 5), dpi=300)
    ax1 = fig.add_subplot(111)
    ax1.plot(ts[0], readings[0], linestyle='-', linewidth=2)
    plt.ylabel("# mentions", fontsize=30)
    plt.title("Twitter Mentions of Apple", fontsize=35)

    plt.xlabel("Time", fontsize=30)
    ax1.xaxis.set_major_formatter(dates.DateFormatter('%m/%d/%y'))
    for tick in ax1.xaxis.get_major_ticks():
        tick.label.set_fontsize(25)

    ax = [ax1]
    for a in ax:
        a.set_yticks([2000, 4000, 6000], minor=False)
        for tick in a.yaxis.get_major_ticks():
            tick.label.set_fontsize(25)
        a.set_axisbelow(True)
        #a.yaxis.grid(color='gray', linestyle='dashed', linewidth=1.5, which='major')

    plt.tight_layout()
    plt.savefig("../plots/twitter.pdf")
    plt.close(fig)

if __name__ == '__main__':
    if len(sys.argv) == 1:
        exit(0)
    filename = sys.argv[1]
    f = open("../plots/%s_batch.txt" %filename, "r")

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

    plot_zscore(ts, readings, name, w, s, binSize)

    f.close()
