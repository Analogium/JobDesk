package com.jobdesk.repository;

import com.jobdesk.domain.MailScan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MailScanRepository extends JpaRepository<MailScan, UUID> {
}
