package com.discushion.summary;
import com.discushion.identity.IdentityFailure;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
final class JdbcSummaryStore {
    private final JdbcTemplate jdbc;
    JdbcSummaryStore(DataSource source){jdbc=new JdbcTemplate(source);}
    record Source(long id,long revision,String title,String content,String author,Instant updated){}
    record State(long revision,String status,String summary,Instant requested,Instant generated){}
    Instant now(){return jdbc.queryForObject("select current_timestamp",Timestamp.class).toInstant();}
    void member(long id){
        var completed=jdbc.query("select registration_completed_at from discushion.users where id=?",(rs,n)->rs.getTimestamp(1)!=null,id);
        if(completed.isEmpty())throw new IdentityFailure(IdentityFailure.Reason.MEMBER_NOT_FOUND);
        if(!completed.get(0))throw new IdentityFailure(IdentityFailure.Reason.INCOMPLETE);
    }
    Source source(long id){
        var rows=jdbc.query("""
            select p.id,p.content_revision,p.type,p.status,p.title,p.content,p.updated_at,f.nickname
            from discushion.posts p join discushion.profiles f on f.user_id=p.author_user_id where p.id=? for share of p
            """,(rs,n)->{
                if(!"PUBLISHED".equals(rs.getString("status")))throw new SummaryFailure(404,"POST_NOT_FOUND");
                if(!"LOCAL_AGENDA".equals(rs.getString("type")))throw new SummaryFailure(422,"AI_SUMMARY_NOT_APPLICABLE");
                return new Source(rs.getLong("id"),rs.getLong("content_revision"),rs.getString("title"),rs.getString("content"),rs.getString("nickname"),rs.getTimestamp("updated_at").toInstant());
            },id);
        if(rows.isEmpty())throw new SummaryFailure(404,"POST_NOT_FOUND");return rows.get(0);
    }
    State state(long id){
        return jdbc.query("select source_revision,status,summary,requested_at,generated_at from discushion.ai_agenda_summaries where post_id=?",
            (rs,n)->new State(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getTimestamp(4).toInstant(),
                rs.getTimestamp(5)==null?null:rs.getTimestamp(5).toInstant()),id).stream().findFirst().orElse(null);
    }
    boolean claim(Source source,Instant now){
        return jdbc.update("""
            insert into discushion.ai_agenda_summaries(post_id,source_revision,status,summary,requested_at,generated_at,updated_at)
            values(?,?,'PENDING',null,?,null,?)
            on conflict(post_id) do update set source_revision=excluded.source_revision,status='PENDING',
              summary=null,requested_at=excluded.requested_at,generated_at=null,updated_at=excluded.updated_at
            where discushion.ai_agenda_summaries.source_revision<excluded.source_revision
            """,source.id,source.revision,Timestamp.from(now),Timestamp.from(now))==1;
    }
    void finish(Source source,Instant requested,SummaryGenerator.Result result,Instant now){
        jdbc.update("""
            update discushion.ai_agenda_summaries set status=?,summary=?,generated_at=?,updated_at=?
            where post_id=? and source_revision=? and status='PENDING' and requested_at=?
            """,result.status(),result.summary(),"SUCCEEDED".equals(result.status())?Timestamp.from(now):null,Timestamp.from(now),
            source.id,source.revision,Timestamp.from(requested));
    }
}
