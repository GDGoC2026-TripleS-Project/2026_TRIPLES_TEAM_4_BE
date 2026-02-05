alter table notification_receipts add column is_read boolean not null default false;
alter table notification_receipts add column processed_at datetime null;
