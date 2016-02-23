package com.google.code.gsonrmi.server;

import com.google.code.gsonrmi.Parameter;
import com.google.code.gsonrmi.RpcError;
import com.google.code.gsonrmi.RpcRequest;
import com.google.code.gsonrmi.annotations.RMI;
import com.google.code.gsonrmi.serializer.ExceptionSerializer;
import com.google.code.gsonrmi.serializer.ParameterSerializer;
import com.google.code.gsonrmi.transport.Route;
import com.google.code.gsonrmi.transport.rmi.Call;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SampleClient {

    Map<String, String> responses = Collections.synchronizedMap(new HashMap<String, String>());
    String name = "test";
    Gson gson = new Gson();

    // make non-blocking RMI call to the server
    //   server is FQDN or IP address of server
    //   port is IP port that server is listening on
    //   func is the name of an RMI function on the server
    //   params is a gson string containing a parameter object
    //   returns a retrieval key for the response. You can retrieve the response with getResponse(String key)
    //
    //
    public String rmi(String server, int port, String func, String params) {
        long id = System.currentTimeMillis();  // unqiue id for request/response

        String key = UUID.randomUUID().toString();
        // send request
        try {
            String serverUri = "tcp://" + server + ":" + port;
            String serviceUri = "rmi:" + name;
            Route serverRoute = new Route(new URI(serverUri), new URI(serviceUri));
            Call rpc = new Call(serverRoute, func, params);
            synchronized (responses){
                responses.put(key, null);
            }
            rpc.callback(new URI(serviceUri), "recieveResponse", key).send(tport);
        } catch (Exception e) {
            String rs = "EXCEPTION: RMI:" + name + " id:" + id + " method:" + func + " to:" + server + ":" + port;
            System.out.println(rs);
        }
        return key;
    }

    @RMI
    public void recieveResponse(String resp, String key,  RpcError error){
        if (error != null) {
            String rs = "RMI:" + name + " ERROR: " + error.toString();
            System.out.println(rs);
        }
        synchronized (responses){
            responses.put(key, resp);
        }
    }

    public <T> T getResponse(Class<T> classOfT, String key){
        String response;
        synchronized (responses){
            response = responses.get(key);
        }
        if (response == null) return null;
//        try {
//            DMRestExceptionBuilder dmRestExceptionBuilder = gson.fromJson(reply, DMRestExceptionBuilder.class);
//            dmRestExceptionBuilder.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
//            if (dmRestExceptionBuilder.errorCount() > 0) {
//                throw dmRestExceptionBuilder.build(); // Will fall through the catch and throw an error
//            }
//        } catch (JsonSyntaxException ignored) {
//            //Ignore the predictable parse exception
//        }
        T res;
        try {
            res = gson.fromJson(response, classOfT);
        } catch (JsonSyntaxException e){
            System.out.println("failure");
            throw new RuntimeException();
        }
        return res;
    }

	public static void main(String[] args) throws IOException {
		Gson gson = new GsonBuilder()
			.registerTypeAdapter(Exception.class, new ExceptionSerializer())
			.registerTypeAdapter(Parameter.class, new ParameterSerializer())
			.create();
			
		Socket s = new Socket("localhost", 30100);
		Reader in = new InputStreamReader(s.getInputStream(), "utf-8");
		Writer out = new OutputStreamWriter(s.getOutputStream(), "utf-8");
		
		//send first req
		RpcRequest req = new RpcRequest();
		req.method = "someMethod1";
		req.params = new Parameter[] {new Parameter("Jack")};
		req.id = new Parameter(1);
		out.write(gson.toJson(req));
		
		//send second req
		req.params = new Parameter[] {new Parameter("Obama")};
		req.id = new Parameter(2);
		out.write(gson.toJson(req));
		out.flush();
		
		//print out all responses from the server
		int c;
		while ((c = in.read()) != -1) System.out.print((char)c);
		System.out.println();
		s.close();
	}
}
