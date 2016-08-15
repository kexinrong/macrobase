import sys
import matplotlib.pyplot as plt
from matplotlib import dates
import datetime
import numpy as np

def plot_window(ts, readings, name, window, slide, binSize):
    fig = plt.figure(figsize=(18, 10))
    ax = fig.add_subplot(111)
    ax.set_title(name)
    ax.plot(ts, readings, linestyle='-', linewidth=1.5)
    ax.xaxis.set_major_formatter(dates.DateFormatter('%m/%d'))
    plt.tick_params(labelsize=20)
    plt.xlabel("time", fontsize=25)
    plt.ylabel("readings", fontsize=25)
    plt.title(name, fontsize=25)
    plt.grid(True)
    ax.text(0.5, 0.9, 'Interval: %s min, window: %s, slide: %s' \
        %(binSize, window, slide),
        fontsize=20,
        horizontalalignment='center',
        verticalalignment='center',
        transform = ax.transAxes,
        bbox=dict(facecolor='red', alpha=0.2))
    plt.savefig("../plots/%s_%s.png" %(filename, name), format="png")
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
    for line in f.readlines():
        if len(line.split()) == 3:
            binSize = float(line.split()[0]) / 1000 / 60
            w = float(line.split()[1])
            s = float(line.split()[2])
        elif "," in line:
            ms = float(line.split(",")[0])
            time = datetime.datetime.fromtimestamp(ms/1000.0)
            ts.append(time)
            readings.append(float(line.split(",")[1]))
        else:
            if len(ts) > 0:
                plot_window(ts, readings, name, w, s, binSize)
                ts = []
                readings = []
            name = line


    plot_window(ts, readings, name, w, s, binSize)

    f.close()
