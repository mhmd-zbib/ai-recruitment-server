package com.zbib.hiresync.repository;

import com.zbib.hiresync.entity.Interview;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface InterviewRepository
    extends JpaRepository<Interview, UUID>, JpaSpecificationExecutor<Interview> {
  //
  //    List<Interview> findByApplicationId(UUID applicationId);
  //
  //    List<Interview> findByJobId(UUID jobId);
  //
  //    List<Interview> findByInterviewersIdAndScheduledStartTimeBetween(
  //            UUID interviewerId, LocalDateTime start, LocalDateTime end);
  //
  //    List<Interview> findByStatusAndScheduledStartTimeBetweenAndReminderSentFalse(
  //            InterviewStatus status, LocalDateTime start, LocalDateTime end);
  //
  //    @Query("SELECT i FROM Interview i WHERE i.status = :status AND i.scheduledStartTime > :now
  // AND i.reminderSent = false")
  //    List<Interview> findUpcomingInterviewsForReminders(InterviewStatus status, LocalDateTime
  // now);
}
