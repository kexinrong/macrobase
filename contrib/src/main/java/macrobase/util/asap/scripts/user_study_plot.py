import sys
import matplotlib.pyplot as plt
from matplotlib import dates
from matplotlib import ticker
import matplotlib.transforms
import datetime
import numpy as np
import scipy.stats

method_names = ["Origin", "ASAP", "Oversmooth", "PAA100", "PAA800"]

def region_text(ax):
    ax.text(0.1, 0.87, 'A',
        verticalalignment='center', horizontalalignment='right',
        transform=ax.transAxes, color='red', fontsize=30)
    ax.text(0.3, 0.87, 'B',
        verticalalignment='center', horizontalalignment='right',
        transform=ax.transAxes, color='red', fontsize=30)
    ax.text(0.5, 0.87, 'C',
        verticalalignment='center', horizontalalignment='right',
        transform=ax.transAxes, color='red', fontsize=30)
    ax.text(0.7, 0.87, 'D',
        verticalalignment='center', horizontalalignment='right',
        transform=ax.transAxes, color='red', fontsize=30)
    ax.text(0.9, 0.87, 'E',
        verticalalignment='center', horizontalalignment='right',
        transform=ax.transAxes, color='red', fontsize=30)

def plot_window(ts, readings, id, format):
    plt.clf()
    plt.figure()
    N = len(ts)
    fig, (ax1, ax2, ax3, ax4, ax5) = plt.subplots(5, 1, sharey=True)
    axes = [ax1, ax2, ax3, ax4, ax5]
    for i in range(5):
        axes[i].plot(ts[i], scipy.stats.zscore(readings[i]), linestyle='-', linewidth=3)
        axes[i].get_yaxis().set_visible(False)

        #if i == 3:
        #if format != "sec":
            #axes[i].xaxis.set_major_formatter(dates.DateFormatter(format))
            #if id == 14:
            #    axes[i].set_xticks([datetime.datetime(1750,1,1), datetime.datetime(1800,1,1), datetime.datetime(1850,1,1),
            #        datetime.datetime(1900,1,1), datetime.datetime(1950,1,1)])
            #axes[i].set_xticks(tick_marks, minor=True)
        for tick in axes[i].xaxis.get_major_ticks():
            tick.label.set_fontsize(20)
    for i in range(5):
        if id == 27:
            axes[i].plot((20.1, 20.1),
                (axes[i].get_ylim()), 'r--', linewidth=2)
            axes[i].plot((31.3, 31.3),
                (axes[i].get_ylim()), 'r--', linewidth=2)
            axes[i].plot((ts[2][len(ts[2]) * 3 / 5 - 5], ts[2][len(ts[2]) * 3 / 5 - 5]), 
                (axes[i].get_ylim()), 'r--', linewidth=2)
            axes[i].plot((54.1, 54.1),
                (axes[i].get_ylim()), 'r--', linewidth=2)
        elif id == 42:
            axes[i].plot((9400, 9400),
                (axes[i].get_ylim()), 'r--', linewidth=2)
            axes[i].plot((14500, 14500),
                (axes[i].get_ylim()), 'r--', linewidth=2)
            axes[i].plot((20200, 20200), 
                (axes[i].get_ylim()), 'r--', linewidth=2)
            axes[i].plot((25300, 25300),
                (axes[i].get_ylim()), 'r--', linewidth=2)
        else:
            axes[i].plot((ts[2][len(ts[2]) / 5], ts[2][len(ts[2]) / 5]), 
                (axes[i].get_ylim()), 'r--', linewidth=2)
            axes[i].plot((ts[2][len(ts[2]) * 2 / 5], ts[2][len(ts[2]) * 2 / 5]), 
                (axes[i].get_ylim()), 'r--', linewidth=2)
            axes[i].plot((ts[2][len(ts[2]) * 3 / 5], ts[2][len(ts[2]) * 3 / 5]), 
                (axes[i].get_ylim()), 'r--', linewidth=2)
            axes[i].plot((ts[2][len(ts[2]) * 4 / 5], ts[2][len(ts[2]) * 4 / 5]), 
                (axes[i].get_ylim()), 'r--', linewidth=2)
        region_text(axes[i])


    plt.tight_layout()
    fig.set_size_inches(16, 18)
    for i in range(5):
        axes[i].set_xlabel(xlabel[id], fontsize=25)
        #axes[i].set_xlim(ts[2][0], ts[2][-1])
        axes[i].set_xlim(ts[i][0], ts[i][-1])
        if id == 27 or id == 42:
            axes[i].set_xlim(ts[2][0], ts[2][-1])
        extent = axes[i].get_window_extent().transformed(fig.dpi_scale_trans.inverted())
        (x0, y0, x1, y1) = extent.extents
        y1 += 0.2
        y0 -= 0.8
        x0 -= 0.5
        x1 += 0.5
        plt.savefig('../plots/us_%s_%d.png' % (id, i), 
            bbox_inches= matplotlib.transforms.Bbox([[x0, y0], [x1, y1]]), dpi=900/16.)
    plt.close(fig)


