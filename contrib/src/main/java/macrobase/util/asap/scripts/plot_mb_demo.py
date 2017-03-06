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

    if not time_format[dataset_id] == "sec":
        ax1.xaxis.set_major_formatter(dates.DateFormatter(time_format[dataset_id]))
    #ax1.set_xlim(min(ts[1]), max(ts[1]))
    ax1.set_xlim(datetime.datetime(2014, 3, 1), datetime.datetime(2014, 5, 12))
    ax1.set_ylabel("Traffic Volume", fontsize=30)
    plt.setp(ax1.get_yticklabels(), visible=False)

    ax1.plot(ts[1], readings[1], linestyle='-', linewidth=3)
    plt.xlabel("Time", fontsize=30)
    for tick in ax1.xaxis.get_major_ticks():
        tick.label.set_fontsize(25)

    #ax1.set_axisbelow(True)
    ax1.spines['right'].set_visible(False)
    ax1.spines['top'].set_visible(False)
    ax1.xaxis.set_ticks_position('bottom')
    ax1.yaxis.set_ticks_position('left')

    sample_ts = [datetime.datetime(2014, 4, 19, 12), datetime.datetime(2014, 3, 5, 12),
        datetime.datetime(2014, 3, 26, 12), datetime.datetime(2014, 5, 8, 12)]
    idx = 1
    for t in sample_ts:
        for i in range(len(ts[1])):
            if ts[1][i] >= t:
                break
        color = '#3CB371'
        if idx == 1:
            color = 'r'
        ax1.plot(ts[1][i-7:i+8], readings[1][i-7:i+8], linewidth=4.5, color=color)
        ax1.text(ts[1][i-5], readings[1][i] + 0.1, str(idx),
            fontsize=35,
            horizontalalignment='left',
            verticalalignment='bottom',
            transform = ax1.transData)
        idx += 1

    plt.tight_layout()
    plt.savefig("../plots/demo_overview_traffic.pdf")
    plt.close(fig)

if __name__ == '__main__':
    datasets = [24]
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
