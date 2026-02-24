-- V2: Seed singleton rows for barangay_settings and fee_config
INSERT INTO barangay_settings (id, barangay_name, municipality, province, captain_name)
VALUES (1, 'Barangay San Jose', 'Municipality of Sample', 'Province of Sample', 'Juan dela Cruz')
ON CONFLICT DO NOTHING;

INSERT INTO fee_config (id, standard_fee, rush_fee)
VALUES (1, 50.00, 100.00)
ON CONFLICT DO NOTHING;
