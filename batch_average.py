from matplotlib import pyplot as plt
import sys
import subprocess
import numpy as np

bashCommand = 'java -Xms10G -Xmx256G -XX:+UseParallelGC -XX:ParallelGCThreads=32 -cp "core/target/classes:frontend/target/classes:frontend/src/main/resources/:contrib/target/classes:assembly/target/*:$CLASSPATH" macrobase.util.asap.BatchExperiment %d %d'
N = 1
results_dir = "batch_results/"

avg_tpt = {}
avg_variances = {}
res = []

def parse_results(dataset_id):
	f = open(results_dir + "batch_%d.txt" % dataset_id, "r")
	tpt = {}
	variances = {}
	key = "ASAP"
	res = []
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

	tpt_means = {}
	for key in tpt:
		tpt_means[key] = []
		for i in range(len(res)):
			tpt_means[key].append(np.mean(tpt[key][i]))

	for i in range(len(variances["Exhaustive search"])):
		for key in variances:
			if key == "Exhaustive search":
				continue
			variances[key][i] /= variances["Exhaustive search"][i]
			tpt_means[key][i] /= tpt_means["Exhaustive search"][i]
		variances["Exhaustive search"][i] = 1
		tpt_means["Exhaustive search"][i] = 1

	for key in tpt_means:
		if not key in avg_tpt:
			avg_tpt[key] = tpt_means[key]
			avg_variances[key] = variances[key]
		else:
			for i in range(len(res)):
				avg_tpt[key][i] += tpt_means[key][i]
				avg_variances[key][i] += variances[key][i]

	return res

def plot_results(res):
	# Plotting
	markers = {"ASAP": "s", "Grid2": "D", 
		"Grid5": "<", "Grid10": "^", "BinarySearch": "o"}
	colors = {"ASAP": "g", "Grid2": "b", "Grid10": "r", "BinarySearch": "c"}
	plt.clf()
	plt.rcParams['axes.linewidth'] = 1.5
	fig = plt.figure(figsize=(24, 10), dpi=300)
	ax1 = fig.add_subplot(121)
	ax2 = fig.add_subplot(122)

	for key in avg_tpt:
		if "Exhaustive" in key or "Grid5" in key:
			continue
		msize = 15
		if markers[key] == "<" or markers[key] == "^" or markers[key] == "o":
			msize += 3
		ax1.plot(res, np.array(avg_tpt[key]) / len(dataset_ids), label=key, marker=markers[key],
			markersize=msize, linewidth=2, color=colors[key])
		ax2.plot(res, np.array(avg_variances[key]) / len(dataset_ids), label=key, marker=markers[key],
			markersize=msize, linewidth=2, color=colors[key])

	ax1.set_xlabel("Resolution", fontsize=30)
	ax1.set_ylabel("AVG Throughput Gain to Exhaustive Search", fontsize=30)

	ax2.set_xlabel("Resolution", fontsize=30)
	ax2.set_ylabel("AVG Roughness to Optimal", fontsize=30)

	ax = [ax1, ax2]
	for a in ax:
		a.set_xticks([0, 1000, 2000, 3000, 4000, 5000], minor=False)
		for tick in a.yaxis.get_major_ticks():
			tick.label.set_fontsize(25)
		for tick in a.xaxis.get_major_ticks():
			tick.label.set_fontsize(25)
		a.set_axisbelow(True)
		a.yaxis.grid(color='gray', linestyle='dashed', linewidth=1.5, which='major')
		#a.xaxis.grid(color='gray', linestyle='dashed', linewidth=1.5)

	#fig.legend(lines, legends, "upper center")
	plt.legend(loc = 'upper center', bbox_to_anchor=(0, 0, 1, 1),
            bbox_transform = plt.gcf().transFigure, ncol=5,
            fontsize=30, markerscale=1, borderaxespad=0.1, frameon=False)
	plt.savefig(results_dir + "batch_average.pdf")


def run():
	f = open(results_dir + "batch_%d.txt" % dataset_id, "w")
	for res in range(500, 5100, 500):
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
	#dataset_ids = [8, 14, 15, 19, 27, 22, 24, 10]
	dataset_ids = [15, 19, 22, 24, 10]
	res = []
	for dataset_id in dataset_ids:
		print dataset_id
		#run()
		res = parse_results(dataset_id)
	plot_results(res)