def plot_all(ts, readings, id, format):
    plt.clf()
    plt.figure()
    N = len(ts)
    fig, (ax1, ax2, ax3, ax4, ax5) = plt.subplots(5, 1, sharey=True)
    axes = [ax1, ax2, ax3, ax4, ax5]
    for i in range(5):
        axes[i].plot(ts[i], scipy.stats.zscore(readings[i]), linestyle='-', linewidth=3)
        axes[i].get_yaxis().set_visible(False)

        for tick in axes[i].xaxis.get_major_ticks():
            tick.label.set_fontsize(20)
    for i in range(5):
        if id == 27:
            axes[i].plot((20.1, 20.1),
                (axes[i].get_ylim()),  ls='--', color="gray", linewidth=2)
            axes[i].plot((31.3, 31.3),
                (axes[i].get_ylim()), ls='--', color="gray", linewidth=2)
            axes[i].plot((ts[2][len(ts[2]) * 3 / 5 - 5], ts[2][len(ts[2]) * 3 / 5 - 5]), 
                (axes[i].get_ylim()), ls='--', color="gray", linewidth=2)
            axes[i].plot((54.1, 54.1),
                (axes[i].get_ylim()), ls='--', color="gray", linewidth=2)
        elif id == 42:
            axes[i].plot((9400, 9400),
                (axes[i].get_ylim()), ls='--', color="gray", linewidth=2)
            axes[i].plot((14500, 14500),
                (axes[i].get_ylim()), ls='--', color="gray", linewidth=2)
            axes[i].plot((20200, 20200),
                (axes[i].get_ylim()), ls='--', color="gray", linewidth=2)
            axes[i].plot((25300, 25300),
                (axes[i].get_ylim()), ls='--', color="gray", linewidth=2)
        else:
            axes[i].plot((ts[2][len(ts[2]) / 5], ts[2][len(ts[2]) / 5]), 
                (axes[i].get_ylim()), ls='--', color="gray", linewidth=2)
            axes[i].plot((ts[2][len(ts[2]) * 2 / 5], ts[2][len(ts[2]) * 2 / 5]), 
                (axes[i].get_ylim()), ls='--', color="gray", linewidth=2)
            axes[i].plot((ts[2][len(ts[2]) * 3 / 5], ts[2][len(ts[2]) * 3 / 5]), 
                (axes[i].get_ylim()), ls='--', color="gray", linewidth=2)
            axes[i].plot((ts[2][len(ts[2]) * 4 / 5], ts[2][len(ts[2]) * 4 / 5]), 
                (axes[i].get_ylim()), ls='--', color="gray", linewidth=2)

    fig.set_size_inches(16, 13)

    for i in range(5):
        axes[i].set_xlim(ts[2][0], ts[2][-1])
        if i < 4:
            axes[i].get_xaxis().set_visible(False)
        axes[i].text(0.03, 0.88, method_names[i],
            verticalalignment='center', horizontalalignment='left',
            transform=axes[i].transAxes, fontsize=25)
    plt.subplots_adjust(hspace=0.05)
    plt.xlabel(xlabel[id], fontsize=25)
    plt.savefig("../plots/us_%s.pdf" % id,  bbox_inches='tight')
    plt.close(fig)


