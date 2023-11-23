Capstone - webteam
캡스톤 - 웹구현팀

팀원: 이정훈, 이민경, 정지원

주제: 게시판을 통한 문제 업로드

##개발기능

1. 게시판
2. CHAT GPT API 연동

(참고) 게시글 작성시 DB의 테이블이 utf8이 아닐경우 한글 게시글 작성이 안됨. 
alter table board convert to charset utf8; 을 DB에 입력.
ALTER TABLE board.board MODIFY COLUMN content varchar(3001) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL

기본 완성 및 개선 여부
