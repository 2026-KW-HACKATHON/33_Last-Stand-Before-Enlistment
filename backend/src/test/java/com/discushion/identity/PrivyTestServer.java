package com.discushion.identity;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/** Local HTTP provider protocol fixture; never used in production or against a real Privy app. */
final class PrivyTestServer implements AutoCloseable {
    record Request(String path, String method, String appId, String authorization) {}
    record Reply(int status, String type, String body, String location, long headerDelay, long bodyDelay) {}
    final List<Request> requests = new CopyOnWriteArrayList<>();
    private final AtomicReference<Reply> reply = new AtomicReference<>(new Reply(200,"application/json","{}",null,0,0));
    private final java.util.concurrent.ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        var thread=new Thread(runnable,"synthetic-privy-provider");thread.setDaemon(true);return thread;
    });
    private final HttpServer server;

    PrivyTestServer() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.setExecutor(executor);
        server.createContext("/",exchange -> {
            requests.add(new Request(exchange.getRequestURI().getRawPath(), exchange.getRequestMethod(),
                exchange.getRequestHeaders().getFirst("privy-app-id"),exchange.getRequestHeaders().getFirst("Authorization")));
            var value=reply.get();
            try {
                if(value.headerDelay()>0) Thread.sleep(value.headerDelay());
                exchange.getResponseHeaders().set("Content-Type",value.type());
                if(value.location()!=null) exchange.getResponseHeaders().set("Location",value.location());
                byte[] bytes=value.body().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(value.status(),bytes.length);
                if(value.bodyDelay()>0) Thread.sleep(value.bodyDelay());
                exchange.getResponseBody().write(bytes);
            } catch(InterruptedException failure) {Thread.currentThread().interrupt();}
            catch(java.io.IOException ignored) { /* A deadline/body-limit cancellation closes the synthetic connection. */ }
            finally {exchange.close();}
        });
        server.start();
    }

    URI base() {return URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/");}
    void json(String body) {reply.set(new Reply(200,"application/json",body,null,0,0));}
    void respond(int status,String type,String body) {reply.set(new Reply(status,type,body,null,0,0));}
    void redirect(URI target) {reply.set(new Reply(302,"application/json","{}",target.toString(),0,0));}
    void delay(long headers,long body) {var old=reply.get();reply.set(new Reply(old.status(),old.type(),old.body(),old.location(),headers,body));}
    @Override public void close() {server.stop(0);executor.shutdownNow();}
}
