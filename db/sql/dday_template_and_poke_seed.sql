-- D-day message templates
create table if not exists dday_message_template (
  id bigint auto_increment primary key,
  dday int not null,
  title_template varchar(200) not null,
  body_template varchar(500) not null
);

insert into dday_message_template (dday, title_template, body_template) values
(1, '[D-1] 내일이 바로 마감일입니다! {title} 최종 확인이 필요한 시점이에요. 끝까지 힘내세요!', '{team}'),
(3, '[D-3] 어느덧 마감이 3일 앞으로 다가왔어요! {title} 진행 상황을 팀원들과 공유해 보세요.', '{team}'),
(7, '[D-7] {title} 마감까지 일주일 남았습니다! 여유 있게 준비를 시작해볼까요?', '{team}');

-- Poke message seeds (5)
insert into poke_messages (content) values
('자료를 기다리고 있는 팀원이 있어요👀'),
('혹시 바쁜 일정에 마감일을 잊으신 건 아니죠? ⏰'),
('팀원이 전한 메시지가 답변을 기다리고 있어요 💌'),
('지금 바로 회의 가능한 시간을 꼭 찍어주세요? 👉'),
('놓치면 안 될 중요한 팀 공지가 도착해요 📣');
