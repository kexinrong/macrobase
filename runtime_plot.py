from matplotlib import pyplot as plt
import sys
import subprocess
import numpy as np

bashCommand = 'java -Xms10G -Xmx256G -XX:+UseParallelGC -XX:ParallelGCThreads=32 -cp "core/target/classes:frontend/target/classes:frontend/src/main/resources/:contrib/target/classes:assembly/target/*:$CLASSPATH" macrobase.util.asap.BatchExperiment %d %d'
N = 5
results_dir = "batch_results/"

def plot_results(dataset_id):
	f = open(results_dir + "batch_%d.txt" % dataset_id, "r")
	tpt = {}
	variances = {}
	res = []
	key = "ASAP"
	for line in f.readlines():
		words = line[:-1].split(",")
		if len(line) == 1:
			continue
		elif len(line) > 3 and len(words) == 1:
			try:
				res.append(int(line))
			except ValueError:
				key = line[:-1]
				if key == "Grid1":
					key = "Exhaustive search"
				if not key in tpt:
					tpt[key] = []
					variances[key] = []
		elif "runtime" in words[2]:
			time = int(words[2].split(":")[1][1:-11])
			points = int(words[0].split(":")[1][1:])
			val = points * 1000000.0 / time
			if len(tpt[key]) < len(res):
				tpt[key].append([val])
			else:
				tpt[key][-1].append(val)
		elif "var" in words[2]:
			val = float(words[2].split(":")[1][1:])
			if len(variances[key]) < len(res):
				variances[key].append(val)
	f.close()

	for i in range(len(variances["Exhaustive search"])):
		for key in variances:
			if key == "Exhaustive search":
				continue
			variances[key][i] /= variances["Exhaustive search"][i]
		variances["Exhaustive search"][i] = 1

	# Plotting
	markers = {"ASAP": "s", "Exhaustive search": "o", "Grid2": "D", "Grid5": "<", "Grid10": "^"}
	plt.clf()
	plt.rcParams['axes.linewidth'] = 1.5
	fig = plt.figure(figsize=(24, 10), dpi=300)
	ax1 = fig.add_subplot(121)

	for key in tpt:
		means = []
		errors = []
		for i in range(len(res)):
			means.append(np.mean(tpt[key][i]))
			errors.append(np.std(tpt[key][i]))
		msize = 12
		if markers[key] == "<" or markers[key] == "^":
			msize += 3
		plt.errorbar(res, means, yerr=errors, label=key, marker=markers[key],
			markersize=msize, linewidth=2)

	plt.yscale('log')
	plt.xlabel("Resolution", fontsize=30)
	plt.ylabel("Throughput (#pts/s)", fontsize=30)


	ax2 = fig.add_subplot(122)
	lines = []
	legends = []
	for key in variances:
		msize = 12
		if markers[key] == "<" or markers[key] == "^":
			msize += 3
		l = ax2.plot(res, variances[key], label=key, marker=markers[key],
			markersize=msize, linewidth=2)
		lines.append(l)
		legends.append(key)

	plt.xlabel("Resolution", fontsize=30)
	plt.ylabel("Relative Roughness to Optimal", fontsize=30)

	ax = [ax1, ax2]
	for a in ax:
		for tick in a.yaxis.get_major_ticks():
			tick.label.set_fontsize(25)
		for tick in a.xaxis.get_major_ticks():
			tick.label.set_fontsize(25)
		a.set_axisbelow(True)
		a.yaxis.grid(color='gray', linestyle='dashed', linewidth=1.5, which='major')
		a.xaxis.grid(color='gray', linestyle='dashed', linewidth=1.5)

	#fig.legend(lines, legends, "upper center")
	plt.legend(loc = 'upper center', bbox_to_anchor=(0, 0, 1, 1),
            bbox_transform = plt.gcf().transFigure, ncol=5,
            fontsize=30, markerscale=1, borderaxespad=0.1, frameon=False)
	plt.savefig(results_dir + "%d_batch.pdf" % dataset_id)


def run():
	f = open(results_dir + "batch_%d.txt" % dataset_id, "w")
	for res in range(400, 4200, 400):
		print "Resolution: %d" %res
		f.write("%d\n" %res)
		for i in range(N):
			process = subprocess.Popen((bashCommand % (res, dataset_id)).split(), stdout=subprocess.PIPE)
			output, error = process.communicate()
			result_file = open(
				"contrib/src/main/java/macrobase/util/asap/results/%d_batch.txt" % dataset_id, "r")
			for line in result_file.readlines():
				f.write(line)
			result_file.close()

	f.close()

if __name__ == '__main__':
	#dataset_ids = [9, 11, 12, 13, 14, 15, 17, 18, 19, 21, 22, 24, 27, 10, 16, 1, 2, 8]
	dataset_ids = [27]
	for dataset_id in dataset_ids:
		print dataset_id
		#run()
		plot_results(dataset_id)