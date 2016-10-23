package sendmsg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
/*
 * condition: read file time << sleep time1
 * spy_path: store control file
 * raw_path: store raw file
 * default speed: 10log/1000ms
 * port: 9999
 * check control speed: every time after reading enough log
 * sleep to monitor speed: every time complete a data sending
 * server accept over time: 60000ms = 1min
 * default read file: raw.txt
 * */
public class SendMsg {
	private static String spy_path="";
	private static String raw_path="/var/lib/docker/share/raw.txt";
	private static int number = 100;
	private static int second = 1000; //default 100log/5s
	//private static String ip = "192.105.146.217";
	private static int port = 9900;
	/*
	 * stop.smc stop send message
	 * pause.smc pause and check if start every 5 second
	 * start.smc start send message
	 * number-second.smc adjust to number logs per second
	 * */
	public static void main(String args[]){
		spy_path = "/var/www/script/analysis/sendmsg";
		if(args.length >= 1){
			try{
				port = Integer.parseInt(args[0]);
				System.out.println("set port as "+port);
			}catch(Exception e){
				port = 9999;
				e.printStackTrace();
				System.out.println("parse parameter error, set default 9900");
			}
		}
		readFileByLines(raw_path);
	}
	public static void readFileByLines(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;
        ServerSocket server=null;
        Socket socket = null;
        PrintWriter out = null;
        try {
        	/*
             * start server at port 9999
             * */
        	server=new ServerSocket(port);
        	server.setSoTimeout(60000);
        	socket = null;
        	int try_time = 0;
        	/*
             * accept socket and set socket
             * quit after 1minute
             * */
        	while(socket == null && try_time < 60){
        		try{
        			socket = server.accept();
        		}catch(Exception e){
        			throw e;
        		}
        	}
        	System.out.println("socket set");
        	/*
             * set output stream on socket
             * */
        	out = new PrintWriter(socket.getOutputStream());
        	System.out.println("outstream set");
        	while(true){
        		reader = new BufferedReader(new FileReader(file));
        		int line = 0;
            	String tempString;
            	String send_data = "";
            	/*
                 * every time read [number] lines log and make new send logs
                 * every time read enough log check control file
                 * stop => throw UserStop Exception
                 * pause => wait and ask every 5s about start
                 * start => continue
                 * continue => clean data and run with newly set number and second
                 * */
            	while ((tempString = reader.readLine()) != null) {
            		send_data += tempString;
            		send_data += "\n";
                	line++;
                	if(line % number == 0){
	                	if(check_control()){
	                		throw(new Exception("UserStop"));
	                	}
	                	send(send_data, out);
	                	Thread.sleep(second);
	                	send_data = "";
	                	line = 0;
	            	}
            	}
            	reader.close();
        	}
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
        	/*
             * close file reader
             * close output stream
             * close socket
             * close server socket
             * */
        	System.out.println("close file reader");
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        	System.out.println("close socket out stream");
            if (out != null){
            	try{
            		out.close();
            	}catch (Exception e2){}
            }
        	System.out.println("close socket");
            if (socket != null){
            	try{
            		socket.close();
            	}catch (Exception e3){}
            }
        	System.out.println("close server socket");
            if (server != null){
            	try{
            		server.close();
            	}catch (Exception e4){}
            }
        }
	}
	public static boolean check_control(){
		//check stop
		File f = new File(spy_path+"/stop.smc");
		if(f.exists()){
			System.out.println("stop");
			System.out.println("delete stop file");
			f.delete();
			return true;
		}
		//check pause
		f = new File(spy_path+"/pause.smc");
		if(f.exists()){
			System.out.println("puase");
			f.delete();
			return check_start();
		}
		File dir = new File(spy_path);
		File [] listOfFiles = dir.listFiles();
		for(int i=0; i< listOfFiles.length;i++)
		{ 
			if(listOfFiles[i].isFile()){
				String fileName = listOfFiles[i].getName();
				try{
					number = Integer.parseInt(fileName.substring(0, fileName.indexOf("-")));
					second = Integer.parseInt(fileName.substring(fileName.indexOf("-")+1,fileName.indexOf(".")));
					System.out.println("set send speed to "+number+"logs/"+second);
				}catch(Exception e){
					System.out.println("illegal file name "+fileName);
					e.printStackTrace();
					System.out.println("set default speed");
					number = 10;
					second = 1000;
				}
				listOfFiles[i].delete();
			}
		}
		return false;
	}
	public static boolean check_start(){
		while(true){
			File f = new File(spy_path+"/stop.smc");
			if(f.exists()){
				System.out.println("stop");
				System.out.println("delete stop file");
				f.delete();
				return true;
			}
			f = new File(spy_path+"/start.smc");
			if(f.exists()){
				System.out.println("start");
				f.delete();
				return false;
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				System.out.println("puase faild");
				e.printStackTrace();
				return true;
			}
		}
	}
	public static boolean send(String data, PrintWriter out){
		out.print(data);
		out.flush();
		return true;
	}
}
