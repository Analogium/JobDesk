package com.jobdesk.repository;

import com.jobdesk.domain.MailScan;
import com.jobdesk.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MailScanRepository extends JpaRepository<MailScan, UUID> {

    List<MailScan> findByUserOrderByScannedAtDesc(User user);

    /** Suppression du compte : la FK vers `user` n'a pas de ON DELETE CASCADE. */
    void deleteByUser(User user);

    /** Limitation de conservation : les journaux de scan anciens n'ont plus d'utilité. */
    @Modifying
    @Query("delete from MailScan s where s.scannedAt < :before")
    int deleteScannedBefore(LocalDateTime before);
}
