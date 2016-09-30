from matplotlib import pyplot as plt
import sys
import subprocess

bashCommand = 'java -cp "core/target/classes:frontend/target/classes:frontend/src/main/resources/:contrib/target/classes:assembly/target/*:$CLASSPATH" macrobase.util.asap.BatchExperiment %d %d'
N = 1

def plot_runtime(dataset_id):
	f = open("runtime_%d.txt" % dataset_id, "r")
	runtimes = {}
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
				if not key in runtimes:
					runtimes[key] = []
		elif "runtime" not in words[2]:
			continue
		else:
			val = int(words[2].split(":")[1][1:-4])
			if len(runtimes[key]) < len(res):
				runtimes[key].append(val / N)
			else:
				runtimes[key][-1] += val / N

	f.close()

	plt.clf()
	for key in runtimes:
		plt.semilogy(res, runtimes[key], label=key, marker='.', linewidth=1.5)
	plt.xlabel("Resolution")
	plt.ylabel("Runtime (ms)")
	plt.title("Runtime VS Resolution")
	plt.grid(True)
	plt.legend(loc=2)
	plt.savefig("runtime_%d.png" % dataset_id, format="png")

def plot_quality(dataset_id):
	f = open("runtime_%d.txt" % dataset_id, "r")
	recalls = {}
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
				if not key in recalls:
					recalls[key] = []
		elif len(words) < 4:
			continue
		else:
			val = float(words[3].split(":")[1][1:])
			if len(recalls[key]) < len(res):
				recalls[key].append(val)

	f.close()
	for i in range(len(recalls["Grid1"])):
		for key in recalls:
			if key == "Grid1":
				continue
			recalls[key][i] /= recalls["Grid1"][i]
		recalls["Grid1"][i] = 1
	plt.clf()
	for key in recalls:
		plt.plot(res, recalls[key], label=key, marker='.', linewidth=1.5)
	plt.xlabel("Resolution")
	plt.ylabel("Relative Recall to Grid Search")
	plt.title("Quality VS Resolution")
	plt.grid(True)
	plt.legend(loc=1)
	plt.savefig("recall_%d.png" % dataset_id, format="png")

def run():
	f = open("runtime_%d.txt" % dataset_id, "w")
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
	dataset_id = int(sys.argv[1])
	run()
	plot_runtime(dataset_id)
	plot_quality(dataset_id)