package com.notification.service;

import com.notification.domain.NotificationJob;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

@Component
@Slf4j
public class QueueManager {

    @Getter
    private final BlockingQueue<NotificationJob> highPriorityQueue = new LinkedBlockingQueue<>();

    @Getter
    private final PriorityBlockingQueue<JobItem> standardPriorityQueue = new PriorityBlockingQueue<>();

    public void push(NotificationJob job) {
        if (job.getPriority() != null && job.getPriority() == 1) {
            highPriorityQueue.offer(job);
        } else {
            standardPriorityQueue.offer(new JobItem(job));
        }
    }

    public record JobItem(NotificationJob job) implements Comparable<JobItem> {
        @Override
        public int compareTo(JobItem o) {
            // Lower priority number = Higher Priority
            int p = Integer.compare(this.job.getPriority(), o.job.getPriority());
            if (p != 0)
                return p;
            // FIFO for same priority
            return this.job.getCreatedAt().compareTo(o.job.getCreatedAt());
        }
    }
}
