/*
 * Server.java
 * Oct 7, 2012
 *
 * Simple Web Server (SWS) for CSSE 477
 * 
 * Copyright (C) 2012 Chandan Raj Rupakheti
 * 
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either 
 * version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/lgpl.html>.
 * 
 */

package server;

import gui.WebServer;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * This represents a welcoming server for the incoming TCP request from a HTTP
 * client such as a web browser.
 * 
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class Server implements Runnable {
	private String rootDirectory;
	private int port;
	private boolean stop;
	private ServerSocket welcomeSocket;

	private long connections;
	private long serviceTime;
	private HashMap<InetAddress, Integer> hashmap;
	private ArrayList<Thread> threadlist;
	private ArrayList<InetAddress> inetlist;
	private int ActiveConnections;
	private LinkedList<Thread> ThreadQueue;
	private LinkedList<InetAddress> inetQueue;
	private WebServer window;

	/**
	 * @param rootDirectory
	 * @param port
	 */
	public Server(String rootDirectory, int port, WebServer window) {
		this.rootDirectory = rootDirectory;
		this.port = port;
		this.stop = false;
		this.connections = 0;
		this.serviceTime = 0;
		this.window = window;
		this.hashmap = new HashMap<InetAddress, Integer>();
		this.threadlist = new ArrayList<Thread>();
		this.inetlist = new ArrayList<InetAddress>();
		this.ActiveConnections = 0;
		this.ThreadQueue = new LinkedList<Thread>();
		this.inetQueue = new LinkedList<InetAddress>();
	}

	/**
	 * Gets the root directory for this web server.
	 * 
	 * @return the rootDirectory
	 */
	public String getRootDirectory() {
		return rootDirectory;
	}

	/**
	 * Gets the port number for this web server.
	 * 
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Returns connections serviced per second. Synchronized to be used in
	 * threaded environment.
	 * 
	 * @return
	 */
	public synchronized double getServiceRate() {
		if (this.serviceTime == 0)
			return Long.MIN_VALUE;
		double rate = this.connections / (double) this.serviceTime;
		rate = rate * 1000;
		return rate;
	}

	/**
	 * Increments number of connection by the supplied value. Synchronized to be
	 * used in threaded environment.
	 * 
	 * @param value
	 */
	public synchronized void incrementConnections(long value) {
		this.connections += value;
	}

	/**
	 * Increments the service time by the supplied value. Synchronized to be
	 * used in threaded environment.
	 * 
	 * @param value
	 */
	public synchronized void incrementServiceTime(long value) {
		this.serviceTime += value;
	}

	/**
	 * The entry method for the main server thread that accepts incoming TCP
	 * connection request and creates a {@link ConnectionHandler} for the
	 * request.
	 */
	public void run() {
		try {
			this.welcomeSocket = new ServerSocket(port);
			// Now keep welcoming new connections until stop flag is set to true
			while (true) {
				// Listen for incoming socket connection
				// This method block until somebody makes a request
				Socket connectionSocket = this.welcomeSocket.accept();
				Integer x = this.hashmap.get(connectionSocket.getInetAddress());
				int q=0;
				int tempconnection=this.ActiveConnections;
				if (this.ActiveConnections > 0) {
					for (int i = 0; i < this.ActiveConnections; i++) {
						
						

						if (!this.threadlist.get(q).isAlive()) {
							this.threadlist.remove(q);
							this.hashmap.put(this.inetlist.get(q),
									this.hashmap.get(this.inetlist.get(q)) - 1);
							this.inetlist.remove(q);
							q--;
							tempconnection--;
						}
						q++;
					}
				}
				this.ActiveConnections=tempconnection;
				if (x!=null&& x >= 10) {
					System.out.println("print overflow");
					continue;
				}
				if (x==null) {
					x=0;
				}
				this.hashmap.put(connectionSocket.getInetAddress(), x++);
				// Come out of the loop if the stop flag is set
				if (this.stop)
					break;
				
				// Create a handler for this incoming connection and start the
				// handler in a new thread
				ConnectionHandler handler = new ConnectionHandler(this,
						connectionSocket);
				Thread newthread = new Thread(handler);
				this.ThreadQueue.add(newthread);
				this.inetQueue.add(connectionSocket.getInetAddress());
				while (this.ActiveConnections < 20
						&& !this.ThreadQueue.isEmpty()) {
					Thread runt = this.ThreadQueue.pop();
					runt.run();
					this.threadlist.add(runt);
					this.inetlist.add(this.inetQueue.pop());
					this.ActiveConnections++;
				}
			}
			this.welcomeSocket.close();
		} catch (Exception e) {
			window.showSocketException(e);
		}
	}

	/**
	 * Stops the server from listening further.
	 */
	public synchronized void stop() {
		if (this.stop)
			return;

		// Set the stop flag to be true
		this.stop = true;
		try {
			// This will force welcomeSocket to come out of the blocked accept()
			// method
			// in the main loop of the start() method
			Socket socket = new Socket(InetAddress.getLocalHost(), port);

			// We do not have any other job for this socket so just close it
			socket.close();
		} catch (Exception e) {
		}
	}

	/**
	 * Checks if the server is stopeed or not.
	 * 
	 * @return
	 */
	public boolean isStoped() {
		if (this.welcomeSocket != null)
			return this.welcomeSocket.isClosed();
		return true;
	}
}
