param(
  [Parameter(Mandatory=$true)][string]$DbUrl,
  [Parameter(Mandatory=$true)][string]$PsqlPath
)
$ErrorActionPreference='Stop'
$taskUri=[Uri]$DbUrl
$taskDatabase=$taskUri.AbsolutePath.Trim('/')
if($taskUri.Scheme -notin @('postgres','postgresql') -or $taskUri.Host -ne '127.0.0.1' -or
   $taskUri.Port -ne 55432 -or $taskDatabase -notin @('discushion_schema_draft','discushion_migration_test','discushion_roles_test')) {
  throw '시험은 127.0.0.1:55432의 명시된 전용 DB에서만 허용됩니다.'
}
$taskRoot=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$taskDump=Join-Path (Split-Path $PsqlPath) 'pg_dump.exe'
$taskMarkerCreated=$false
$taskMarkerId=$null
$taskMarkerCode='synthetic-idempotency-'+[Guid]::NewGuid().ToString('N')
function Invoke-LocalSql([string]$Sql) {
  $taskResult=& $PsqlPath -X -q -v ON_ERROR_STOP=1 -d $DbUrl -Atc $Sql
  if($LASTEXITCODE -ne 0) { throw '로컬 SQL 검사 실패' }
  # 이력이 2개 이상이면 네이티브 출력은 줄 배열이다. 배열 -cne 비교의 거짓 실패를 방지한다.
  return ($taskResult -join "`n")
}
function Get-SchemaSnapshot {
  $taskResult=& $taskDump --schema-only --no-owner --schema=discushion --dbname=$DbUrl
  if($LASTEXITCODE -ne 0) { throw '카탈로그 덤프 실패' }
  # PostgreSQL pg_dump의 세션별 임의 restrict 키만 제외한다. 권한/정의는 비교 대상이다.
  return (($taskResult | Where-Object {$_ -notmatch '^\\(un)?restrict '}) -join "`n")
}
Push-Location $taskRoot
try {
  & npm.cmd exec --yes --package=supabase@2.120.0 -- supabase migration up --db-url $DbUrl
  if($LASTEXITCODE -ne 0) { throw 'Migration 적용 실패' }
  & $PsqlPath -X -v ON_ERROR_STOP=1 -d $DbUrl -f 'supabase/tests/schema_integrity.sql'
  if($LASTEXITCODE -ne 0) { throw '제약/보안 시험 실패' }
  $taskBeforeSchema=Get-SchemaSnapshot
  $taskBeforeHistory=Invoke-LocalSql 'select string_agg(version,chr(10) order by version) from supabase_migrations.schema_migrations;'
  # Windows psql의 -c 인자 인코딩 영향을 피한다. 한글 fixture는 UTF-8 SQL 파일에서 검증한다.
  $taskMarkerId=Invoke-LocalSql "insert into discushion.regions(name,external_code) values('synthetic-idempotency-marker','$taskMarkerCode') returning id;"
  $taskMarkerId=($taskMarkerId | Where-Object {$_ -match '^\d+$'} | Select-Object -First 1)
  if(!$taskMarkerId) { throw '시험 표식 ID 확인 실패' }
  $taskMarkerCreated=$true
  $taskBeforeData=Invoke-LocalSql "select row_to_json(r)::text from discushion.regions r where id=$taskMarkerId;"
  & npm.cmd exec --yes --package=supabase@2.120.0 -- supabase migration up --db-url $DbUrl
  if($LASTEXITCODE -ne 0) { throw 'Migration 재실행 실패' }
  $taskAfterSchema=Get-SchemaSnapshot
  $taskAfterHistory=Invoke-LocalSql 'select string_agg(version,chr(10) order by version) from supabase_migrations.schema_migrations;'
  $taskAfterData=Invoke-LocalSql "select row_to_json(r)::text from discushion.regions r where id=$taskMarkerId;"
  if($taskBeforeSchema -cne $taskAfterSchema -or $taskBeforeHistory -cne $taskAfterHistory -or $taskBeforeData -cne $taskAfterData) {
    throw ('재실행 비교 실패: Schema={0}, 이력={1}, 데이터={2}' -f
      ($taskBeforeSchema -cne $taskAfterSchema),($taskBeforeHistory -cne $taskAfterHistory),($taskBeforeData -cne $taskAfterData))
  }
  Write-Output 'PASS: DB 무결성 시험 및 Migration 재실행의 Schema/이력/기존 데이터 불변 확인 (assertion 수는 SQL 출력 참조)'
} finally {
  # 이 실행이 만든 합성 표식만 제거. 다른 데이터·Schema·DB를 초기화하지 않는다.
  if($taskMarkerCreated) {
    Invoke-LocalSql "delete from discushion.regions where id=$taskMarkerId and external_code='$taskMarkerCode';" | Out-Null
  }
  Pop-Location
}
