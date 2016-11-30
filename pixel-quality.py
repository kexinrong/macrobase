from matplotlib import pyplot as plt
import sys, os
import subprocess
import numpy as np
import datetime

bashCommand = 'java -Xms10G -Xmx256G -XX:+UseParallelGC -XX:ParallelGCThreads=32 -cp "core/target/classes:frontend/target/classes:frontend/src/main/resources/:contrib/target/classes:assembly/target/*:$CLASSPATH" macrobase.util.asap.ASAPGridRaw %d'
N = 1
results_dir = "evals/pixel_quality/%s/" %(str(datetime.datetime.now()))


def run():
	f = open(results_dir + "pixel_quality_%d.txt" % dataset_id, "w")
	for i in range(N):
		process = subprocess.Popen((bashCommand % (dataset_id)).split(), stdout=subprocess.PIPE)
		output, error = process.communicate()
		result_file = open(
			"contrib/src/main/java/macrobase/util/asap/results/%d_pixelquality.txt" % dataset_id, "r")
		for line in result_file.readlines():
			f.write(line)
		result_file.close()

	f.close()

if __name__ == '__main__':
	os.mkdir(results_dir)
	dataset_ids = [15, 19, 22, 24, 36, 42, 10]
	for dataset_id in dataset_ids:
		print dataset_id
		run()