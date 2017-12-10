import os
from subprocess import Popen, PIPE
import json

json_dir = '/lfs/1/krong/hp_configs/hyperpilot-confs/'
command = './bin/cli.sh ../hp_configs/hyperpilot-confs/%s'

for f in os.listdir(json_dir):
	p = Popen((command % f).split(), stdout=PIPE)
	output, err = p.communicate()
	# If MB generates explanations
	if 'risk ratio' in output:
		outfile = open('results/%s.out' % (f[:-5] ), 'w')
		outfile.write(output)
		outfile.close()