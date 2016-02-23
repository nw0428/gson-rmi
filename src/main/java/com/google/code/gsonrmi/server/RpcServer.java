package com.google.code.gsonrmi.server;

import com.google.gson.Gson;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;

public class RpcServer extends Thread {

    Logger logger = Logger.getLogger(RpcServer.class);

	private ServerSocket ss;
	private RpcTarget rpcTarget;
	private Gson gson;

	public RpcServer(int port, RpcTarget rpcTarget, Gson gson) throws IOException {
		ss = new ServerSocket(port);
		this.rpcTarget = rpcTarget;
		this.gson = gson;
	}
	
	public void shutdown() throws IOException {
		ss.close();
	}
	
	@Override
	public void run() {
		try {
			while (true) new RpcServlet(ss.accept(), rpcTarget, gson).start();
		}
		catch (IOException e) {
			if (ss.isClosed()) System.err.println("RPC server terminated normally");
			else {
				System.err.println("RPC server terminated by error");
				e.printStackTrace();
			}
		}
	}

}
