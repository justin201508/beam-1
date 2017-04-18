<!--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
-->

Spark Beam Runner (Spark-Runner)
================================

## 简介

Spark-Runner允许用户在Apache Spark上执行使用Apache Beam API编写的数据管道。Spark Runner允许在Spark引擎之上执行批处理和流式计算数据管道。

## 概述

### 特性集

- ParDo
- GroupByKey
- Combine
- Windowing
- Flatten
- View
- Side inputs/outputs
- Encoding

### 容错

Spark runner提供的容错机制与[Apache Spark](http://spark.apache.org/)一致
### 监控

Spark runner支持通过Beam Aggregators（实现了Spark的 [Accumulators](http://spark.apache.org/docs/1.6.3/programming-guide.html#accumulators)）支持用户自定义的计数器。这个Aggregators/聚合器（由管道作者定义）和Spark内部的度量机制通过Spark的[Metrics System/度量系统](http://spark.apache.org/docs/1.6.3/monitoring.html#metrics)给出度量报告。

## Beam 模型支持

### 批处理

Spark runner通过Spark的[弹性数据集](http://spark.apache.org/docs/1.6.3/programming-guide.html#resilient-distributed-datasets-rdds)提供对Beam批处理编程模型的完全支持。

### 流式计算

对Beam流式管道编程模型的完全支持正在开发中。你可以通过订阅[邮件列表](http://beam.apache.org/get-started/support/)来跟踪进展。

### 问题追踪

参考[Beam JIRA](https://issues.apache.org/jira/browse/BEAM) (runner-spark)

## 开始

要获取Spark Runner的最新版本，请首先克隆下面的Beam仓库：
    git clone https://github.com/apache/beam

然后切换到新创建的git目录，运行下面的Maven命令构建Apache Beam:

    cd beam
    mvn clean install -DskipTests

这样Apache Beam和Spark Runner就安装到了你的本地maven仓库中。

如果想在本地模式的Spark实例中，使用默认选项运行一个Beam管道，代码如下：

    Pipeline p = <logic for pipeline creation >
    PipelineResult result = p.run();
    result.waitUntilFinish();

如果想要创建一个管道运行器，运行在一个自定义Spark master url的Spark集群中，可以这么做：

    SparkPipelineOptions options = PipelineOptionsFactory.as(SparkPipelineOptions.class);
    options.setSparkMaster("spark://host:port");
    Pipeline p = <logic for pipeline creation >
    PipelineResult result = p.run();
    result.waitUntilFinish();

## 单词计数的例子

首先下载一个文本文档作为输入：

    curl http://www.gutenberg.org/cache/epub/1128/pg1128.txt > /tmp/kinglear.txt

切换到Spark runner目录：

    cd runners/spark

然后在Spark本地模式实例下运行[单词计数例子][wc]：

    mvn exec:exec -DmainClass=org.apache.beam.runners.spark.examples.WordCount \
          -Dinput=/tmp/kinglear.txt -Doutput=/tmp/out -Drunner=SparkRunner \
          -DsparkMaster=local

查看运行结果:

    head /tmp/out-00000-of-00001

注意：使用`mvn exec:exec`运行例子，仅支持工作在Spark本地模式下。可以查看下面的章节，了解如何在集群模式下运行例子。

[wc]: https://github.com/apache/beam/blob/master/runners/spark/src/main/java/org/apache/beam/runners/spark/examples/WordCount.java
## 运行在Spark集群模式下

Spark Beam管道可以使用`spark-submit`命令运行在集群模式下。


TBD： 本地hdfs的支持正在开发中 (当前阻塞于 [BEAM-59](https://issues.apache.org/jira/browse/BEAM-59)).