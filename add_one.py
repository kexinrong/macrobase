from matplotlib import pyplot as plt
import sys
import subprocess
import numpy as np

bashCommand = 'java -Xms10G -Xmx256G -XX:+UseParallelGC -XX:ParallelGCThreads=32 -cp "core/target/classes:frontend/target/classes:frontend/src/main/resources/:contrib/target/classes:assembly/target/*:$CLASSPATH" macrobase.util.asap.AddOne %d %d'
N = 3
results_dir = "add_one/"


def run():
	f = open(results_dir + "addone_%d.txt" % dataset_id, "w")
	for res in [2000]:
		f.write("%d\n" %res)
		for i in range(N):
			process = subprocess.Popen((bashCommand % (res, dataset_id)).split(), stdout=subprocess.PIPE)
			output, error = process.communicate()
			result_file = open(
				"contrib/src/main/java/macrobase/util/asap/results/%d_addone.txt" % dataset_id, "r")
			for line in result_file.readlines():
				f.write(line)
			result_file.close()

	f.close()

if __name__ == '__main__':
	dataset_ids = [22]
	for dataset_id in dataset_ids:
		run()
