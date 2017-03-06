import matplotlib.pyplot as plt

f = open("14.csv", "r")
std = []
kurt = []
acf = []
for line in f.readlines():
	if "std" in line:
		continue
	nums = line.split(",")
	std.append(float(nums[1]))
	kurt.append(float(nums[2]))
	acf.append(float(nums[6]))
f.close()

plt.semilogy(std)
plt.xlabel("Window Size")
plt.ylabel("Roughness")
plt.show()