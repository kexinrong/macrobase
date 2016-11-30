from matplotlib import pyplot as plt
import sys, os
import subprocess
import numpy as np
import datetime

bashCommand = 'java -Xms10G -Xmx256G -XX:+UseParallelGC -XX:ParallelGCThreads=32 -cp "core/target/classes:frontend/target/classes:frontend/src/main/resources/:contrib/target/classes:assembly/target/*:$CLASSPATH" macrobase.util.asap.RemoveOne %d %d'
N = 3
results_dir = "evals/remove_one/%s/" %(str(datetime.datetime.now()))


def run():
	for res in [2000, 5000]:
		f = open(results_dir + "removeone_%d_%d.txt" % (dataset_id, res), "w")
		f.write("%d\n" %res)
		for i in range(N):
			print i
			process = subprocess.Popen((bashCommand % (res, dataset_id)).split(), stdout=subprocess.PIPE)
			output, error = process.communicate()
			result_file = open(
				"contrib/src/main/java/macrobase/util/asap/results/%d_removeone.txt" % dataset_id, "r")
			for line in result_file.readlines():
				f.write(line)
			result_file.close()

		f.close()

if __name__ == '__main__':
	os.mkdir(results_dir)
	dataset_ids = [24]
	for dataset_id in dataset_ids:
		print dataset_id
		run()
