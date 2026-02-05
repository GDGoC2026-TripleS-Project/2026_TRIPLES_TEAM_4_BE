-- Step 1: nullable로 컬럼 추가
alter table teams add column start_at date null;
alter table teams add column end_at date null;

-- Step 2: 기존 데이터 기본값 채우기 (30일 기준)
update teams
set start_at = date(created_at),
    end_at = date(created_at) + interval 30 day
where start_at is null or end_at is null;

-- Step 3: not null 제약 적용
alter table teams modify column start_at date not null;
alter table teams modify column end_at date not null;
