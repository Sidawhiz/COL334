import java.io.*;
import java.net.*;
import java.util.*; 
import java.util.regex.Matcher; 
import java.util.regex.Pattern;


public class server{
    public static Map< String, CliSerSocket> ss_map;
    public static Map< String, CliSerSocket> rs_map;

    public server(int port) throws IOException{

        ServerSocket serve = new ServerSocket(port);
        ss_map = new HashMap<>();
        rs_map = new HashMap<>();

        while(true){

            Socket temp = null;

            try{
                temp = serve.accept();
                DataOutputStream outToClient = new DataOutputStream(temp.getOutputStream());     
                BufferedReader inFromClient  = new BufferedReader(new InputStreamReader(temp.getInputStream()));
                CliSerSocket new_socket_for_incoming_client = new CliSerSocket(temp, inFromClient, outToClient);
                ClientHandler xyz  = new ClientHandler(new_socket_for_incoming_client);               
                Thread t = new Thread(xyz);              
                t.start();
            }
            catch (Exception e){ 
                temp.close();                
                e.printStackTrace(); 
            }
        }
    }

    public static void main(String[] args) throws IOException
    {
        server server1 = new server(Integer.parseInt(args[0]));        
    }
}

class CliSerSocket{
    public Socket s;
    public BufferedReader in;
    public DataOutputStream out;
    public CliSerSocket(Socket s1, BufferedReader in1, DataOutputStream out1){
        this.s = s1;
        this.in = in1;
        this.out = out1;
    }
}

class ClientHandler implements Runnable{

    private BufferedReader instrm;      
    private DataOutputStream outstrm;       
    private Socket s;           
    private String usr;
    private CliSerSocket temp;

    public ClientHandler(CliSerSocket temp2)
    { 
        this.s = temp2.s;         
        this.instrm = temp2.in;          
        this.outstrm = temp2.out; 
        this.temp = temp2;
    }

    public void run()
    {
        String msg,usr,hdr;
        String response;

        while(true)
        {
            try
            {
                msg = this.instrm.readLine();        
                usr = msg.substring(16);        
                hdr=msg.substring(0,15);

                if(hdr.equals("REGISTER TOSEND"))
                {
                    if(usr.matches("[A-Za-z0-9]+"))
                    {          
                        this.outstrm.writeBytes("REGISTERED TOSEND "+usr+"\n");            
                        server.ss_map.put(usr,temp);            
                        this.usr=usr;        
                        forward();
                        break;                
                    }
                    else
                    {
                        this.outstrm.writeBytes("ERROR 100 Malformed username\n");            
                    }
                }
                else if(hdr.equals("REGISTER TORECV"))
                {
                    if(usr.matches("[A-Za-z0-9]+"))
                    {           
                        this.outstrm.writeBytes("REGISTERED TORECV "+usr+"\n");            
                        server.rs_map.put(usr,temp);            
                        this.usr=usr; 
                        break;   
                    }
                    else
                    {
                        this.outstrm.writeBytes("ERROR 100 Malformed username\n");
                    }
                }
                else
                {
                    this.outstrm.writeBytes("ERROR 101 No user registered\n");            
                    this.s.close();
                    break;
                }
            }
            catch (IOException e) 
            {
                e.printStackTrace();
            } 
        }
    }

    public void forward(){
        BufferedReader instream = this.instrm;                      
        DataOutputStream outstream = this.outstrm;  
        DataOutputStream rcvrout;                                    
        BufferedReader rcvrin; 
        String line_name,line_len,line_empty,line_message,resp,rcvr,line_len2; 
        Pattern p1 = Pattern.compile("^SEND [a-zA-Z0-9]+$");        
        Pattern p2 = Pattern.compile("^Content-length: [0-9]+$");

        while(true){
            try{
                line_name = instream.readLine();
                if(line_name!=null){
                    line_len  = instream.readLine();
                    line_empty = instream.readLine();
                    line_message = instream.readLine();
                    String[] rcvrx = line_name.split(" ",2);
                    rcvr = rcvrx[1];
                    int leng = line_message.length();
                    line_len2 = line_len;
                    String[] z = line_len2.split(" ",2);
                    String lol = z[1];
                    int tr = Integer.parseInt(lol);
                    if(tr!=leng){
                        outstream.writeBytes("ERROR 103 Header incomplete\n");
                        continue;
                    }


                System.out.println("["+this.usr+"] to ["+rcvr+"]: "+line_message);

                if(rcvr.equals("all") && p1.matcher(line_name).matches() && p2.matcher(line_len).matches()){
                    for(Map.Entry<String, CliSerSocket> entry : server.rs_map.entrySet()){
                        String client_name = entry.getKey();
                        CliSerSocket p = entry.getValue();
                        if(client_name.equals(this.usr)){
                            continue;
                        }
                        rcvrin= p.in;                         
                        rcvrout = p.out;
                        rcvrout.writeBytes("FORWARD "+this.usr+"\n"+line_len+"\n\n"+line_message+"\n");
                        String getmess =null;


                        while(true){
                            resp = rcvrin.readLine();
                            if(resp!=null){
                                break;
                            }
                        }
                        System.out.println(resp);
                        Integer tttt = (Integer)server.rs_map.size();
                        String ttt = tttt.toString();
                        //System.out.println(client_name);
                        if(resp.equals("RECEIVED "+this.usr))
                        {
                            outstream.writeBytes("SENT "+client_name+"\n" + ttt + "\n");
                        }
                        else 
                        {
                            outstream.writeBytes("ERROR 102 Unable to send\n");
                            break;
                        }

                    }
                }
                else if(!server.rs_map.containsKey(rcvr))
                {
                    System.out.println("Client ["+rcvr+"] not registered");
                    outstream.writeBytes("ERROR 101 No user registered\n");
                }
                else if(p1.matcher(line_name).matches() && p2.matcher(line_len).matches())
                {   
                    CliSerSocket p = server.rs_map.get(rcvr);
                    rcvrin= p.in;                         
                    rcvrout = p.out;
                    rcvrout.writeBytes("FORWARD "+this.usr+"\n"+line_len+"\n\n"+line_message+"\n");
                    String getmess =null;


                    while(true){
                        resp = rcvrin.readLine();
                        if(resp!=null){
                            break;
                        }
                    }

                    if(resp.equals("RECEIVED "+this.usr))
                    {
                        outstream.writeBytes("SENT "+rcvr+"\n");
                    }
                    else if(resp.equals("ERROR 103 Header incomplete\n")){
                        outstream.writeBytes("ERROR 103 Header incomplete\n");
                    }
                    else
                    {
                        outstream.writeBytes("ERROR 102 Unable to send\n");
                    }

                }
                else
                {
                    outstream.writeBytes("ERROR 103 Header incomplete\n");
                }
            }

            }
            catch (Exception e) 
            {
                e.printStackTrace();
            }
        }
    }

}











