import json
import os
import pandas as pd

source_dir = '/lfs/1/macrobase-data/hyperpilot/'

json_file = "hp_query.json"
with open(json_file) as json_data_file:
    params = json.load(json_data_file)
    files = []
    for f in os.listdir(source_dir):
        if not 'snapaverage' in f:
            continue

        print "Inspecting " + f
        df = pd.read_csv("%s%s" % (source_dir, f))
        # If target metric contains only one unique value, ignore this file
        if len(set(df['value'])) == 1:
            continue
        if not 'value' in df.columns.values:
            continue

        # Make sure selected attribute column contains more than one unique value
        attrs = []
        for attr in df.columns.values:
            if attr == 'time' or attr == 'name' or attr == 'value':
                continue
            if len(set(df[attr])) > 1:
                attrs.append(attr)
        if len(attrs) == 0:
            continue

        # Specify input csv file and attributes for the config file
        params["attributes"] = attrs
        params["inputURI"] = 'csv://%s%s_selected.csv' % (source_dir, f[:-4])
        params["minRatioMetric"] = 10.0
        params["ratioMetric"] = "riskratio"
        attrs.append('value')
        # Save selected columns to another CSV file
        df_selected = df[attrs]
        df_selected.to_csv("%s%s_selected.csv" % (source_dir, f[:-4]))

        # Save config 
        with open('hyperpilot-confs/%s' % (f[:-4] + '.json'), 'w') as outfile:
             json.dump(params, outfile)


