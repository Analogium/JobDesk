<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

/**
 * Auto-generated Migration: Please modify to your needs!
 */
final class Version20260416163147 extends AbstractMigration
{
    public function getDescription(): string
    {
        return '';
    }

    public function up(Schema $schema): void
    {
        // this up() migration is auto-generated, please modify it to your needs
        $this->addSql('CREATE TABLE application (id UUID NOT NULL, company_name VARCHAR(255) NOT NULL, job_title VARCHAR(255) NOT NULL, job_url VARCHAR(2048) DEFAULT NULL, job_description TEXT DEFAULT NULL, location VARCHAR(255) DEFAULT NULL, contract_type VARCHAR(255) DEFAULT NULL, salary_range VARCHAR(100) DEFAULT NULL, status VARCHAR(255) NOT NULL, applied_at TIMESTAMP(0) WITHOUT TIME ZONE DEFAULT NULL, source VARCHAR(255) NOT NULL, notes TEXT DEFAULT NULL, created_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL, updated_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL, user_id UUID NOT NULL, PRIMARY KEY (id))');
        $this->addSql('CREATE INDEX IDX_A45BDDC1A76ED395 ON application (user_id)');
        $this->addSql('CREATE TABLE contact (id UUID NOT NULL, name VARCHAR(255) NOT NULL, email VARCHAR(255) DEFAULT NULL, role VARCHAR(255) DEFAULT NULL, notes TEXT DEFAULT NULL, application_id UUID NOT NULL, PRIMARY KEY (id))');
        $this->addSql('CREATE INDEX IDX_4C62E6383E030ACD ON contact (application_id)');
        $this->addSql('CREATE TABLE mail_scan (id UUID NOT NULL, scanned_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL, mails_analyzed INT DEFAULT NULL, matches_found INT DEFAULT NULL, status VARCHAR(20) NOT NULL, error_message TEXT DEFAULT NULL, user_id UUID NOT NULL, PRIMARY KEY (id))');
        $this->addSql('CREATE INDEX IDX_69C820C7A76ED395 ON mail_scan (user_id)');
        $this->addSql('CREATE TABLE status_history (id UUID NOT NULL, previous_status VARCHAR(255) DEFAULT NULL, new_status VARCHAR(255) NOT NULL, changed_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL, trigger VARCHAR(20) NOT NULL, notes TEXT DEFAULT NULL, application_id UUID NOT NULL, PRIMARY KEY (id))');
        $this->addSql('CREATE INDEX IDX_2F6A07CE3E030ACD ON status_history (application_id)');
        $this->addSql('CREATE TABLE "user" (id UUID NOT NULL, email VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, avatar_url VARCHAR(500) DEFAULT NULL, google_token TEXT DEFAULT NULL, gmail_token TEXT DEFAULT NULL, gmail_refresh_token TEXT DEFAULT NULL, last_mail_scan_at TIMESTAMP(0) WITHOUT TIME ZONE DEFAULT NULL, created_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL, updated_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL, PRIMARY KEY (id))');
        $this->addSql('CREATE UNIQUE INDEX UNIQ_8D93D649E7927C74 ON "user" (email)');
        $this->addSql('ALTER TABLE application ADD CONSTRAINT FK_A45BDDC1A76ED395 FOREIGN KEY (user_id) REFERENCES "user" (id) NOT DEFERRABLE');
        $this->addSql('ALTER TABLE contact ADD CONSTRAINT FK_4C62E6383E030ACD FOREIGN KEY (application_id) REFERENCES application (id) NOT DEFERRABLE');
        $this->addSql('ALTER TABLE mail_scan ADD CONSTRAINT FK_69C820C7A76ED395 FOREIGN KEY (user_id) REFERENCES "user" (id) NOT DEFERRABLE');
        $this->addSql('ALTER TABLE status_history ADD CONSTRAINT FK_2F6A07CE3E030ACD FOREIGN KEY (application_id) REFERENCES application (id) NOT DEFERRABLE');
    }

    public function down(Schema $schema): void
    {
        // this down() migration is auto-generated, please modify it to your needs
        $this->addSql('ALTER TABLE application DROP CONSTRAINT FK_A45BDDC1A76ED395');
        $this->addSql('ALTER TABLE contact DROP CONSTRAINT FK_4C62E6383E030ACD');
        $this->addSql('ALTER TABLE mail_scan DROP CONSTRAINT FK_69C820C7A76ED395');
        $this->addSql('ALTER TABLE status_history DROP CONSTRAINT FK_2F6A07CE3E030ACD');
        $this->addSql('DROP TABLE application');
        $this->addSql('DROP TABLE contact');
        $this->addSql('DROP TABLE mail_scan');
        $this->addSql('DROP TABLE status_history');
        $this->addSql('DROP TABLE "user"');
    }
}
