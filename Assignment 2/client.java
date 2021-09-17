import java.io.*;
import java.net.*;
import java.util.*; 
import java.util.regex.Matcher; 
import java.util.regex.Pattern;

public class client{

    private Socket sSocket;
    public BufferedReader insSocket;
    public DataOutputStream outsSocket;

    private Socket rSocket;
    public BufferedReader inrSocket;
    public DataOutputStream outrSocket;

    private BufferedReader keyboard_input;

    public client(String addr, int port) throws IOException{
        String response_from_server;
        String username;

        try{
            this.sSocket = new Socket(addr,port);
            this.outsSocket = new DataOutputStream(sSocket.getOutputStream());        
            this.insSocket = new BufferedReader(new InputStreamReader(sSocket.getInputStream())); 
            this.rSocket = new Socket(addr,port);     
            this.outrSocket = new DataOutputStream(rSocket.getOutputStream());        
            this.inrSocket = new BufferedReader(new InputStreamReader(rSocket.getInputStream())); 
        }
        catch(IOException i)
        {   
            System.out.println(i);  
        }

        while(true){
            System.out.println("Enter Username:"); 
            this.keyboard_input = new BufferedReader(new InputStreamReader(System.in));  
            username = this.keyboard_input.readLine();
            this.outsSocket.writeBytes("REGISTER TOSEND "+username+"\n");
            response_from_server = this.insSocket.readLine();
            String a = (response_from_server.split(" ",2))[0];
            System.out.println(response_from_server);
            if(!(a.equals("ERROR")))
            {
                this.outrSocket.writeBytes("REGISTER TORECV "+username+"\n");
                response_from_server = this.inrSocket.readLine();
                String b = (response_from_server.split(" ",2))[0];
                System.out.println(response_from_server);
                if((a.equals("REGISTERED")) && (b.equals("REGISTERED"))){
                    System.out.println("Connected to Server");
                    break;
                }
            }
        }

        Sending x = new Sending(this.sSocket, this.insSocket, this.outsSocket);
        Thread xx = new Thread(x);
        xx.start();
        Zerodha y = new Zerodha(this.rSocket, this.inrSocket, this.outrSocket);
        Thread yy = new Thread(y);
        yy.start();

    }

    public static void main(String[] args) throws IOException {
        client a = new client(args[0],Integer.parseInt(args[1]));
    }


}

class Sending implements Runnable{
    private Socket scion;
    private BufferedReader instream ;                     
    private DataOutputStream outstream ;
    private int count = 1;
    private int count2 = 1;

    public Sending(Socket scion, BufferedReader insSocket, DataOutputStream outsSocket){ 
        this.scion = scion;         
        this.instream = insSocket;            
        this.outstream = outsSocket; 
    }

    public void run(){
        BufferedReader input_message  = new BufferedReader(new InputStreamReader(System.in)); //@Khushi Hello
        while(true)
        {
            try
            {
                String command = input_message.readLine();
                command = command.substring(1);
                String[] friend = command.split(" ",2);
                String friendname = friend[0];
                String messageforfriend = friend[1];

                String messagetoserver = "SEND " + friendname + "\n" + "Content-length: "+messageforfriend.length() + "\n\n" + messageforfriend + "\n";
                //SEND Khushi\nContent-length: 5\n\nHello\n
                this.outstream.writeBytes(messagetoserver);
                String responsefromserver = this.instream.readLine();
                if(responsefromserver!=null && friendname.equals("all"))
                {   
                    String z = this.instream.readLine();
                    //System.out.println(z);
                    count = Integer.parseInt(z);
                    //System.out.println(this.count);
                }
                while(true)
                {
                    if(responsefromserver!=null)
                    {
                        break;
                    }
                    responsefromserver = this.instream.readLine();
                    if(responsefromserver!=null && friendname.equals("all"))
                    {
                        String zzz = this.instream.readLine();
                        count = Integer.parseInt(zzz);
                        //System.out.println(this.count);
                    }
                    if( ((responsefromserver.split(" ",2))[0]).equals("ERROR") ){
                        count=0;
                    }
                }
                System.out.println("Server says: " + responsefromserver);
                if(count==2 && friendname.equals("all")){
                    System.out.println("Server says: " + "Successful Broadcast");
                }
                
                if(count==0){
                    System.out.println("Server says: " + "Unsuccessful Broadcast");
                }
                int cry=1;
                while(count>2)
                {   
                    responsefromserver = null;
                    while(true)
                    {
                        if(responsefromserver!=null)
                        {
                            break;
                        }
                        responsefromserver = this.instream.readLine();
                        if(responsefromserver!=null && friendname.equals("all"))
                        {
                            String zz = this.instream.readLine();
                            count2 = Integer.parseInt(zz);
                        }
                    }
                    if( ((responsefromserver.split(" ",2))[0]).equals("ERROR") ){
                        cry=0;
                    }
                    System.out.println("Server says: " + responsefromserver);
                    if(cry==0){
                        System.out.println("Server says: " + "Unsuccessful Broadcast");
                        break;
                    }
                    count--;
                    if(count==2 && friendname.equals("all")){
                        System.out.println("Server says: " + "Successful Broadcast");
                    }
                }
            }
            catch(IOException i)
            {   
                System.out.println(i);  
            }
        }
    }}

class Zerodha implements Runnable{
    private Socket scion;
    private BufferedReader instream ;                     
    private DataOutputStream outstream ;
    private BufferedReader instream2 ;                     
    private DataOutputStream outstream2 ;

    public Zerodha(Socket scion, BufferedReader insSocket, DataOutputStream outsSocket){ 
        this.scion = scion;         
        this.instream = insSocket;            
        this.outstream = outsSocket; 
    }

    public void run()
    {
        Pattern p1 = Pattern.compile("^FORWARD [a-zA-Z0-9]+$");        
        Pattern p2 = Pattern.compile("^Content-length: [0-9]+$");
        String line_name,line_len,line_empty,line_message,line_len2;
        while(true)
        {
            try
            {
                line_name = instream.readLine();
                if(line_name!=null)
                {
                    line_len = instream.readLine();   
                    line_empty = instream.readLine();         
                    line_message = instream.readLine();  
                    int leng = line_message.length();
                    line_len2 = line_len;
                    String[] z = line_len2.split(" ",2);
                    String[] rcvrx = line_name.split(" ",2);
                    String who_is_sender = rcvrx[1];
                    String lol = z[1];
                    int tr = Integer.parseInt(lol);
                    if(tr!=leng){
                        this.outstream.writeBytes("ERROR 103 Header incomplete\n");
                    }
                    else if(p1.matcher(line_name).matches() && p2.matcher(line_len).matches())
                    {
                        this.outstream.writeBytes("RECEIVED " + who_is_sender+"\n");
                        System.out.println(who_is_sender + ": " + line_message);
                    }
                    else
                    {
                        this.outstream.writeBytes("ERROR 103 Header incomplete\n");
                    }

                }
            }
            catch(IOException i)
            {      
                System.out.println(i);  
            }
        }
    }
}


