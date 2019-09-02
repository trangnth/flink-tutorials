package com.cloudera.streaming.examples.flink;

import com.cloudera.streaming.examples.flink.types.HeapMetrics;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.net.InetAddress;

public class HeapMonitorSource extends RichParallelSourceFunction<HeapMetrics> {

    private static final Logger LOG = LoggerFactory.getLogger(HeapMonitorSource.class);

    private final long sleepMillis;
    private volatile boolean running = true;

    private RuntimeContext ctx;
    private String hostname;

    public HeapMonitorSource(long sleepMillis) {
        this.sleepMillis = sleepMillis;
    }

    @Override
    public void run(SourceFunction.SourceContext<HeapMetrics> sourceContext) throws Exception {
        LOG.info("starting HeapMonitorSource");

        ctx = this.getRuntimeContext();

        hostname = InetAddress.getLocalHost().getHostName();

        while (running) {
            Thread.sleep(sleepMillis);

            for (MemoryPoolMXBean mpBean : ManagementFactory.getMemoryPoolMXBeans()) {
                if (mpBean.getType() == MemoryType.HEAP) {
                    MemoryUsage memoryUsage = mpBean.getUsage();
                    long used = memoryUsage.getUsed();
                    long max = memoryUsage.getMax();

                    sourceContext.collect(new HeapMetrics(mpBean.getName(), used, max, (double) used / max, ctx.getIndexOfThisSubtask(), hostname));
                }
            }
        }
    }

    @Override
    public void cancel() {
        this.running = false;
    }
}
