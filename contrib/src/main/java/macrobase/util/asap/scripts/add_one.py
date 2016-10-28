from matplotlib import pyplot as plt
import numpy as np
import os
import re
import matplotlib


def setHatchThickness(value):
	libpath = matplotlib.__path__[0]
	backend_pdf = libpath + "/backends/backend_pdf.py"
	with open(backend_pdf, "r") as r:
	    code = r.read()
	    code = re.sub(r'self\.output\((\d+\.\d+|\d+)\,\ Op\.setlinewidth\)',
	                   "self.output(%s, Op.setlinewidth)" % str(value), code)
	    with open('/tmp/hatch.tmp', "w") as w:
	        w.write(code)
	    print backend_pdf
	    os.system('sudo mv /tmp/hatch.tmp %s' % backend_pdf)


def get_tpt(filename):
	f = open(filename, "r")
	tpt = []
	for line in f.readlines():
		if "runtime" in line:
			words = line.split(",")
			points = int(words[0].split(":")[1][1:])
			runtime = int(words[2].split(":")[1][1:-11])
			tpt.append(points * 1000000.0 / runtime)
	f.close()
	N = len(tpt) / 3
	avg_tpt = []
	std_tpt = []
	for i in range(3):
		data = []
		for j in range(N):
			data.append(tpt[j * 4 + i])
		avg_tpt.append(np.mean(data))
		std_tpt.append(np.std(data))
	return avg_tpt, std_tpt

tpt1, err1 = get_tpt("../results/22_addone.txt")
tpt2, err2 = get_tpt("../results/24_addone.txt")

plt.rcParams['axes.linewidth'] = 1.5
fig = plt.figure(figsize=(12, 6), dpi=300)
ax = fig.add_subplot(111)

N = 3
ind = np.arange(N)
width = 0.35
ax.bar(ind, tpt1, width, hatch='x', color=(0.2588,0.4433,1.0), label="machine_temp", linewidth=1.5)
ax.bar(ind+width, tpt2, width, hatch="\\", color=(1.0,0.5,0.62), 
	label="traffic_data", linewidth=1.5)

ax.set_xticks(ind + width)
ax.set_xticklabels(("Grid", "+ Pixel", "+ AC"))
ax.set_yscale("log")
ax.set_ylabel("Throughput (#pts/sec)", fontsize=30)

for tick in ax.yaxis.get_major_ticks():
	tick.label.set_fontsize(25)
for tick in ax.xaxis.get_major_ticks():
	tick.label.set_fontsize(25)
ax.set_axisbelow(True)
ax.yaxis.grid(color='gray', linestyle='dashed', linewidth=1.5, which='major')
plt.legend(loc = 'upper center', bbox_to_anchor=(0, 0, 1, 1),
            bbox_transform = plt.gcf().transFigure, ncol=5,
            fontsize=30, markerscale=1, borderaxespad=0.1, frameon=False)
setHatchThickness(1.5)
plt.savefig("addone.pdf")
