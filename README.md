# apex-core
Various Java utilities

[![Build Status](https://travis-ci.org/blasd/apex-core.svg?branch=master)](https://travis-ci.org/blasd/apex-core)
[![Coverage Status](https://coveralls.io/repos/github/blasd/apex-core/badge.svg?branch=master)](https://coveralls.io/github/blasd/apex-core?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.blasd.apex/apex-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.blasd.apex/apex-core)
[![Quality Gate](https://sonarqube.com/api/badges/gate?key=com.github.blasd.apex:apex-core)](https://sonarqube.com/dashboard/index/com.github.blasd.apex:apex-core)
[![Technical debt ratio](https://sonarqube.com/api/badges/measure?key=com.github.blasd.apex:apex-core&metric=sqale_debt_ratio)](https://sonarqube.com/dashboard/index/com.github.blasd.apex:apex-core)
[![javadoc.io](https://javadoc-emblem.rhcloud.com/doc/com.github.blasd.apex/apex-core/badge.svg)](http://www.javadoc.io/doc/com.github.blasd.apex/apex-core)
[![Issues](https://img.shields.io/github/issues/blasd/apex-core.svg)](https://github.com/revelc/apex-core/issues)
[![Forks](https://img.shields.io/github/forks/blasd/apex-core.svg)](https://github.com/blasd/apex-core/network)
[![Stars](https://img.shields.io/github/stars/blasd/apex-core.svg)](https://github.com/blasd/apex-core/stargazers)
[![MIT License](http://img.shields.io/badge/license-ASL-blue.svg)](https://github.com/blasd/apex-core/blob/master/LICENSE)

# Apex-java

Various utilities helping operating in Java on a daily basis.

## Standard helpers
GCInspector is drop-in class providing standard logs related to GC activity
```
  @Bean
	public IApexThreadDumper apexThreadDumper() {
		return new ApexThreadDump(ManagementFactory.getThreadMXBean());
	}

	@Bean
	public GCInspector gcInspector(IApexThreadDumper apexThreadDumper) {
		return new GCInspector(apexThreadDumper);
	}
```

ApexLogHelper helps publishing relevant logs regarding memory and timings
```
  Assert.assertEquals("0.09%", ApexLogHelper.getNicePercentage(123, 123456).toString());
  
  
  Assert.assertEquals("9sec 600ms", ApexLogHelper.getNiceTime(9600).toString());
  Assert.assertEquals("2min 11sec", ApexLogHelper.getNiceTime(131, TimeUnit.SECONDS).toString());
  
  
  Assert.assertEquals("789B", ApexLogHelper.getNiceMemory(789L).toString());
  Assert.assertEquals("607KB", ApexLogHelper.getNiceMemory(789L * 789).toString());
  Assert.assertEquals("468MB", ApexLogHelper.getNiceMemory(789L * 789 * 789).toString());
  Assert.assertEquals("360GB", ApexLogHelper.getNiceMemory(789L * 789 * 789 * 789).toString());
  Assert.assertEquals("278TB", ApexLogHelper.getNiceMemory(789L * 789 * 789 * 789 * 789).toString());
  Assert.assertEquals("214PB", ApexLogHelper.getNiceMemory(789L * 789 * 789 * 789 * 789 * 789).toString());
```

## Fancy helpers
ObjectInputHandlingInputStream enables transmitting a raw InputStream through an ObjectInput

ApexCartesianProductHelper helps computing covering cartesian products over sets defined by Collections and Maps.

ApexProcessHelper enables tracking the memory consumption of a process (would it be current JVM, a forked Process or any other process).
```
  ApexProcessHelper.getProcessResidentMemory(processPID)
```

# Apex-MAT
A fork from Eclipse MAT for HeapDump analysis. It improves original MAT by lowering the heap required to prepare MAT index files, while keeping the produced indexes compatible with the original MAT.

