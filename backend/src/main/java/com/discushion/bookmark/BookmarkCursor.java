package com.discushion.bookmark;

import java.io.*;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/** Opaque keyset position, bound to the selected type/topic filters. */
final class BookmarkCursor {
    private BookmarkCursor() {}
    record Filter(String type,String topic) {}
    record Position(Instant createdAt,long postId) {}
    static String encode(Filter filter,Position position) {
        try {
            var bytes=new ByteArrayOutputStream();
            try(var out=new DataOutputStream(bytes)) {
                out.writeByte(1);writeNullable(out,filter.type());writeNullable(out,filter.topic());
                out.writeLong(position.createdAt().getEpochSecond());out.writeInt(position.createdAt().getNano());out.writeLong(position.postId());
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch(IOException failure) { throw new IllegalStateException("Bookmark cursor encoding failed",failure); }
    }
    static Position decode(String value,Filter filter) {
        if(value.isEmpty()||value.length()>512||!value.matches("[A-Za-z0-9_-]+"))throw BookmarkFailure.invalid("cursor");
        try {
            byte[] bytes=Base64.getUrlDecoder().decode(value);
            if(!Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(value))throw BookmarkFailure.invalid("cursor");
            try(var in=new DataInputStream(new ByteArrayInputStream(bytes))) {
                if(in.readUnsignedByte()!=1||!Objects.equals(readNullable(in),filter.type())||!Objects.equals(readNullable(in),filter.topic()))throw BookmarkFailure.invalid("cursor");
                long seconds=in.readLong();int nanos=in.readInt();long postId=in.readLong();
                if(nanos<0||nanos>999_999_999||postId<1||postId>BookmarkInput.MAX_ID||in.available()!=0)throw BookmarkFailure.invalid("cursor");
                return new Position(Instant.ofEpochSecond(seconds,nanos),postId);
            }
        } catch(IOException|RuntimeException failure) { throw BookmarkFailure.invalid("cursor"); }
    }
    private static void writeNullable(DataOutputStream out,String value)throws IOException {out.writeBoolean(value!=null);if(value!=null)out.writeUTF(value);}
    private static String readNullable(DataInputStream in)throws IOException {return in.readBoolean()?in.readUTF():null;}
}
