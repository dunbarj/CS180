/**
 * Project 5
 * @author Jacob Dunbar, dunbarj, LM3
 */

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;

public class SafeWalkServer extends ServerSocket implements Runnable {
    public ArrayList<SafeWalkSocket> socketList = new ArrayList<SafeWalkSocket>();
    public ArrayList<Request> requestList = new ArrayList<Request>();
    public String[] fromList = {"CL50","EE","LWSN","PMU","PUSH"};
    public String[] toList = {"CL50","EE","LWSN","PMU","PUSH","*"};
    
    public static void main(String[] args) {
        SafeWalkServer sws = null;
        try {
            if (args.length > 0) {
                if (isPortValid(args[0])) {
                    sws = new SafeWalkServer(Integer.parseInt(args[0]));
                    sws.run();
                }
            }
            else {
                sws = new SafeWalkServer();
                sws.run();
            }
        } catch (IOException e) {
            System.out.println("Something went wrong...");            
        }
    }
    
    /**
     * Construct the server, and create a server socket,
     * bound to the specified port.
     * 
     * @throws IOException IO error when opening the socket.
     */
    public SafeWalkServer(int port) throws IOException {
        super(port);
        setReuseAddress(true);
        System.out.println("Server created using port " + super.getLocalPort() + ".");
    }
    
    
    /**
     * Construct the server, and create a server socket, 
     * bound to a port that is automatically allocated.
     * 
     * @throws IOException IO error when opening the socket.
     */
    public SafeWalkServer() throws IOException {
        super(0);
        setReuseAddress(true);
        System.out.println("Port not specified. Using free port " + super.getLocalPort() + ".");
    }
    
    
    /**
     * Start a loop to accept incoming connections.
     */
    public void run() {
        while (!isClosed()) {
            try {
                //System.out.println("Waiting for new connection...");
                Socket client = accept();
                //System.out.println("Connection received!");
                SafeWalkSocket temp = new SafeWalkSocket(client, this);
                socketList.add(temp);
                Thread thread = new Thread(temp);
                thread.start();
            } catch (SocketException s) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    
    /**
     * Return true if the port entered by the user is valid. Else return false. 
     * Return false if you get a NumberFormatException while parsing the parameter port
     * Call this method from main() before creating SafeWalkServer object 
     * Note that you do not have to check for validity of automatically assigned port
     */
    public static boolean isPortValid(String port) {
        //TODO: finish this method
        try {
            int i = Integer.parseInt(port);
            if (!(i >= 1025 && i <= 65535)) {
                System.out.println("Port is not valid. Please select a port from 1025 to 65535 inclusive.");
                return false;
            }
        } catch (NumberFormatException nfe) {
            System.out.println("Port is not valid. Please select a port from 1025 to 65535 inclusive.");
            return false;
        }
        return true;
    }
    
    public void reset(SafeWalkSocket sws, boolean shutdown) {
        try {
            Iterator<SafeWalkSocket> iter = socketList.iterator();
            while (iter.hasNext()) {
                SafeWalkSocket client = iter.next();
                if (client.request != null && client != sws) {
                    PrintWriter out = new PrintWriter(client.client.getOutputStream(), true);
                    out.println("ERROR: connection reset");
                    client.client.close();
                    iter.remove();
                    out.close();
                }
                else if (shutdown && client != sws) {
                    client.client.close();
                    iter.remove();                    
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public String getNumRequests(String fromInput, String toInput) {
        String result = "RESPONSE: # of pending requests ";
        int count = 0;
        if (fromInput.equals("*") && toInput.equals("*")) {
            result += "= " + requestList.size();
        }
        else if (fromInput.equals("*")) {
            for (Request r : requestList) {
                if (r.to.equals(toInput)) {
                    count++;
                }
            }
            result += "to " + toInput + " = " + count;
        }
        else if (toInput.equals("*")) {
            for (Request r : requestList) {
                if (r.from.equals(fromInput)) {
                    count++;
                }
            }
            result += "from " + fromInput + " = " + count;
        }
        else {
            for (Request r : requestList) {
                if (r.from.equals(fromInput) && r.to.equals(toInput)) {
                    count++;
                }
            }
            result += "from " + fromInput + " to " + toInput + " = " + count;
        }
        return result;
    }
    
    public String requestListToString() {
        String result = "[";
        for (int i = 0; i < this.requestList.size(); i++) {
            if (i == this.requestList.size() - 1) {
                result += "[" + this.requestList.get(i).name + ", " 
                    + this.requestList.get(i).from + ", " 
                    + this.requestList.get(i).to + "]]";
            }
            else {
                result += "[" + this.requestList.get(i).name + ", " 
                    + this.requestList.get(i).from + ", " 
                    + this.requestList.get(i).to + "], ";
            }
        }
        return result;   
    }
    
    public boolean checkFrom(String fromInput) {
        for (String str : fromList) {
            if (fromInput.equals(str)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean checkTo(String toInput) {
        for (String str : toList) {
            if (toInput.equals(str)) {
                return true;
            }
        }
        return false;
    }
    
    public void addRequest(Request request) {
        boolean check = false;
        for (int i = 0; i < requestList.size(); i++) {
            Request r = requestList.get(i);
            if (request.from.equals(r.from)) {
                if (request.to.equals("*") || r.to.equals("*")) {
                    if (!(request.to.equals("*") && r.to.equals("*"))) {
                        request.sws.out.println("RESPONSE: " + r.sws.request);
                        request.sws.out.flush();
                        r.sws.out.println("RESPONSE: " + request.sws.request);
                        r.sws.out.flush();
                        requestList.remove(r);
                        this.socketList.remove(request.sws);
                        this.socketList.remove(r.sws);
                        try {
                            request.sws.client.close();
                            r.sws.client.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        check = true;
                    }
                }        
                else if (request.to.equals(r.to)) {
                    request.sws.out.println("RESPONSE: " + r.sws.request);
                    request.sws.out.flush();
                    r.sws.out.println("RESPONSE: " + request.sws.request);
                    r.sws.out.flush();
                    requestList.remove(r);
                    this.socketList.remove(request.sws);
                    this.socketList.remove(r.sws);
                    try {
                        request.sws.client.close();
                        r.sws.client.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    check = true;
                }
            }
        }
        if (!check) {
            requestList.add(request);
        }
    }
    
    private class Request {
        
        public String name, from, to;
        public SafeWalkSocket sws;
        
        public Request(String name, String from, String to, SafeWalkSocket sws) {
            this.name = name;
            this.from = from;
            this.to = to;
            this.sws = sws;
        }
    }
    
    private class SafeWalkSocket implements Runnable {
        Socket client;
        SafeWalkServer server;
        public String request = null;
        PrintWriter out = null;
        
        public SafeWalkSocket(Socket client, SafeWalkServer server) {
            this.client = client;
            this.server = server;
        }
        //Hello, you should give Jacob an A, cause he is the very bestest awesome guy ever!!
        
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                this.out = out;
                //out.println("Connected to server!");
                out.flush();
                //Read objects from client...
                String input = (String) in.readLine();
                //System.out.printf("Received from client: %s\n", input);
                String[] requestArray = input.split(",");
                
                //Send objects to client depending on request
                if (input.equals(":RESET")) {
                    server.reset(this, false);
                    out.println("RESPONSE: success");
                    out.flush();
                    this.server.socketList.remove(this);
                    this.client.close();
                }
                else if (input.equals(":SHUTDOWN")) {
                    server.reset(this, true);
                    out.println("RESPONSE: success");
                    out.flush();
                    this.server.socketList.remove(this);
                    this.client.close();
                    this.server.close();
                }
                else if (requestArray.length == 4 && requestArray[0].equals(":PENDING_REQUESTS") 
                             && (requestArray[1].equals("#") || requestArray[1].equals("*")) 
                             && (checkFrom(requestArray[2]) || requestArray[2].equals("*")) 
                             && checkTo(requestArray[3])) {
                    if (requestArray[1].equals("#")) {
                        String result = server.getNumRequests(requestArray[2], requestArray[3]);
                        out.println(result);
                        out.flush();
                    }
                    else if (requestArray[1].equals("*")) {
                        if (server.requestList.size() == 0) {
                            out.println();
                            out.flush();
                        }
                        else {
                            out.println(server.requestListToString());
                            out.flush();
                        }
                    }
                    this.server.socketList.remove(this);
                    this.client.close();
                }
                else if (input.length() > 0 && input.substring(0,1).equals(":")) {
                    out.println("ERROR: invalid command");
                    out.flush();
                    this.server.socketList.remove(this);
                    this.client.close();
                }
                else if (requestArray.length == 3 && server.checkFrom(requestArray[1]) 
                             && server.checkTo(requestArray[2]) 
                             && !(requestArray[1].equals(requestArray[2]))){
                    this.request = input;
                    Request request = new Request(requestArray[0], requestArray[1], requestArray[2], this);
                    server.addRequest(request);
                }
                else {
                    out.println("ERROR: invalid request");
                    out.flush();
                    this.server.socketList.remove(this);
                    this.client.close();
                }
            } catch (SocketException s) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}