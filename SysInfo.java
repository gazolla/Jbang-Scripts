
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
import java.lang.management.*;
import java.time.*;

void main() throws Exception {
    while (true) {
        var os = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        var mem = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        String output = String.format("\r🚀 CPU: %.1f%% | RAM: %d/%dMB | %s", os.getCpuLoad() * 100,
                mem.getUsed() / 1024 / 1024,
                mem.getMax() / 1024 / 1024, LocalTime.now());
        System.out.print(output);
        System.out.flush();
        Thread.sleep(1000);
    }
}