package com.google.code.gsonrmi.transport;

import com.google.code.gsonrmi.Parameter;
import com.google.code.gsonrmi.annotations.RMI;
import com.google.code.gsonrmi.serializer.ExceptionSerializer;
import com.google.code.gsonrmi.serializer.ParameterSerializer;
import com.google.code.gsonrmi.transport.rmi.Call;
import com.google.code.gsonrmi.transport.rmi.RmiService;
import com.google.code.gsonrmi.transport.tcp.TcpProxy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

public class SampleServer {

    private final static Logger logger = Logger.getLogger(SampleServer.class);

    @RMI
	public String someMethod(String name) {
		return "Hello, " + name;
	}

	public static void main(String[] args) throws IOException, URISyntaxException {
        logger.info("Started");
		//setup the transport layer
		Gson gson = new GsonBuilder()
			.registerTypeAdapter(Exception.class, new ExceptionSerializer())
			.registerTypeAdapter(Parameter.class, new ParameterSerializer())
			.create();
		Transport t = new Transport();
		new TcpProxy(Arrays.asList(new InetSocketAddress(30100)), t, gson).start();
		new RmiService(t, gson).start();

		//register an object for remote invocation
		new Call(new Route(new URI("rmi:service")), "register", "herObject", new SampleServer()).send(t);
	}
}
