from matplotlib import pyplot as plt
import sys
import subprocess
import numpy as np

bashCommand = 'java -Xms10G -Xmx256G -XX:+UseParallelGC -XX:ParallelGCThreads=32 -cp "core/target/classes:frontend/target/classes:frontend/src/main/resources/:contrib/target/classes:assembly/target/*:$CLASSPATH" macrobase.util.asap.StreamingExperiment %d %d %d'
N = 3
RES = 1024

def plot_tpt(dataset_id):
	f = open("stream_%d.txt" % dataset_id, "r")
	tpt = {}
	interval = []
	interval_in_points = []
	key = "ASAP"
	for line in f.readlines():
		words = line[:-1].split(",")
		if len(line) == 1:
			continue
		elif len(words) == 1:
			try:
				interval.append(int(line))
			except ValueError:
				key = line[:-1]
				if key == "Grid1":
					key = "Exhaustive search"
				if not key in tpt:
					tpt[key] = []
		elif "runtime" not in words[2]:
			continue
		else:
			time = int(words[2].split(":")[1][1:-11])
			points = int(words[0].split(":")[1][1:])
			val = points * 1000000.0 / time
			interval_points = int(words[3].split(":")[1][1:])
			if len(tpt[key]) < len(interval):
				tpt[key].append([val])
				if len(interval_in_points) < len(interval):
					interval_in_points.append(interval_points)
			else:
				tpt[key][-1].append(val)
	f.close()

	plt.clf()
	for key in tpt:
		means = []
		errors = []
		for i in range(len(interval)):
			means.append(np.mean(tpt[key][i]))
			errors.append(np.std(tpt[key][i]))
		plt.errorbar(interval_in_points, means, yerr=errors, label=key, marker='.', linewidth=1.5)
	plt.yscale('log')
	plt.xlabel("Refresh Interval (# points)")
	plt.ylabel("Throughput (#pts/sec)")
	plt.title("Throughput VS Refresh Interval")
	plt.grid(True)
	plt.legend(loc=4)
	plt.savefig("stream_runtime_%d.png" % dataset_id, format="png")

def run():
	f = open("stream_%d.txt" % dataset_id, "w")
	for interval in range(1, 31, 4):
		print "Interval: %d days" %interval
		f.write("%d\n" %interval)
		for i in range(N):
			process = subprocess.Popen(
				(bashCommand % (RES, dataset_id, interval * 24 * 3600)).split(),
				stdout=subprocess.PIPE)
			output, error = process.communicate()
			result_file = open(
				"contrib/src/main/java/macrobase/util/asap/results/%d_stream.txt" % dataset_id, "r")
			for line in result_file.readlines():
				f.write(line)
			result_file.close()

	f.close()

if __name__ == '__main__':
	dataset_id = int(sys.argv[1])
	run()
	plot_tpt(dataset_id)
