package com.google.code.gsonrmi.transport.http;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.code.gsonrmi.RpcError;
import com.google.code.gsonrmi.RpcRequest;
import com.google.code.gsonrmi.RpcResponse;
import com.google.code.gsonrmi.transport.Message;
import com.google.code.gsonrmi.transport.Route;
import com.google.code.gsonrmi.transport.Transport;
import com.google.code.gsonrmi.transport.Transport.Shutdown;
import com.google.gson.Gson;

public class HttpClient extends Thread {

	private final Transport t;
	private final BlockingQueue<Message> mq;
	private final Gson gson;
	
	public HttpClient(Transport transport, Gson serializer) {
		t = transport;
		mq = new LinkedBlockingQueue<Message>();
		gson = serializer;
	}
	
	@Override
	public void run() {
		try {
			while (true) process(mq.take());
		}
		catch (InterruptedException e) {
		}
	}

	protected void process(Message m) {
		if (m.contentOfType(Shutdown.class)) handle(m.getContentAs(Shutdown.class, gson));
		else handle(m);
	}

	private void handle(Shutdown m) {
		interrupt();
	}

	private void handle(Message m) {
		for (Route dest : m.dests) if (!dest.hops.isEmpty()) {
			RpcResponse response;
			try {
				URL requestUrl = dest.hops.getFirst().toURL();
				HttpURLConnection con = (HttpURLConnection) requestUrl.openConnection();
				con.setRequestMethod("POST");
				con.setRequestProperty("Content-Type", "application/json");
				con.setRequestProperty("Content-Class", m.contentType);
				con.setDoOutput(true);
				con.getOutputStream().write(m.content.getSerializedValue(gson).toString().getBytes("utf-8"));
				
				int responseCode = con.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK) {
					response = gson.fromJson(new InputStreamReader(con.getInputStream(), "utf-8"), RpcResponse.class);
				}
				else {
					RpcRequest request = m.content.getValue(RpcRequest.class, gson);
					response = new RpcResponse();
					response.id = request.id;
					response.error = new RpcError(HttpError.HTTP_REQUEST_FAILED, responseCode + " " + con.getResponseMessage());
				}
			}
			catch (IOException e) {
				RpcRequest request = m.content.getValue(RpcRequest.class, gson);
				response = new RpcResponse();
				response.id = request.id;
				response.error = new RpcError(HttpError.IO_EXCEPTION, e);
			}
			t.send(new Message(null, Arrays.asList(m.src), response));
		}
	}
}
