from matplotlib import pyplot as plt
import sys, os
import subprocess
import numpy as np
import datetime

bashCommand = 'java -Xms10G -Xmx256G -XX:+UseParallelGC -XX:ParallelGCThreads=32 -cp lib/target/classes:lib/target/test-classes:/core/target/classes:contrib/target/classes:assembly/target/*:/afs/cs.stanford.edu/u/krong/.m2/repository/junit/junit/4.12/junit-4.12.jar:$CLASSPATH edu.stanford.futuredata.macrobase.integration.StreamingSummarizationBenchmark %d %d %d %d %f %d %d %d %d'

'''
@param n              number of rows
@param k              k-way simultaneous bad attributes required for bug
@param C              cardinality of each attribute column
@param d              dimension of number of attribute columns
@param p              probability of a random row being an outlier
@param changeStartIdx index at which the "bug" starts showing up
@param changeEndIdx   index at which the "bug" ends showing up
'''

n = 100000
k = 3
C = 4
d = 10
p = 0.005
eventIdx = 50000
eventEndIdx = 100000
windowSize = 10000
slideSize = 1000

def run():
	global d, n, eventIdx, eventEndIdx
	d = 10
	print d
	for i in range(10):
		for j in range(10):
			subprocess.call((bashCommand % (n, k, C, d, p, eventIdx, eventEndIdx, windowSize, slideSize)).split())
		d = d + 1

if __name__ == '__main__':
	run()