from matplotlib import pyplot as plt
import sys
import subprocess
import numpy as np

bashCommand = 'java -Xms10G -Xmx256G -XX:+UseParallelGC -XX:ParallelGCThreads=32 -cp "core/target/classes:frontend/target/classes:frontend/src/main/resources/:contrib/target/classes:assembly/target/*:$CLASSPATH" macrobase.util.asap.StreamingExperiment %d %d %d'
N = 5
RES = 2000
tpt = {}
results_dir = "evals/"

def parse_results(dataset_id):
	f = open(results_dir + "stream_%d.txt" % dataset_id, "r")
	interval = []
	interval_in_points = []
	raw_tpt = {}
	for line in f.readlines():
		words = line[:-1].split(",")
		if len(line) == 1:
			continue
		elif len(words) == 1:
			try:
				interval.append(int(line))
			except ValueError:
				continue
		elif "runtime" not in words[2]:
			continue
		else:
			time = int(words[2].split(":")[1][1:-11])
			points = int(words[0].split(":")[1][1:])
			val = points * 1000000.0 / time
			interval_points = int(words[3].split(":")[1][1:])
			if not interval_points in raw_tpt:
				raw_tpt[interval_points] = []

			raw_tpt[interval_points].append(val)
			if not interval_points in interval_in_points:
				interval_in_points.append(interval_points)

	f.close()

	means = []
	errors = []
	for i in interval_in_points:
		means.append(np.mean(raw_tpt[i]))
		errors.append(np.std(raw_tpt[i]))
	tpt[dataset_id] = {}
	tpt[dataset_id]["means"] = means
	tpt[dataset_id]["errors"] = errors
	return interval_in_points


def plot(interval_in_points):
	plt.clf()
	plt.rcParams['axes.linewidth'] = 1.5
	fig = plt.figure(figsize=(12, 6), dpi=300)
	ax = fig.add_subplot(111)
	markers = {22: "s", 24: "D"}
	labels = {22: "machine_temp", 24: "traffic_data"}
	for dataset_id in tpt:
		plt.errorbar(interval_in_points, tpt[dataset_id]["means"], 
			yerr=tpt[dataset_id]["errors"], label=labels[dataset_id], 
			markersize = 15,
			marker=markers[dataset_id], linewidth=2)
	plt.xscale('log')
	plt.yscale('log')
	plt.xlabel("Refresh Interval (# points)", fontsize=30)
	plt.ylabel("Throughput (#pts/sec)", fontsize=30)
	for tick in ax.yaxis.get_major_ticks():
		tick.label.set_fontsize(20)
	for tick in ax.xaxis.get_major_ticks():
		tick.label.set_fontsize(20)
	ax.set_axisbelow(True)
	ax.yaxis.grid(color='gray', linestyle='dashed', linewidth=1.5, which='major')
	plt.legend(loc='upper center', bbox_to_anchor=(0, 0, 1, 1),
             fontsize=30, markerscale=1, borderaxespad=0.1, frameon=False,
             bbox_transform = plt.gcf().transFigure, ncol=2)
	plt.savefig("stream_runtime.pdf", bbox_inches='tight')

def run(dataset_id):
	f = open(results_dir + "stream_%d.txt" % dataset_id, "w")
	for interval in [1, 2, 4, 8, 16, 32, 64, 128, 256, 512]:
		print "Interval: %d mins" %interval * 5
		f.write("%d\n" %interval)
		for i in range(N):
			process = subprocess.Popen(
				(bashCommand % (RES, dataset_id, interval * 5 * 60)).split(),
				stdout=subprocess.PIPE)
			output, error = process.communicate()
			result_file = open(
				"contrib/src/main/java/macrobase/util/asap/results/%d_stream.txt" % dataset_id, "r")
			for line in result_file.readlines():
				f.write(line)
			result_file.close()

	f.close()

if __name__ == '__main__':
	dataset_ids = [22, 24]
	interval_in_points = []
	for dataset_id in dataset_ids:
		print dataset_id
		run(dataset_id)
		#interval_in_points = parse_results(dataset_id)

	#plot(interval_in_points)
