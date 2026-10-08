import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const dbUrl = process.env.DISCUSHION_TEST_DB_URL;
const psql = process.env.DISCUSHION_TEST_PSQL;
if (!dbUrl || !psql) throw new Error('DISCUSHION_TEST_DB_URL/PSQL 설정이 필요합니다.');
const url = new URL(dbUrl);
if (!['postgres:', 'postgresql:'].includes(url.protocol) || url.hostname !== '127.0.0.1' || url.port !== '55432' ||
    !['/discushion_schema_draft', '/discushion_migration_test', '/discushion_roles_test'].includes(url.pathname)) {
  throw new Error('localhost 전용 시험 DB만 허용됩니다.');
}
const sql = `select json_build_object(
  'columns',(select json_agg(row_to_json(x)) from (select table_name,column_name,data_type,is_nullable,
     character_maximum_length from information_schema.columns where table_schema='discushion') x),
  'keys',(select json_agg(json_build_object('table',r.relname,'kind',c.contype,
     'parent',case when c.contype='f' then c.confrelid::regclass::text else null end,
     'parentColumns',(select json_agg(a.attname order by k.ord) from unnest(c.confkey) with ordinality k(num,ord)
       join pg_attribute a on a.attrelid=c.confrelid and a.attnum=k.num),
     'columns',(select json_agg(a.attname order by k.ord) from unnest(c.conkey) with ordinality k(num,ord)
       join pg_attribute a on a.attrelid=c.conrelid and a.attnum=k.num)))
     from pg_constraint c join pg_class r on r.oid=c.conrelid
     where c.connamespace='discushion'::regnamespace and c.contype in ('p','u','f')),
  'compositeFks',(select json_agg(json_build_object('child',r.relname,'parent',p.relname,
     'childColumns',(select json_agg(a.attname order by k.ord) from unnest(c.conkey) with ordinality k(num,ord)
       join pg_attribute a on a.attrelid=c.conrelid and a.attnum=k.num),
     'parentColumns',(select json_agg(a.attname order by k.ord) from unnest(c.confkey) with ordinality k(num,ord)
       join pg_attribute a on a.attrelid=c.confrelid and a.attnum=k.num)))
     from pg_constraint c join pg_class r on r.oid=c.conrelid join pg_class p on p.oid=c.confrelid
     where c.connamespace='discushion'::regnamespace and c.contype='f' and cardinality(c.conkey)>1)
)`;
// 자격정보를 명령 인자/exec 오류에 포함하지 않는다. 실패 시 원본 Error 객체도 출력하지 않는다.
const childEnv = { ...process.env };
if (url.password) childEnv.PGPASSWORD = decodeURIComponent(url.password);
url.password = '';
let catalog;
try {
  catalog = JSON.parse(execFileSync(psql, ['-X', '-v', 'ON_ERROR_STOP=1', '-d', url.toString(), '-Atc', sql],
    { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'], env: childEnv }).trim());
} catch {
  throw new Error('로컬 카탈로그 조회 실패. 연결 권한/DB 상태를 확인하세요. 자격정보는 출력하지 않습니다.');
}
const erd = fs.readFileSync(path.join(root, 'docs/specs/Discushion_MVP_ERD_상세명세.md'), 'utf8');
const diagram = erd.match(/```mermaid\r?\n([\s\S]*?)```/)[1];
const errors = [];
// 문서 상단의 v10.2 확정 범위 호환 표만 overlay한다. 역사적 Mermaid를 현재 MVP 정본으로 취급하지 않는다.
const nullableOverrides = new Set(['users.password_hash', 'institution_credentials.request_id']);
// 승인된 v10.2 후속 컬럼. 기존 그림의 legacy 컬럼을 삭제하지 않고 신규 컬럼도 개수/타입/NULL 검사한다.
const addedColumns = {
  users: [['privy_user_id','text',true], ['registration_completed_at','timestamp with time zone',true]],
  media_files: [['lifecycle_status','text',false], ['uploaded_at','timestamp with time zone',true],
    ['linked_at','timestamp with time zone',true], ['delete_requested_at','timestamp with time zone',true],
    ['deleted_at','timestamp with time zone',true], ['deletion_attempts','integer',false],
    ['next_delete_attempt_at','timestamp with time zone',true], ['last_delete_error_code','text',true],
    ['upload_authorization_expires_at','timestamp with time zone',true], ['deletion_claim_token','uuid',true],
    ['deletion_claimed_at','timestamp with time zone',true], ['deletion_claim_expires_at','timestamp with time zone',true],
    ['upload_transport','text',false], ['upload_attempt_id','uuid',true], ['upload_attempt_status','text',true],
    ['upload_attempt_started_at','timestamp with time zone',true], ['upload_attempt_finished_at','timestamp with time zone',true]]
};
const entities = new Map();
const typeMap = { bigint: 'bigint', integer: 'integer', text: 'text', boolean: 'boolean', timestamp: 'timestamp with time zone' };
let fields = 0;
for (const entity of diagram.matchAll(/^    (\w+) \{\r?\n([\s\S]*?)^    \}/gm)) {
  if (entity[1] === 'post_share_links') continue;
  const columns = [...entity[2].matchAll(/^[ \t]+(\w+)[ \t]+(\w+)(?:[ \t]+([A-Z,]+))?[ \t]+"([^"]+)"/gm)];
  entities.set(entity[1], columns);
  const actual = catalog.columns.filter(c => c.table_name === entity[1]);
  const extra = addedColumns[entity[1]] ?? [];
  if (actual.length !== columns.length + extra.length) errors.push(`컬럼 수: ${entity[1]}`);
  for (const [name, type, nullable] of extra) {
    fields++;
    const column = actual.find(c => c.column_name === name);
    if (!column || column.data_type !== type || (column.is_nullable === 'YES') !== nullable)
      errors.push(`v10.2 컬럼: ${entity[1]}.${name}`);
  }
  for (const [, type, name, flags = '', label] of columns) {
    fields++;
    const column = actual.find(c => c.column_name === name);
    if (!column) { errors.push(`누락 컬럼: ${entity[1]}.${name}`); continue; }
    const expectedType = type === 'varchar' ? (name === 'nickname' || name === 'bio' ? 'character varying' : 'text') : typeMap[type];
    if (column.data_type !== expectedType) errors.push(`타입: ${entity[1]}.${name}`);
    const nullable = nullableOverrides.has(`${entity[1]}.${name}`) ||
      (!flags.includes('PK') && /\bNULL\b/.test(label));
    if ((column.is_nullable === 'YES') !== nullable) errors.push(`NULL: ${entity[1]}.${name}`);
    if (name === 'nickname' && column.character_maximum_length !== 10) errors.push('닉네임 길이');
    if (name === 'bio' && column.character_maximum_length !== 50) errors.push('소개 길이');
    if (flags.includes('FK') && !catalog.keys.some(k => k.table === entity[1] && k.kind === 'f' && k.columns.includes(name)))
      errors.push(`FK 누락: ${entity[1]}.${name}`);
    if (flags.includes('UK') && !catalog.keys.some(k => k.table === entity[1] && k.kind === 'u' && k.columns.length === 1 && k.columns[0] === name))
      errors.push(`UNIQUE 누락: ${entity[1]}.${name}`);
  }
  const expectedPk = columns.filter(c => (c[3] ?? '').includes('PK')).map(c => c[2]);
  const actualPk = catalog.keys.find(k => k.table === entity[1] && k.kind === 'p')?.columns;
  if (JSON.stringify(expectedPk) !== JSON.stringify(actualPk)) errors.push(`PK: ${entity[1]}`);
}
if (new Set(catalog.columns.map(c => c.table_name)).size !== entities.size) errors.push('추가/누락 테이블');
// ERD §4의 단일 연결도 참조 대상까지 대조한다. 단순 FK 표시 존재만 검사하지 않는다.
const relationSection = erd.split('## 4.')[1].split('## 5.')[0];
let singleReferences = 0;
for (const row of relationSection.matchAll(/^\| ([^|]+) \| ([^|]+) \|/gm)) {
  const first = row[1].trim().match(/^(\w+)\.(\w+)(?:\s|$)/);
  if (!first || first[1] === 'post_share_links') continue;
  const children = row[1].trim().split(/\s*\/\s*/);
  const parents = row[2].trim().split(/\s*\/\s*/);
  if (children.length !== parents.length) { errors.push('단일 관계 표 구조'); continue; }
  for (let i = 0; i < children.length; i++) {
    const child = children[i].includes('.') ? children[i].split('.') : [first[1], children[i]];
    const parent = parents[i].split('.');
    singleReferences++;
    if (!catalog.keys.some(k => k.kind === 'f' && k.table === child[0] &&
      k.parent?.split('.').at(-1) === parent[0] && JSON.stringify(k.columns) === JSON.stringify([child[1]]) &&
      JSON.stringify(k.parentColumns) === JSON.stringify([parent[1]]))) errors.push(`단일 FK 대상: ${child.join('.')}`);
  }
}
const plan = fs.readFileSync(path.join(root, 'docs/architecture/Discushion_MVP_DB_SCHEMA_준비명세_2026-10-07.md'), 'utf8');
const section = plan.split('## 4.')[1].split('## 5.')[0];
const composite = [...section.matchAll(/^\| `(\w+)\.\(([^)]+)\)` \| `(\w+)\.\(([^)]+)\)`/gm)];
for (const [, child, childCols, parent, parentCols] of composite) {
  if (!catalog.compositeFks.some(k => k.child === child && k.parent === parent &&
    JSON.stringify(k.childColumns) === JSON.stringify(childCols.split(',').map(s => s.trim())) &&
    JSON.stringify(k.parentColumns) === JSON.stringify(parentCols.split(',').map(s => s.trim())))) errors.push(`복합 FK: ${child}`);
}
if (catalog.compositeFks.length !== 8 || composite.length !== 8) errors.push('복합 FK 개수');
if (!catalog.keys.some(k => k.table === 'users' && k.kind === 'u' && JSON.stringify(k.columns) === '["privy_user_id"]'))
  errors.push('Privy 1:1 유일성');
if (!catalog.keys.some(k => k.table === 'post_photos' && k.kind === 'u' && JSON.stringify(k.columns) === '["file_id"]'))
  errors.push('#74 파일 단일 게시물 연결 유일성');
console.log(JSON.stringify({ scope: 'legacy-preserving schema + approved v10.2 Privy/registration/media columns; API/worker excluded',
  nullableOverrides: [...nullableOverrides], tables: entities.size, columns: fields, singleReferences,
  compositeFks: composite.length, errors }, null, 2));
if (errors.length) process.exit(1);