if __name__ == '__main__':
    datasets = [14, 27, 36, 38, 42]
    timeformat = ["%Y", "%m/%d", "sec", "sec", "sec"]
    xlabel = {14:"Years", 27:"Days", 36:"Sec", 38:"Sec", 42:"Time"}
    '''major_ticks = {
        14:[datetime.datetime(1750,1,1), datetime.datetime(1800,1,1), datetime.datetime(1850,1,1),
        datetime.datetime(1900,1,1)]
    }'''
    tick_marks = {36: [220, 221], 14:[datetime.datetime(1880,1,1), datetime.datetime(1881,1,1)], 
        38: [383, 415], 42: [11500, 12500], 
        27: [datetime.datetime(2014, 11, 24), datetime.datetime(2014, 12, 1)]}

    for i in range(len(datasets)):
        f = open("../plots/%s_userstudy.txt" %datasets[i], "r")

        name = "Default"
        w = 1
        s = 1
        binSize = 60
        ts = []
        readings = []
        all_ts = []
        all_readings = []
        min_ts = None
        for line in f.readlines():
            if line[0] >= '0' and line[0] <= '9' and len(line.split()) == 3:
                binSize = float(line.split()[0]) / 1000 / 60
                w = float(line.split()[1])
                s = float(line.split()[2])
            elif "," in line:
                ms = float(line.split(",")[0])
                if timeformat[i] == "sec":
                    ts.append(ms / 1000)
                else:
                    time = datetime.datetime.fromtimestamp(ms/1000.0)
                    ts.append(time)
                    if not min_ts:
                        min_ts = time
                readings.append(float(line.split(",")[1]))
            else:
                if len(ts) > 0:
                    if datasets[i] == 14 or datasets[i] == 27:
                        delta_ts = []
                        for k in range(len(ts)):
                            elapsed = ts[k] - min_ts
                            if datasets[i] == 14:
                                delta_ts.append(elapsed.total_seconds() / (24 * 3600 * 365))
                            elif  datasets[i] == 27:
                                delta_ts.append(elapsed.total_seconds() / (24 * 3600))
                        ts = delta_ts
                    all_ts.append(ts)
                    all_readings.append(readings)
                    ts = []
                    readings = []
                name = line

        if len(ts) > 0:
            if datasets[i] == 14 or datasets[i] == 27:
                delta_ts = []
                for k in range(len(ts)):
                    elapsed = ts[k] - min_ts
                    if datasets[i] == 14:
                        delta_ts.append(elapsed.total_seconds() / (24 * 3600 * 365))
                    elif  datasets[i] == 27:
                        delta_ts.append(elapsed.total_seconds() / (24 * 3600))
                ts = delta_ts
            all_ts.append(ts)
            all_readings.append(readings)
        plot_window(all_ts, all_readings, datasets[i], timeformat[i])
        plot_all(all_ts, all_readings, datasets[i], timeformat[i])

        f.close()



    # Example data
    sim = []
    l = 800
    for i in range(l):
        if i > 14 * 24 and i < 15 * 24:
            sim.append(4)
        else:
            sim.append(1)

    fig = plt.figure()
    ax = fig.add_subplot(111)
    ax.set_ylim(0, 5)
    ax.plot(sim,  linestyle='-', linewidth=3)
    ax.set_xlabel("Time", fontsize=25)
    ax.plot((l / 5, l / 5), (ax.get_ylim()), 'r--', linewidth=2)
    ax.plot((l * 2 / 5, l * 2 / 5), (ax.get_ylim()), 'r--', linewidth=2)
    ax.plot((l * 3 / 5, l * 3 / 5), (ax.get_ylim()), 'r--', linewidth=2)
    ax.plot((l * 4 / 5, l * 4 / 5), (ax.get_ylim()), 'r--', linewidth=2)
    ax.get_yaxis().set_visible(False)
    for tick in ax.xaxis.get_major_ticks():
            tick.label.set_fontsize(20)
    region_text(ax)
    plt.tight_layout()
    fig.set_size_inches(16, 4.5)
    plt.savefig('../plots/us_sim.png', dpi=800/16.)