-- my_schedule
alter table my_schedule add column category varchar(20) null;
alter table my_schedule add column category_memo varchar(200) null;
alter table my_schedule add column alarm_minutes int null;

-- team_schedule
alter table team_schedule add column category varchar(20) null;
alter table team_schedule add column category_memo varchar(200) null;
alter table team_schedule add column alarm_minutes int null;

-- 기존 데이터 기본값
update my_schedule set category = 'OTHER', category_memo = '미분류' where category is null;
update team_schedule set category = 'OTHER', category_memo = '미분류' where category is null;
