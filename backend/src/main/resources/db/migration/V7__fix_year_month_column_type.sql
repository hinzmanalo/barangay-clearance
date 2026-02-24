-- Fix: change year_month from CHAR(7) (bpchar) to VARCHAR(7) to match Hibernate entity mapping.
-- Hibernate validates String @Column(length=7) as varchar; bpchar causes schema-validation failure.
ALTER TABLE clearance_number_sequence
    ALTER COLUMN year_month TYPE VARCHAR(7);
