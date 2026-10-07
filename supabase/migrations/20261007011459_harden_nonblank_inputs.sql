-- 기존 적용 이력은 보존하고 후속 Migration으로 공백-only 입력을 차단한다.
-- Unicode White_Space와 BOM만으로 구성된 값은 거부, 실제 문자가 있는 다중행 본문은 허용.
alter table discushion.profiles
  drop constraint profiles_nickname_check,
  add constraint profiles_nickname_nonblank check (
    length(btrim(nickname, U&'\0009\000A\000B\000C\000D\0020\0085\00A0\1680\2000\2001\2002\2003\2004\2005\2006\2007\2008\2009\200A\2028\2029\202F\205F\3000\FEFF')) > 0
  );
alter table discushion.comments
  drop constraint comments_content_check,
  add constraint comments_content_nonblank check (
    length(btrim(content, U&'\0009\000A\000B\000C\000D\0020\0085\00A0\1680\2000\2001\2002\2003\2004\2005\2006\2007\2008\2009\200A\2028\2029\202F\205F\3000\FEFF')) > 0
  );
