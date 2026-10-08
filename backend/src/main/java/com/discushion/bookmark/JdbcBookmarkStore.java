package com.discushion.bookmark;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

final class JdbcBookmarkStore {
    private final JdbcTemplate jdbc;
    JdbcBookmarkStore(DataSource source){jdbc=new JdbcTemplate(source);}
    void add(long postId,long userId,Instant now) {
        jdbc.update("insert into discushion.bookmarks(post_id,user_id,created_at) values(?,?,?) on conflict(post_id,user_id) do nothing",postId,userId,Timestamp.from(now));
    }
    void remove(long postId,long userId){jdbc.update("delete from discushion.bookmarks where post_id=? and user_id=?",postId,userId);}
    List<Row> page(long userId,BookmarkCursor.Position after,int limit) {
        String sql="select post_id,created_at from discushion.bookmarks where user_id=?"+(after==null?"":" and (created_at,post_id)<(?,?)")+" order by created_at desc,post_id desc limit ?";
        var args=new ArrayList<Object>();args.add(userId);
        if(after!=null){args.add(Timestamp.from(after.createdAt()));args.add(after.postId());}args.add(limit);
        return jdbc.query(sql,(rs,row)->new Row(rs.getLong("post_id"),rs.getTimestamp("created_at").toInstant()),args.toArray());
    }
    record Row(long postId,Instant bookmarkedAt) {}
}
