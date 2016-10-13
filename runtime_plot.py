from matplotlib import pyplot as plt
import sys
import subprocess
import numpy as np

bashCommand = 'java -Xms10G -Xmx256G -XX:+UseParallelGC -XX:ParallelGCThreads=32 -cp "core/target/classes:frontend/target/classes:frontend/src/main/resources/:contrib/target/classes:assembly/target/*:$CLASSPATH" macrobase.util.asap.BatchExperiment %d %d'
N = 5
results_dir = "batch_results/"

def plot_tpt(dataset_id):
	f = open(results_dir + "batch_%d.txt" % dataset_id, "r")
	tpt = {}
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
		elif "runtime" not in words[2]:
			continue
		else:
			time = int(words[2].split(":")[1][1:-11])
			points = int(words[0].split(":")[1][1:])
			val = points * 1000000.0 / time
			if len(tpt[key]) < len(res):
				tpt[key].append([val])
			else:
				tpt[key][-1].append(val)

	f.close()

	plt.clf()
	for key in tpt:
		means = []
		errors = []
		for i in range(len(res)):
			means.append(np.mean(tpt[key][i]))
			errors.append(np.std(tpt[key][i]))
		plt.errorbar(res, means, yerr=errors, label=key, marker='.', linewidth=1.5)
	plt.yscale('log')
	plt.xlabel("Resolution")
	plt.ylabel("Throughput (#pts/s)")
	plt.title("Throughput VS Resolution")
	plt.grid(True)
	plt.legend(loc=3)
	plt.savefig(results_dir + "tpt_%d.png" % dataset_id, format="png")

def plot_quality(dataset_id):
	f = open(results_dir + "batch_%d.txt" % dataset_id, "r")
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
				if not key in variances:
					variances[key] = []
		elif "var" not in words[2]:
			continue
		else:
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
	plt.clf()
	for key in variances:
		#plt.semilogy(res, variances[key], label=key, marker='.', linewidth=1.5)
		plt.plot(res, variances[key], label=key, marker='.', linewidth=1.5)
	plt.xlabel("Resolution")
	plt.ylabel("Relative to Optimal")
	plt.title("Quality VS Resolution")
	plt.grid(True)
	plt.legend(loc=1)
	plt.savefig(results_dir + "quality_%d.png" % dataset_id, format="png")

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
	dataset_ids = [5, 12]
	#dataset_ids = [9, 11, 12, 13, 14, 15, 17, 18, 19, 21, 22, 24, 27, 10, 16]
	#[5, 8, 9]
	for dataset_id in dataset_ids:
		print dataset_id
		run()
		plot_tpt(dataset_id)
		plot_quality(dataset_id)